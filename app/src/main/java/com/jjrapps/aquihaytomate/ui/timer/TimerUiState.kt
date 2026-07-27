package com.jjrapps.aquihaytomate.ui.timer

import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerStatus

/** What the primary control does right now. The label follows from it. */
enum class PrimaryControl { START, PAUSE, RESUME, START_BREAK, BACK_TO_WORK }

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
    ) : TimerUiState {

        /** The secondary control appears only when there is progress to throw away. */
        val showReset: Boolean get() = status != TimerStatus.IDLE

        /** Breaks carry the calyx, the redundancy that keeps the phase off colour alone. */
        val showCalyx: Boolean get() = slotType.isBreak
    }
}
