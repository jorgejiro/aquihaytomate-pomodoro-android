package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerMathTest {

    private val epochStart = 1_800_000_000_000L
    private val elapsedStart = 90_000_000L
    private val twentyFiveMinutes = 25 * 60_000L

    private fun running(
        remaining: Long = twentyFiveMinutes,
        nowEpoch: Long = epochStart,
        nowElapsed: Long = elapsedStart,
    ) = TimerState(
        status = TimerStatus.RUNNING,
        slotType = SlotType.FOCUS,
        sessionId = nowEpoch,
        slotDurationMs = twentyFiveMinutes,
        slotStartedAtEpochMs = nowEpoch,
        endAtEpochMs = nowEpoch + remaining,
        endAtElapsedRealtimeMs = nowElapsed + remaining,
        bootEpochMs = TimerMath.bootEpochMs(nowEpoch, nowElapsed),
    )

    @Test
    fun `remaining counts down with the monotonic clock`() {
        val state = running()

        assertEquals(
            twentyFiveMinutes - 60_000L,
            TimerMath.remainingMs(state, epochStart + 60_000L, elapsedStart + 60_000L),
        )
    }

    @Test
    fun `remaining never goes negative`() {
        val state = running()

        assertEquals(
            0L,
            TimerMath.remainingMs(state, epochStart + 9_999_999L, elapsedStart + 9_999_999L),
        )
    }

    // The rule from CLAUDE.md §6: changing the system clock must not make the pomodoro jump.
    @Test
    fun `moving the wall clock forward one hour does not change the remaining time`() {
        val state = running()
        val oneHour = 3_600_000L

        val remaining = TimerMath.remainingMs(
            state,
            nowEpochMs = epochStart + 60_000L + oneHour,
            nowElapsedRealtimeMs = elapsedStart + 60_000L,
        )

        assertEquals(twentyFiveMinutes - 60_000L, remaining)
    }

    @Test
    fun `moving the wall clock backwards does not change the remaining time`() {
        val state = running()

        val remaining = TimerMath.remainingMs(
            state,
            nowEpochMs = epochStart + 60_000L - 3_600_000L,
            nowElapsedRealtimeMs = elapsedStart + 60_000L,
        )

        assertEquals(twentyFiveMinutes - 60_000L, remaining)
    }

    // After a reboot elapsedRealtime restarts from zero; trusting it would fire the timer at once.
    @Test
    fun `after a reboot the wall clock deadline takes over`() {
        val state = running()
        val fiveMinutesLater = epochStart + 5 * 60_000L

        val remaining = TimerMath.remainingMs(
            state,
            nowEpochMs = fiveMinutesLater,
            nowElapsedRealtimeMs = 12_000L, // device has just booted
        )

        assertEquals(twentyFiveMinutes - 5 * 60_000L, remaining)
        assertTrue(TimerMath.hasRebooted(state, 12_000L))
    }

    // A clock change must NOT look like a reboot: the uptime carries on regardless.
    @Test
    fun `moving the wall clock is not mistaken for a reboot`() {
        val state = running()

        assertFalse(TimerMath.hasRebooted(state, elapsedStart + 60_000L))
    }

    @Test
    fun `the uptime bound tolerates sampling skew`() {
        val state = running()
        val earliestPossible = state.endAtElapsedRealtimeMs - state.slotDurationMs

        assertFalse(
            TimerMath.hasRebooted(state, earliestPossible - TimerMath.REBOOT_TOLERANCE_MS + 1),
        )
        assertTrue(
            TimerMath.hasRebooted(state, earliestPossible - TimerMath.REBOOT_TOLERANCE_MS - 1),
        )
    }

    @Test
    fun `a state with no monotonic deadline is treated as rebooted`() {
        assertTrue(TimerMath.hasRebooted(TimerState.EMPTY, elapsedStart))
    }

    @Test
    fun `idle reports the duration from live settings`() {
        val settings = TimerSettings(focusMinutes = 40)
        val state = TimerState.idle(TimerSettings(), SlotType.FOCUS)

        assertEquals(
            40 * 60_000L,
            TimerMath.remainingMs(state, epochStart, elapsedStart, settings),
        )
    }

    @Test
    fun `paused reports the frozen remaining time regardless of the clock`() {
        val state = TimerState(
            status = TimerStatus.PAUSED,
            slotDurationMs = twentyFiveMinutes,
            remainingAtPauseMs = 754_000L,
        )

        assertEquals(754_000L, TimerMath.remainingMs(state, epochStart, elapsedStart))
        assertEquals(
            754_000L,
            TimerMath.remainingMs(state, epochStart + 10_000_000L, elapsedStart + 10_000_000L),
        )
    }

    @Test
    fun `ringing reports zero`() {
        val state = TimerState(status = TimerStatus.RINGING, slotDurationMs = twentyFiveMinutes)

        assertEquals(0L, TimerMath.remainingMs(state, epochStart, elapsedStart))
    }

    @Test
    fun `expired only applies to a running slot`() {
        val past = running(remaining = -1_000L)
        assertTrue(TimerMath.isExpired(past, epochStart, elapsedStart))

        assertFalse(TimerMath.isExpired(running(), epochStart, elapsedStart))
        assertFalse(
            TimerMath.isExpired(
                TimerState(status = TimerStatus.RINGING),
                epochStart,
                elapsedStart,
            ),
        )
    }

    @Test
    fun `fill fraction goes from one to zero`() {
        assertEquals(1f, TimerMath.fillFraction(twentyFiveMinutes, twentyFiveMinutes), 0.0001f)
        assertEquals(0.5f, TimerMath.fillFraction(twentyFiveMinutes, twentyFiveMinutes / 2), 0.0001f)
        assertEquals(0f, TimerMath.fillFraction(twentyFiveMinutes, 0L), 0.0001f)
    }

    @Test
    fun `fill fraction survives a zero length slot`() {
        assertEquals(0f, TimerMath.fillFraction(0L, 0L), 0.0001f)
    }

    @Test
    fun `fill fraction clamps when the remaining time is out of range`() {
        assertEquals(1f, TimerMath.fillFraction(twentyFiveMinutes, twentyFiveMinutes * 2), 0.0001f)
        assertEquals(0f, TimerMath.fillFraction(twentyFiveMinutes, -5_000L), 0.0001f)
    }

    @Test
    fun `active elapsed time is the slot length minus what is left`() {
        assertEquals(
            5 * 60_000L,
            TimerMath.activeElapsedMs(twentyFiveMinutes, 20 * 60_000L),
        )
        assertEquals(twentyFiveMinutes, TimerMath.activeElapsedMs(twentyFiveMinutes, 0L))
        assertEquals(0L, TimerMath.activeElapsedMs(twentyFiveMinutes, twentyFiveMinutes))
    }

    // A fresh 25 minute slot must read 25:00, not 24:59.
    @Test
    fun `display seconds round up`() {
        assertEquals(1500, TimerMath.displaySeconds(twentyFiveMinutes))
        assertEquals(1500, TimerMath.displaySeconds(twentyFiveMinutes - 1))
        assertEquals(1, TimerMath.displaySeconds(1L))
        assertEquals(0, TimerMath.displaySeconds(0L))
        assertEquals(0, TimerMath.displaySeconds(-500L))
    }

    @Test
    fun `format uses mm ss below the hour and h mm ss above it`() {
        assertEquals("25:00", TimerMath.formatRemaining(twentyFiveMinutes))
        assertEquals("18:42", TimerMath.formatRemaining(18 * 60_000L + 42_000L))
        assertEquals("00:09", TimerMath.formatRemaining(8_500L))
        assertEquals("00:00", TimerMath.formatRemaining(0L))
        assertEquals("1:05:03", TimerMath.formatRemaining(3_903_000L))
        assertEquals("3:00:00", TimerMath.formatRemaining(180 * 60_000L))
    }

    @Test
    fun `the tick aligns to the next whole second`() {
        assertEquals(1000L, TimerMath.msUntilNextSecond(1_000L))
        assertEquals(700L, TimerMath.msUntilNextSecond(1_300L))
        assertEquals(1L, TimerMath.msUntilNextSecond(1_999L))
    }
}
