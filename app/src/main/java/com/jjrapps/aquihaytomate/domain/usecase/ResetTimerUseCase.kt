package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import java.time.Clock
import javax.inject.Inject

/**
 * Sends the current slot back to its start, stopped. The cycle position and the batch are untouched:
 * restarting a pomodoro is not the same as abandoning the cycle.
 *
 * A focus slot that had been running for at least a minute is recorded as a partial one before the
 * reset, so the time really spent is not lost.
 */
class ResetTimerUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val settingsRepository: SettingsRepository,
    private val recordFocusSlot: RecordFocusSlotUseCase,
    private val syncTimerRuntime: SyncTimerRuntimeUseCase,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    suspend operator fun invoke(): Boolean {
        val state = timerStateRepository.current()
        if (state.status == TimerStatus.IDLE) return false

        val settings = settingsRepository.current()

        // Only a slot with a clock behind it can have produced focus time. In RINGING the slot that
        // finished has already been recorded, and the state now describes the *next* one, so reading
        // an elapsed time off it would invent a full pomodoro out of nothing.
        val recorded = if (state.status.isActive) {
            val remainingMs = TimerMath.remainingMs(
                state,
                clock.millis(),
                elapsedRealtime.millis(),
                settings,
            )
            recordFocusSlot(
                state = state,
                activeElapsedMs = TimerMath.activeElapsedMs(state.slotDurationMs, remainingMs),
                completed = false,
            )
        } else {
            false
        }

        // A recorded partial burns the slot index: reusing it would make the unique index swallow the
        // row for the retried slot, and a pomodoro the user did finish would go unrecorded.
        val nextSlotIndex = if (recorded) state.slotIndex + 1 else state.slotIndex

        val changed = timerStateRepository.update { current ->
            if (current.status == TimerStatus.IDLE) {
                null
            } else {
                TimerTransitions.resetSlot(current, settings, nextSlotIndex)
            }
        }
        if (changed) syncTimerRuntime()
        return changed
    }
}
