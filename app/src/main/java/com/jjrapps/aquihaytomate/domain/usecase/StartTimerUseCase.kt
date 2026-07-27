package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import java.time.Clock
import javax.inject.Inject

/**
 * Starts the pending slot, or resumes a paused one.
 *
 * @return true when the state changed. An already running timer is left alone rather than restarted,
 *   which matters because this is reachable from a widget tap.
 */
class StartTimerUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    suspend operator fun invoke(): Boolean {
        val settings = settingsRepository.current()
        val nowEpochMs = clock.millis()
        val nowElapsedRealtimeMs = elapsedRealtime.millis()

        return timerStateRepository.update { state ->
            when (state.status) {
                TimerStatus.IDLE, TimerStatus.RINGING ->
                    TimerTransitions.start(state, settings, nowEpochMs, nowElapsedRealtimeMs)

                TimerStatus.PAUSED ->
                    TimerTransitions.resume(state, nowEpochMs, nowElapsedRealtimeMs)

                TimerStatus.RUNNING -> null
            }
        }
    }
}
