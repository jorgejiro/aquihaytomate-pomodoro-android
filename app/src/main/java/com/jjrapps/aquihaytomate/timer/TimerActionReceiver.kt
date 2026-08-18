package com.jjrapps.aquihaytomate.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jjrapps.aquihaytomate.domain.repository.AlertPlayer
import com.jjrapps.aquihaytomate.domain.repository.TimerNotifier
import com.jjrapps.aquihaytomate.domain.usecase.PauseTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ResetTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.RestoreOngoingNotificationUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ResumeTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.SkipSlotUseCase
import com.jjrapps.aquihaytomate.domain.usecase.StartTimerUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The notification actions.
 *
 * A notification action is one of the legitimate exemptions for starting a foreground service, so the
 * Resume and Start-next paths can bring the service back up from here.
 */
@AndroidEntryPoint
class TimerActionReceiver : BroadcastReceiver() {

    @Inject lateinit var startTimer: StartTimerUseCase

    @Inject lateinit var pauseTimer: PauseTimerUseCase

    @Inject lateinit var resumeTimer: ResumeTimerUseCase

    @Inject lateinit var skipSlot: SkipSlotUseCase

    @Inject lateinit var resetTimer: ResetTimerUseCase

    @Inject lateinit var restoreOngoingNotification: RestoreOngoingNotificationUseCase

    @Inject lateinit var notifier: TimerNotifier

    @Inject lateinit var alertPlayer: AlertPlayer

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_PAUSE -> pauseTimer()
                    ACTION_RESUME -> resumeTimer()
                    ACTION_SKIP -> skipSlot()
                    ACTION_RESET -> resetTimer()
                    // The ▸ of a stopped slot. Same thing the Timer screen's primary control does.
                    ACTION_START -> startTimer()
                    ACTION_START_NEXT -> {
                        // Whatever is still sounding has been acknowledged by the tap itself.
                        alertPlayer.stop()
                        startTimer()
                    }

                    // The user swiped the ongoing notification away. From Android 13 that is allowed
                    // even for a foreground service, and the timer would keep running unseen.
                    ACTION_ONGOING_DISMISSED -> restoreOngoingNotification()

                    ACTION_DISMISS -> {
                        alertPlayer.stop()
                        notifier.clearAlert()
                    }

                    else -> Timber.w("Unknown timer action: %s", action)
                }
            } catch (e: Exception) {
                Timber.e(e, "Could not handle timer action %s", action)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.jjrapps.aquihaytomate.PAUSE"
        const val ACTION_RESUME = "com.jjrapps.aquihaytomate.RESUME"
        const val ACTION_SKIP = "com.jjrapps.aquihaytomate.SKIP"
        const val ACTION_RESET = "com.jjrapps.aquihaytomate.RESET"
        const val ACTION_START = "com.jjrapps.aquihaytomate.START"
        const val ACTION_START_NEXT = "com.jjrapps.aquihaytomate.START_NEXT"
        const val ACTION_DISMISS = "com.jjrapps.aquihaytomate.DISMISS"
        const val ACTION_ONGOING_DISMISSED = "com.jjrapps.aquihaytomate.ONGOING_DISMISSED"
    }
}
