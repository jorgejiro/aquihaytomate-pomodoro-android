package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import java.time.Instant
import java.time.ZoneId

/**
 * Decides whether a stopped timer is left over from a day that is already over.
 *
 * The cycle counter lives in the persisted state and nothing used to age it, so a phone picked up a
 * week later still opened on `3/4` — a cycle nobody was in the middle of. A cycle belongs to a working
 * day: when the day turns, it starts again at zero and the next slot is a focus one.
 *
 * Pure, and with a `ZoneId` handed in rather than read from the system, like every other time rule in
 * the engine. See docs/decisions/015-el-ciclo-se-reinicia-al-cambiar-de-dia.md.
 */
object DayRollover {

    /**
     * @param nowEpochMs wall clock, since the question is about calendar days rather than about
     *   elapsed time.
     *
     * Three things it deliberately does not do:
     * - **It never touches a running timer.** A pomodoro straddling midnight belongs to whoever
     *   started it, and the countdown keeps its own deadlines.
     * - **It only rolls forward.** `isBefore` rather than an inequality, so moving the system clock
     *   back does not wipe the cycle the user is in the middle of — the same rule that keeps a clock
     *   change from completing a slot.
     * - **It says no when there is no marker.** A state written before `lastActivityEpochMs` existed
     *   falls back to the older stamps; when even those are zero nothing is known, and guessing would
     *   mean clearing a cycle that may well be from this morning.
     */
    fun startsNewDay(state: TimerState, nowEpochMs: Long, zone: ZoneId): Boolean {
        if (state.status == TimerStatus.RUNNING) return false
        if (state.isFreshStart) return false
        return isEarlierDay(state.lastTouchedEpochMs, nowEpochMs, zone)
    }

    /**
     * Whether [epochMs] fell on a local day before the one [nowEpochMs] is in. Zero and anything in the
     * future answer false, for the reasons above.
     *
     * Exposed on its own because `reconcile()` has to judge a moment the state no longer remembers: the
     * deadline of a slot that ran out days ago and is only being closed now.
     */
    fun isEarlierDay(epochMs: Long, nowEpochMs: Long, zone: ZoneId): Boolean {
        if (epochMs <= 0L) return false
        val day = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        return day.isBefore(today)
    }

    /**
     * A state that already looks like the first pomodoro of a fresh batch: nothing to roll over, and
     * skipping it keeps the app from writing to disk every single day just to say the same thing.
     */
    private val TimerState.isFreshStart: Boolean
        get() = status == TimerStatus.IDLE &&
            slotType == SlotType.FOCUS &&
            completedFocusInCycle == 0 &&
            sessionId == 0L &&
            slotIndex == 0
}
