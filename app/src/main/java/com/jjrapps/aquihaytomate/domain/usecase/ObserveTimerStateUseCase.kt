package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** The persisted timer state as a stream. Nothing ticks on it: consumers derive time via `TimerMath`. */
class ObserveTimerStateUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
) {
    operator fun invoke(): Flow<TimerState> = timerStateRepository.state
}
