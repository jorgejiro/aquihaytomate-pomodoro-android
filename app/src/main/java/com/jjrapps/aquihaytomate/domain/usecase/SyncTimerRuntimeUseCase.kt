package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerAlarmScheduler
import com.jjrapps.aquihaytomate.domain.repository.TimerNotifier
import com.jjrapps.aquihaytomate.domain.repository.TimerServiceController
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import java.time.Clock
import javax.inject.Inject

/**
 * Brings the three moving parts of the engine in line with the persisted state: the foreground
 * service, the backup alarm and the notification.
 *
 * **Called explicitly by every command use case that changed something**, rather than by a collector
 * watching the state. That is deliberate: `startForegroundService` is only allowed from a background
 * process within the exemption window opened by the event that triggered it — a tap in the app, a
 * notification action, a widget tap, an exact alarm. A debounced collector would routinely miss that
 * window and every start would degrade to "state plus alarm only". Calling it inline keeps the start
 * inside the window.
 *
 * Being one place rather than six also means the service, the alarm and the notification cannot end up
 * disagreeing about what the state is.
 */
class SyncTimerRuntimeUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: TimerAlarmScheduler,
    private val serviceController: TimerServiceController,
    private val notifier: TimerNotifier,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    suspend operator fun invoke() {
        val state = timerStateRepository.current()

        when (state.status) {
            TimerStatus.RUNNING -> {
                // The alarm goes first: if the service start is refused, the safety net is already up.
                alarmScheduler.arm(state.endAtElapsedRealtimeMs)
                notifier.clearAlert()
                serviceController.start()
            }

            TimerStatus.PAUSED -> {
                // Nothing to wake up for with the clock stopped, and burning a wakelock and a
                // foreground notification on a stationary timer makes no sense.
                alarmScheduler.cancel()
                serviceController.stop()
                notifier.clearAlert()
                notifier.showPaused(
                    state,
                    TimerMath.remainingMs(state, clock.millis(), elapsedRealtime.millis()),
                )
            }

            TimerStatus.RINGING -> {
                alarmScheduler.cancel()
                serviceController.stop()
                notifier.clearOngoing()
                notifier.showSlotFinished(state)
            }

            TimerStatus.IDLE -> {
                alarmScheduler.cancel()
                serviceController.stop()
                // A stopped timer with a batch behind it still has something to offer: the pending slot
                // at its full length, with a ▸. Resetting from the shade used to clear the shade — the
                // timer was still there and the only way back to it was opening the app. With no batch
                // under way there is nothing to report, and a notification would be clutter.
                if (state.hasBatchUnderWay) {
                    notifier.showIdle(state, state.durationMsWith(settingsRepository.current()))
                } else {
                    notifier.clearOngoing()
                }
            }
        }
    }
}
