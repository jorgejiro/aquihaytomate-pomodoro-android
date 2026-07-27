package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import java.time.Clock
import javax.inject.Inject

/**
 * Freezes the countdown, storing what was left.
 *
 * The remaining time is read from the clocks *before* entering the transaction, so the value written
 * is the one the user saw when they tapped rather than one sampled after the disk write.
 */
class PauseTimerUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val syncTimerRuntime: SyncTimerRuntimeUseCase,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    suspend operator fun invoke(): Boolean {
        val nowEpochMs = clock.millis()
        val nowElapsedRealtimeMs = elapsedRealtime.millis()

        val changed = timerStateRepository.update { state ->
            if (state.status != TimerStatus.RUNNING) {
                null
            } else {
                TimerTransitions.pause(
                    state,
                    TimerMath.remainingMs(state, nowEpochMs, nowElapsedRealtimeMs),
                )
            }
        }
        if (changed) syncTimerRuntime()
        return changed
    }
}
