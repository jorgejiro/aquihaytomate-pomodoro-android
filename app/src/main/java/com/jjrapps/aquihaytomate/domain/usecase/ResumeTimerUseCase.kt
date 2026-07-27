package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import java.time.Clock
import javax.inject.Inject

/** Restarts a paused countdown from where it stopped. */
class ResumeTimerUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val syncTimerRuntime: SyncTimerRuntimeUseCase,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    suspend operator fun invoke(): Boolean {
        val nowEpochMs = clock.millis()
        val nowElapsedRealtimeMs = elapsedRealtime.millis()

        val changed = timerStateRepository.update { state ->
            if (state.status != TimerStatus.PAUSED) {
                null
            } else {
                TimerTransitions.resume(state, nowEpochMs, nowElapsedRealtimeMs)
            }
        }
        if (changed) syncTimerRuntime()
        return changed
    }
}
