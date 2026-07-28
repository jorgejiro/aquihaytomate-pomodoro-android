package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import javax.inject.Inject

/**
 * The single-tap action: start, pause, resume or move on to the next slot, whichever the current
 * status calls for.
 *
 * This is what the primary control in the app, the widget tap and the notification action all go
 * through, so those three cannot end up disagreeing about what a tap means.
 */
class ToggleTimerUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val startTimer: StartTimerUseCase,
    private val pauseTimer: PauseTimerUseCase,
) {

    suspend operator fun invoke(): Boolean =
        when (timerStateRepository.current().status) {
            TimerStatus.RUNNING -> pauseTimer()
            // IDLE, PAUSED and RINGING all mean "get going"; StartTimerUseCase resolves which.
            else -> startTimer()
        }
}
