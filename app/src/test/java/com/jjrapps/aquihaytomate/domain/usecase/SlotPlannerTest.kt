package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class SlotPlannerTest {

    private val settings = TimerSettings(
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 15,
        pomodorosPerCycle = 4,
    )

    private fun plan(
        currentType: SlotType,
        completedFocusInCycle: Int,
        completedFully: Boolean,
        slotIndex: Int = 0,
        with: TimerSettings = settings,
    ) = SlotPlanner.planNextSlot(
        currentType = currentType,
        completedFocusInCycle = completedFocusInCycle,
        currentSlotIndex = slotIndex,
        completedFully = completedFully,
        settings = with,
    )

    @Test
    fun `a completed focus leads to a short break and advances the cycle`() {
        val next = plan(SlotType.FOCUS, completedFocusInCycle = 0, completedFully = true)

        assertEquals(SlotType.SHORT_BREAK, next.type)
        assertEquals(5 * 60_000L, next.durationMs)
        assertEquals(1, next.completedFocusInCycle)
    }

    @Test
    fun `the last focus of the cycle leads to the long break`() {
        val next = plan(SlotType.FOCUS, completedFocusInCycle = 3, completedFully = true)

        assertEquals(SlotType.LONG_BREAK, next.type)
        assertEquals(15 * 60_000L, next.durationMs)
        assertEquals(4, next.completedFocusInCycle)
    }

    // CLAUDE.md §6: skipping a focus must never bring the long break closer.
    @Test
    fun `a skipped focus leads to a short break and leaves the cycle where it was`() {
        val next = plan(SlotType.FOCUS, completedFocusInCycle = 2, completedFully = false)

        assertEquals(SlotType.SHORT_BREAK, next.type)
        assertEquals(2, next.completedFocusInCycle)
    }

    @Test
    fun `skipping the fourth focus does not unlock the long break`() {
        val next = plan(SlotType.FOCUS, completedFocusInCycle = 3, completedFully = false)

        assertEquals(SlotType.SHORT_BREAK, next.type)
        assertEquals(3, next.completedFocusInCycle)
    }

    @Test
    fun `a short break leads back to focus without touching the cycle`() {
        val next = plan(SlotType.SHORT_BREAK, completedFocusInCycle = 2, completedFully = true)

        assertEquals(SlotType.FOCUS, next.type)
        assertEquals(25 * 60_000L, next.durationMs)
        assertEquals(2, next.completedFocusInCycle)
    }

    // The counter is reset on the way out of the long break, not on the way in, so the dots keep
    // reading 4/4 for the whole long break.
    @Test
    fun `the long break resets the cycle when it hands over to focus`() {
        val next = plan(SlotType.LONG_BREAK, completedFocusInCycle = 4, completedFully = true)

        assertEquals(SlotType.FOCUS, next.type)
        assertEquals(0, next.completedFocusInCycle)
    }

    @Test
    fun `a skipped break still hands over to focus`() {
        val next = plan(SlotType.SHORT_BREAK, completedFocusInCycle = 1, completedFully = false)

        assertEquals(SlotType.FOCUS, next.type)
        assertEquals(1, next.completedFocusInCycle)
    }

    @Test
    fun `the slot index always advances`() {
        assertEquals(
            8,
            plan(SlotType.FOCUS, completedFocusInCycle = 0, completedFully = true, slotIndex = 7)
                .slotIndex,
        )
    }

    @Test
    fun `a full two pomodoro cycle walks focus break focus long break`() {
        val short = settings.copy(pomodorosPerCycle = 2)

        val first = plan(SlotType.FOCUS, 0, completedFully = true, with = short)
        assertEquals(SlotType.SHORT_BREAK, first.type)
        assertEquals(1, first.completedFocusInCycle)

        val second = plan(first.type, first.completedFocusInCycle, completedFully = true, with = short)
        assertEquals(SlotType.FOCUS, second.type)
        assertEquals(1, second.completedFocusInCycle)

        val third = plan(second.type, second.completedFocusInCycle, completedFully = true, with = short)
        assertEquals(SlotType.LONG_BREAK, third.type)
        assertEquals(2, third.completedFocusInCycle)

        val fourth = plan(third.type, third.completedFocusInCycle, completedFully = true, with = short)
        assertEquals(SlotType.FOCUS, fourth.type)
        assertEquals(0, fourth.completedFocusInCycle)
    }

    // Shrinking the cycle mid-run must not leave the counter above the new limit, or the dots would
    // render out of bounds.
    @Test
    fun `shrinking the cycle clamps the counter`() {
        val next = plan(
            SlotType.SHORT_BREAK,
            completedFocusInCycle = 3,
            completedFully = true,
            with = settings.copy(pomodorosPerCycle = 2),
        )

        assertEquals(1, next.completedFocusInCycle)
    }
}
