package com.jjrapps.aquihaytomate.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import com.jjrapps.aquihaytomate.domain.usecase.CompleteSlotUseCase
import com.jjrapps.aquihaytomate.domain.usecase.PlannedSlot
import com.jjrapps.aquihaytomate.domain.usecase.SlotPlanner
import com.jjrapps.aquihaytomate.domain.usecase.TimerMath
import dagger.hilt.android.AndroidEntryPoint
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Layer 2 of the engine: the mechanism that actually runs the countdown, and the only one that needs no
 * permission from the user.
 *
 * It lives only while the timer is `RUNNING`. See CLAUDE.md §6 for the rules this file must not break;
 * the two that shape the code most:
 *
 * - **A foreground service does not keep the CPU awake.** The process survives, but the device suspends
 *   and `delay()` wakes up late — minutes late on some OEMs. Hence the `PARTIAL_WAKE_LOCK`, and hence it
 *   always carries a timeout: a wakelock stuck for an hour shows up in Play Vitals.
 * - **`startForeground()` on the first line of `onStartCommand`**, before touching DataStore. There are
 *   five seconds before `ForegroundServiceDidNotStartInTimeException`, and a disk read is not something
 *   to gamble them on.
 */
@AndroidEntryPoint
class PomodoroTimerService : Service() {

    @Inject lateinit var timerStateRepository: TimerStateRepository

    /** Read only to work out what follows the running slot, for the second line of the notification. */
    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var completeSlot: CompleteSlotUseCase

    @Inject lateinit var notificationFactory: TimerNotificationFactory

    @Inject lateinit var clock: Clock

    @Inject lateinit var elapsedRealtime: ElapsedRealtimeSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var countdownJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // First thing, always: a placeholder built from the last known state is fine, and the countdown
        // coroutine republishes the real one a few milliseconds later.
        startForegroundWithPlaceholder()

        countdownJob?.cancel()
        countdownJob = scope.launch { runCountdown() }

        // START_NOT_STICKY: a sticky restart would revive us with a null intent and no context. We would
        // rather be gone and let reconcile() repair the state when the app or the widget comes back.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundWithPlaceholder() {
        val placeholder = notificationFactory.ongoingRunning(TimerState.EMPTY)
        ServiceCompat.startForeground(
            this,
            TimerNotificationFactory.NOTIFICATION_ID_ONGOING,
            placeholder,
            specialUseTypeFlag,
        )
    }

    /**
     * `specialUse` only exists from Android 14; on 12 and 13 the type is neither required nor accepted,
     * and the manifest attribute is simply ignored there.
     */
    private val specialUseTypeFlag: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    /**
     * Waits for the deadline of whatever slot is running, closes it, and repeats if auto-start moved
     * straight into the next one.
     *
     * The notification is republished once per transition — about four `notify()` calls per pomodoro —
     * because the figure itself is drawn by SystemUI from the deadline. Nothing here repaints per second.
     */
    private suspend fun runCountdown() {
        while (true) {
            val state = timerStateRepository.state.distinctUntilChanged().first()
            if (state.status != TimerStatus.RUNNING) {
                stopSelf()
                return
            }

            publishOngoing(state, SlotPlanner.upcomingSlot(state, settingsRepository.current()))

            val remainingMs =
                TimerMath.remainingMs(state, clock.millis(), elapsedRealtime.millis())
            withWakeLock(remainingMs) { delay(remainingMs) }

            // completeSlot is idempotent, so racing the backup alarm here is harmless by construction.
            completeSlot()

            if (timerStateRepository.current().status != TimerStatus.RUNNING) {
                stopSelf()
                return
            }
        }
    }

    private fun publishOngoing(state: TimerState, nextSlot: PlannedSlot) {
        try {
            ServiceCompat.startForeground(
                this,
                TimerNotificationFactory.NOTIFICATION_ID_ONGOING,
                notificationFactory.ongoingRunning(state, nextSlot),
                specialUseTypeFlag,
            )
        } catch (e: IllegalStateException) {
            Timber.w(e, "Could not refresh the ongoing notification")
        }
    }

    /**
     * Holds a `PARTIAL_WAKE_LOCK` for the duration of [block], bounded to the remaining time plus a
     * margin.
     *
     * **The timeout is not optional.** It is the difference between a timer that fires on the second and
     * a battery-drain warning in Play Vitals.
     */
    private suspend fun withWakeLock(remainingMs: Long, block: suspend () -> Unit) {
        val powerManager = getSystemService(PowerManager::class.java)
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)

        try {
            wakeLock?.acquire(remainingMs + WAKELOCK_MARGIN_MS)
            block()
        } finally {
            if (wakeLock?.isHeld == true) {
                runCatching { wakeLock.release() }
            }
        }
    }

    companion object {
        private const val WAKELOCK_TAG = "AquiHayTomate:slot"
        private const val WAKELOCK_MARGIN_MS = 5_000L

        fun intent(context: Context): Intent = Intent(context, PomodoroTimerService::class.java)
    }
}
