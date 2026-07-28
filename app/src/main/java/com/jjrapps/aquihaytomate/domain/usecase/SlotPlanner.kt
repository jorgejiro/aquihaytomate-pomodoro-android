package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerState

/** The shape of the slot that comes next, before any clock is involved. */
data class PlannedSlot(
    val type: SlotType,
    val durationMs: Long,
    val completedFocusInCycle: Int,
    val slotIndex: Int,
)

/**
 * Decides what follows the slot that just ended. Pure, so the service, the backup alarm and the UI
 * cannot disagree about where the cycle is.
 */
object SlotPlanner {

    /**
     * @param completedFully true when the slot ran to zero, false when it was skipped or reset.
     *
     * Two rules that are easy to get wrong:
     * - **The cycle counter only advances on focus slots completed in full.** Skipping a pomodoro
     *   must not bring the long break closer, or skipping becomes a shortcut to it.
     * - **A skipped focus always leads to a short break, never to the long one**, for the same
     *   reason.
     *
     * The counter is reset when leaving the long break rather than when entering it, so the cycle
     * dots keep reading `4/4` for the whole long break instead of snapping back to `1/4`.
     */
    fun planNextSlot(
        currentType: SlotType,
        completedFocusInCycle: Int,
        currentSlotIndex: Int,
        completedFully: Boolean,
        settings: TimerSettings,
    ): PlannedSlot {
        val perCycle = settings.pomodorosPerCycle
        val nextIndex = currentSlotIndex + 1

        return when (currentType) {
            SlotType.FOCUS -> {
                val completed =
                    if (completedFully) completedFocusInCycle + 1 else completedFocusInCycle
                val goesLong = completedFully && completed >= perCycle
                val type = if (goesLong) SlotType.LONG_BREAK else SlotType.SHORT_BREAK
                PlannedSlot(
                    type = type,
                    durationMs = settings.durationMsFor(type),
                    completedFocusInCycle = completed.coerceAtMost(perCycle),
                    slotIndex = nextIndex,
                )
            }

            SlotType.SHORT_BREAK -> PlannedSlot(
                type = SlotType.FOCUS,
                durationMs = settings.durationMsFor(SlotType.FOCUS),
                completedFocusInCycle = completedFocusInCycle.coerceAtMost(perCycle - 1),
                slotIndex = nextIndex,
            )

            SlotType.LONG_BREAK -> PlannedSlot(
                type = SlotType.FOCUS,
                durationMs = settings.durationMsFor(SlotType.FOCUS),
                completedFocusInCycle = 0,
                slotIndex = nextIndex,
            )
        }
    }

    /**
     * What comes after the slot [state] describes, assuming it is left to run out.
     *
     * The "up next" readout of the timer screen and of the ongoing notification both go through here, so
     * the two cannot promise the user different things. It is the same call the engine will make when the
     * slot really ends, with `completedFully = true` — the optimistic case, which is the one being
     * predicted.
     *
     * **Meaningless in [com.jjrapps.aquihaytomate.domain.model.TimerStatus.RINGING]**: there the state has
     * already moved on to the next slot, so this would answer with the one after it. Callers do not show
     * the readout in that state.
     */
    fun upcomingSlot(state: TimerState, settings: TimerSettings): PlannedSlot = planNextSlot(
        currentType = state.slotType,
        completedFocusInCycle = state.completedFocusInCycle,
        currentSlotIndex = state.slotIndex,
        completedFully = true,
        settings = settings,
    )
}
