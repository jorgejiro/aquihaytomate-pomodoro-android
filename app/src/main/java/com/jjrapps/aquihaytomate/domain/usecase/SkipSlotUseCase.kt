package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import java.time.Clock
import javax.inject.Inject

/**
 * Abandons the current slot and moves on to the next one.
 *
 * Skipping a focus slot does **not** advance the cycle counter and always leads to a short break,
 * never to the long one — `SlotPlanner` enforces both, so skipping cannot be used as a shortcut to the
 * long break. Time already spent focusing is recorded as a partial slot if it reached a minute.
 */
class SkipSlotUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val settingsRepository: SettingsRepository,
    private val recordFocusSlot: RecordFocusSlotUseCase,
    private val syncTimerRuntime: SyncTimerRuntimeUseCase,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    suspend operator fun invoke(): Boolean {
        val state = timerStateRepository.current()
        val settings = settingsRepository.current()
        val nowEpochMs = clock.millis()
        val nowElapsedRealtimeMs = elapsedRealtime.millis()

        if (state.status.isActive) {
            val remainingMs =
                TimerMath.remainingMs(state, nowEpochMs, nowElapsedRealtimeMs, settings)
            recordFocusSlot(
                state = state,
                activeElapsedMs = TimerMath.activeElapsedMs(state.slotDurationMs, remainingMs),
                completed = false,
            )
        }

        val planned = SlotPlanner.planNextSlot(
            currentType = state.slotType,
            completedFocusInCycle = state.completedFocusInCycle,
            currentSlotIndex = state.slotIndex,
            completedFully = false,
            settings = settings,
        )
        val nextStatus =
            if (settings.autoStartsInto(planned.type)) TimerStatus.RUNNING else TimerStatus.IDLE

        val changed = timerStateRepository.update { current ->
            // Bail out if something else moved the timer on while we were reading and recording.
            if (current.slotIndex != state.slotIndex || current.status != state.status) {
                null
            } else {
                TimerTransitions.advanceTo(
                    state = current,
                    planned = planned,
                    status = nextStatus,
                    settings = settings,
                    nowEpochMs = nowEpochMs,
                    nowElapsedRealtimeMs = nowElapsedRealtimeMs,
                )
            }
        }
        if (changed) syncTimerRuntime()
        return changed
    }
}
