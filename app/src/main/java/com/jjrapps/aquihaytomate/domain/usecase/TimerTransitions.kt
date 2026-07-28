package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus

/**
 * Every move of the state machine `IDLE → RUNNING ⇄ PAUSED → RINGING → …`, as pure functions.
 *
 * The command use cases are thin wrappers around these: they read the clocks and the repositories and
 * hand the arithmetic over here, so the interesting part is testable without Hilt, DataStore or an
 * emulator.
 *
 * **Note what `RINGING` means here:** the state already describes the *next* slot, planned and ready
 * to start, and the slot that just finished is implied by it — a break coming up means a focus slot
 * ended, and vice versa. That keeps `RINGING` a hair away from `IDLE` (start it and it runs) instead
 * of a special case every consumer has to unpack, and it stops the cycle counter from being advanced
 * twice by re-planning the same transition.
 */
object TimerTransitions {

    /**
     * Arms the pending slot described by [state] — from `IDLE` or `RINGING` — for its full duration.
     *
     * The duration and the cycle length are frozen into the snapshot here. From this moment changing
     * Settings does not reshape the run under way.
     */
    fun start(
        state: TimerState,
        settings: TimerSettings,
        nowEpochMs: Long,
        nowElapsedRealtimeMs: Long,
    ): TimerState {
        val durationMs = state.durationMsWith(settings)
        return state.copy(
            status = TimerStatus.RUNNING,
            sessionId = if (state.sessionId == 0L) nowEpochMs else state.sessionId,
            pomodorosPerCycle = state.pomodorosPerCycleWith(settings),
            slotDurationMs = durationMs,
            slotStartedAtEpochMs = nowEpochMs,
            remainingAtPauseMs = 0L,
        ).armed(durationMs, nowEpochMs, nowElapsedRealtimeMs)
    }

    /** Freezes the clock. The remaining time is stored so it survives the process dying. */
    fun pause(state: TimerState, remainingMs: Long): TimerState = state.copy(
        status = TimerStatus.PAUSED,
        remainingAtPauseMs = remainingMs.coerceIn(0L, state.slotDurationMs),
    )

    /**
     * Restarts the clock from where it was paused.
     *
     * Pushing the deadline forward by exactly the remaining time is what makes
     * `duration - remaining` equal the real active time, which is how the engine avoids keeping a
     * separate accumulator. See docs/decisions/006-el-tiempo-activo-se-deriva-del-restante.md.
     */
    fun resume(state: TimerState, nowEpochMs: Long, nowElapsedRealtimeMs: Long): TimerState =
        state.copy(status = TimerStatus.RUNNING)
            .armed(state.remainingAtPauseMs, nowEpochMs, nowElapsedRealtimeMs)

    /**
     * Moves on to [planned].
     *
     * @param status `RUNNING` when auto-start is on, `RINGING` when the user has to acknowledge the
     *   end of the slot, `IDLE` when the slot expired so long ago that ringing would be noise.
     */
    fun advanceTo(
        state: TimerState,
        planned: PlannedSlot,
        status: TimerStatus,
        settings: TimerSettings,
        nowEpochMs: Long,
        nowElapsedRealtimeMs: Long,
    ): TimerState {
        val next = state.copy(
            status = status,
            slotType = planned.type,
            slotIndex = planned.slotIndex,
            completedFocusInCycle = planned.completedFocusInCycle,
            pomodorosPerCycle = settings.pomodorosPerCycle,
            slotDurationMs = planned.durationMs,
            slotStartedAtEpochMs = nowEpochMs,
            remainingAtPauseMs = 0L,
            endAtEpochMs = 0L,
            endAtElapsedRealtimeMs = 0L,
            bootEpochMs = 0L,
        )
        return if (status == TimerStatus.RUNNING) {
            next.armed(planned.durationMs, nowEpochMs, nowElapsedRealtimeMs)
        } else {
            next
        }
    }

    /**
     * Sends the current slot back to its start, stopped.
     *
     * @param slotIndex the index the restarted slot takes. When a partial focus slot was recorded the
     *   caller passes the next index: `UNIQUE(session_id, slot_index)` would otherwise swallow the
     *   row for the retried slot, and a pomodoro the user did complete would go unrecorded.
     */
    fun resetSlot(state: TimerState, settings: TimerSettings, slotIndex: Int): TimerState =
        state.copy(
            status = TimerStatus.IDLE,
            slotIndex = slotIndex,
            slotDurationMs = settings.durationMsFor(state.slotType),
            pomodorosPerCycle = settings.pomodorosPerCycle,
            slotStartedAtEpochMs = 0L,
            endAtEpochMs = 0L,
            endAtElapsedRealtimeMs = 0L,
            bootEpochMs = 0L,
            remainingAtPauseMs = 0L,
        )

    /** Writes the two deadlines and the boot marker for a slot with [remainingMs] left to run. */
    private fun TimerState.armed(
        remainingMs: Long,
        nowEpochMs: Long,
        nowElapsedRealtimeMs: Long,
    ): TimerState {
        val remaining = remainingMs.coerceAtLeast(0L)
        return copy(
            endAtEpochMs = nowEpochMs + remaining,
            endAtElapsedRealtimeMs = nowElapsedRealtimeMs + remaining,
            bootEpochMs = TimerMath.bootEpochMs(nowEpochMs, nowElapsedRealtimeMs),
        )
    }
}
