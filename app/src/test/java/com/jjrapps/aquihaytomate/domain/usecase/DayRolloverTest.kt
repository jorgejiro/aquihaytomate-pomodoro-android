package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The rule that decides when a stopped timer belongs to a day that is already over. */
class DayRolloverTest {

    private val zone: ZoneId = ZoneId.of("Europe/Madrid")

    private fun at(date: String, time: String): Long =
        LocalDateTime.parse("${date}T$time").atZone(zone).toInstant().toEpochMilli()

    private fun stopped(
        status: TimerStatus = TimerStatus.IDLE,
        slotType: SlotType = SlotType.SHORT_BREAK,
        completedFocusInCycle: Int = 3,
        lastActivityEpochMs: Long = 0L,
        slotStartedAtEpochMs: Long = 0L,
        sessionId: Long = 0L,
    ) = TimerState(
        status = status,
        slotType = slotType,
        sessionId = sessionId,
        completedFocusInCycle = completedFocusInCycle,
        slotStartedAtEpochMs = slotStartedAtEpochMs,
        lastActivityEpochMs = lastActivityEpochMs,
    )

    @Test
    fun `a cycle left from an earlier day rolls over`() {
        val state = stopped(lastActivityEpochMs = at("2026-08-14", "18:20"))

        assertTrue(DayRollover.startsNewDay(state, at("2026-08-18", "09:05"), zone))
    }

    @Test
    fun `a cycle from this morning stays`() {
        val state = stopped(lastActivityEpochMs = at("2026-08-18", "09:05"))

        assertFalse(DayRollover.startsNewDay(state, at("2026-08-18", "18:40"), zone))
    }

    /** Yesterday at 23:58 and today at 00:03 are minutes apart, and still two working days. */
    @Test
    fun `midnight is the boundary, not a number of hours`() {
        val state = stopped(lastActivityEpochMs = at("2026-08-17", "23:58"))

        assertTrue(DayRollover.startsNewDay(state, at("2026-08-18", "00:03"), zone))
    }

    @Test
    fun `a running timer is never touched`() {
        val state = stopped(
            status = TimerStatus.RUNNING,
            slotType = SlotType.FOCUS,
            lastActivityEpochMs = at("2026-08-17", "23:50"),
        )

        assertFalse(DayRollover.startsNewDay(state, at("2026-08-18", "00:10"), zone))
    }

    @Test
    fun `a paused slot from another day rolls over`() {
        val state = stopped(
            status = TimerStatus.PAUSED,
            slotType = SlotType.FOCUS,
            lastActivityEpochMs = at("2026-08-14", "12:00"),
        )

        assertTrue(DayRollover.startsNewDay(state, at("2026-08-18", "09:00"), zone))
    }

    /** Moving the system clock back must not wipe a cycle the user is in the middle of. */
    @Test
    fun `a marker in the future does not roll over`() {
        val state = stopped(lastActivityEpochMs = at("2026-08-20", "10:00"))

        assertFalse(DayRollover.startsNewDay(state, at("2026-08-18", "10:00"), zone))
    }

    @Test
    fun `a timer that is already at the start of a batch has nothing to roll`() {
        val state = TimerState(
            status = TimerStatus.IDLE,
            slotType = SlotType.FOCUS,
            lastActivityEpochMs = at("2026-08-14", "18:20"),
        )

        assertFalse(DayRollover.startsNewDay(state, at("2026-08-18", "09:05"), zone))
    }

    /** States written before 1.3.0 have no stamp of their own; the older markers stand in. */
    @Test
    fun `a state from an older version falls back to the slot start`() {
        val state = stopped(slotStartedAtEpochMs = at("2026-08-14", "18:20"))

        assertTrue(DayRollover.startsNewDay(state, at("2026-08-18", "09:05"), zone))
    }

    @Test
    fun `a state from an older version falls back to the session id`() {
        val state = stopped(sessionId = at("2026-08-14", "17:00"))

        assertTrue(DayRollover.startsNewDay(state, at("2026-08-18", "09:05"), zone))
    }

    /** No marker at all means no idea, and clearing a cycle on a guess is worse than leaving it. */
    @Test
    fun `a state with no marker at all is left alone`() {
        assertFalse(DayRollover.startsNewDay(stopped(), at("2026-08-18", "09:05"), zone))
    }

    @Test
    fun `isEarlierDay ignores zero and the future`() {
        val now = at("2026-08-18", "09:05")

        assertFalse(DayRollover.isEarlierDay(0L, now, zone))
        assertFalse(DayRollover.isEarlierDay(at("2026-08-19", "09:05"), now, zone))
        assertTrue(DayRollover.isEarlierDay(at("2026-08-17", "09:05"), now, zone))
    }
}
