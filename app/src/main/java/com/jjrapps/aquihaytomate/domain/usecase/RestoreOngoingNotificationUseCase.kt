package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.TimerNotifier
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import java.time.Clock
import javax.inject.Inject

/**
 * Puts the ongoing notification back after the user swiped it away, as long as there is still a slot to
 * report on.
 *
 * From Android 13 the user can dismiss the notification of a foreground service, and the service survives
 * it: the countdown keeps running with nothing on screen to prove it, and the three controls go with it.
 * For a timer that is a bad trade — the notification *is* the timer while the app is closed — so a
 * `deleteIntent` brings it back.
 *
 * **Only while the clock is a going concern.** In `IDLE` there is nothing to restore, and in `RINGING` the
 * dismissal is the user acknowledging the alert, which is exactly what dismissing it should do. Republishing
 * either of those would be the notification that will not die.
 */
class RestoreOngoingNotificationUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val notifier: TimerNotifier,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    suspend operator fun invoke() {
        val state = timerStateRepository.current()

        when (state.status) {
            TimerStatus.RUNNING -> notifier.showRunning(state)

            TimerStatus.PAUSED -> notifier.showPaused(
                state,
                TimerMath.remainingMs(state, clock.millis(), elapsedRealtime.millis()),
            )

            TimerStatus.RINGING, TimerStatus.IDLE -> Unit
        }
    }
}
