package com.jjrapps.aquihaytomate.ui.timer

import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus

/** What the primary control does right now. The label follows from it. */
enum class PrimaryControl { START, PAUSE, RESUME, START_BREAK, BACK_TO_WORK }

/**
 * The slot that follows the current one, for the "up next" readout.
 *
 * @param minutes the length of that slot, already in whole minutes: it is a duration to read, not a
 *   countdown, and `5 min` is easier to take in at a glance than `05:00`.
 */
data class NextSlot(val type: SlotType, val minutes: Int)

sealed interface TimerUiState {

    /** First frame, before the persisted state has been read off disk. */
    data object Loading : TimerUiState

    data class Success(
        val status: TimerStatus,
        val slotType: SlotType,
        val timeText: String,
        val remainingMs: Long,
        /** 1 when the slot begins, 0 when it runs out. */
        val fillFraction: Float,
        val primaryControl: PrimaryControl,
        val completedInCycle: Int,
        val cyclePosition: Int,
        val pomodorosPerCycle: Int,
        val keepScreenOn: Boolean,
        /** Null while ringing: there the primary control already names what comes next. */
        val nextSlot: NextSlot? = null,
    ) : TimerUiState {

        /** Reset appears only when there is progress to throw away. */
        val showReset: Boolean get() = status != TimerStatus.IDLE

        /**
         * Skip appears except on the untouched first screen.
         *
         * Skipping a break that has not started yet is a real thing to want — "I do not need this one" —
         * so it is offered while idle too. Skipping the very first focus slot before starting it is not:
         * it would jump to a break earned by nothing.
         */
        val showSkip: Boolean get() = TimerState.offersSkip(status, slotType)
    }
}
