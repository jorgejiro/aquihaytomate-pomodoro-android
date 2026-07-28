package com.jjrapps.aquihaytomate.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The defaults are product decisions, and the asymmetric pair is the one worth pinning: a default that
 * flips by accident changes what the app does on a fresh install, which is the one run nobody re-tests.
 */
class TimerSettingsTest {

    @Test
    fun `the break starts by itself and the next pomodoro does not`() {
        val defaults = TimerSettings()

        assertTrue(
            "The break is earned; its clock should be running while you get up",
            defaults.autoStartBreak,
        )
        assertFalse(
            "A break often runs long on purpose, so going back to work is a decision",
            defaults.autoStartFocus,
        )
    }

    @Test
    fun `auto start is keyed on the slot coming up, not the one that ended`() {
        val breakOnly = TimerSettings(autoStartBreak = true, autoStartFocus = false)

        assertTrue(breakOnly.autoStartsInto(SlotType.SHORT_BREAK))
        assertTrue(breakOnly.autoStartsInto(SlotType.LONG_BREAK))
        assertFalse(breakOnly.autoStartsInto(SlotType.FOCUS))
    }

    @Test
    fun `the classic twenty-five five fifteen four is the starting point`() {
        val defaults = TimerSettings()

        assertTrue(defaults.focusMinutes == 25)
        assertTrue(defaults.shortBreakMinutes == 5)
        assertTrue(defaults.longBreakMinutes == 15)
        assertTrue(defaults.pomodorosPerCycle == 4)
    }
}
