package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus

/**
 * Everything time-related the engine needs, as pure functions over a [TimerState] plus the two
 * clocks read by the caller. No Android, no singletons, no ticking.
 *
 * The whole point is that the UI, the widget, the notification, the service and the backup alarm all
 * compute the remaining time the same way from the same persisted snapshot, so they cannot drift.
 */
object TimerMath {

    /** Slack allowed on the uptime comparison below, to absorb rounding and sampling skew. */
    const val REBOOT_TOLERANCE_MS = 2_000L

    /** Epoch minus monotonic uptime: constant within a boot, recorded for diagnostics. */
    fun bootEpochMs(nowEpochMs: Long, nowElapsedRealtimeMs: Long): Long =
        nowEpochMs - nowElapsedRealtimeMs

    /**
     * True when `elapsedRealtime` has restarted, i.e. the device rebooted and the monotonic deadline
     * in [state] is meaningless.
     *
     * **A reboot is detected by the uptime going backwards, not by `bootEpochMs` drifting.** Both a
     * reboot and the user moving the system clock break `epoch - uptime`, but they call for opposite
     * responses: after a reboot the monotonic deadline is stale and the wall clock is the only
     * reference left, whereas after a clock change the monotonic deadline is the *only* trustworthy
     * one. Keying off the epoch delta cannot tell them apart and would make the pomodoro jump by
     * however much the clock moved, which CLAUDE.md §6 forbids and the resilience checklist §10.4
     * tests for.
     *
     * The bound is exact rather than heuristic: while a slot runs, the deadline was last written as
     * `someUptime + remaining` with `remaining ≤ slotDuration`, so `endAtElapsedRealtimeMs -
     * slotDurationMs` can never exceed the current uptime. If it does, the counter restarted.
     */
    fun hasRebooted(state: TimerState, nowElapsedRealtimeMs: Long): Boolean {
        if (state.endAtElapsedRealtimeMs <= 0L) return true
        val earliestPossibleUptime = state.endAtElapsedRealtimeMs - state.slotDurationMs
        return nowElapsedRealtimeMs < earliestPossibleUptime - REBOOT_TOLERANCE_MS
    }

    /**
     * Milliseconds left in the current slot, never negative.
     *
     * `RUNNING` prefers the monotonic deadline: it is immune to the user moving the system clock and
     * it keeps counting while the device is suspended. Only when [hasRebooted] says the monotonic
     * reference restarted do we fall back to the wall clock — comparing against a counter that went
     * back to zero would otherwise fire the timer instantly.
     */
    fun remainingMs(
        state: TimerState,
        nowEpochMs: Long,
        nowElapsedRealtimeMs: Long,
        settings: TimerSettings? = null,
    ): Long = when (state.status) {
        TimerStatus.IDLE ->
            settings?.let { state.durationMsWith(it) } ?: state.slotDurationMs

        TimerStatus.PAUSED -> state.remainingAtPauseMs.coerceAtLeast(0L)

        TimerStatus.RINGING -> 0L

        TimerStatus.RUNNING -> {
            val raw = if (hasRebooted(state, nowElapsedRealtimeMs)) {
                state.endAtEpochMs - nowEpochMs
            } else {
                state.endAtElapsedRealtimeMs - nowElapsedRealtimeMs
            }
            raw.coerceAtLeast(0L)
        }
    }

    /** True when a `RUNNING` slot has already reached its deadline and needs closing. */
    fun isExpired(state: TimerState, nowEpochMs: Long, nowElapsedRealtimeMs: Long): Boolean =
        state.status == TimerStatus.RUNNING &&
            remainingMs(state, nowEpochMs, nowElapsedRealtimeMs) == 0L

    /**
     * How much of the slot has been spent, as `0f..1f`. A zero-length slot counts as finished rather
     * than dividing by zero.
     */
    fun elapsedFraction(durationMs: Long, remainingMs: Long): Float {
        if (durationMs <= 0L) return 1f
        val elapsed = (durationMs - remainingMs).coerceIn(0L, durationMs)
        return elapsed.toFloat() / durationMs
    }

    /** How full the tomato is drawn: 1 at the start of the slot, 0 when it runs out. */
    fun fillFraction(durationMs: Long, remainingMs: Long): Float =
        1f - elapsedFraction(durationMs, remainingMs)

    /**
     * Active focus time, pauses excluded.
     *
     * It is derived rather than accumulated: because the deadline is pushed forward by exactly the
     * remaining time on every resume, `duration - remaining` *is* the time the clock actually ran.
     * Keeping a separate accumulator would be a second source of truth to reconcile after the
     * process dies. See docs/decisions/006-el-tiempo-activo-se-deriva-del-restante.md.
     */
    fun activeElapsedMs(durationMs: Long, remainingMs: Long): Long =
        (durationMs - remainingMs).coerceIn(0L, durationMs)

    /**
     * Whole seconds to display, rounded **up**, so a 25 minute slot reads `25:00` on its first frame
     * instead of flashing `24:59`.
     */
    fun displaySeconds(remainingMs: Long): Int =
        ((remainingMs.coerceAtLeast(0L) + 999L) / 1000L).toInt()

    /** `mm:ss`, or `h:mm:ss` past the hour — focus slots go up to three hours. */
    fun formatRemaining(remainingMs: Long): String {
        val total = displaySeconds(remainingMs)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    /**
     * Milliseconds until the next whole-second boundary, so the UI tick lands on the second instead
     * of drifting a little further into it with every iteration.
     */
    fun msUntilNextSecond(nowEpochMs: Long): Long {
        val offset = nowEpochMs % 1000L
        return if (offset == 0L) 1000L else 1000L - offset
    }
}
