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
    fun `the two ends of a slot sound different by default`() {
        val defaults = TimerSettings()

        assertTrue(
            "Acabar el pomodoro suena a campana: brillante y 2,6 s, elegida por oído",
            defaults.focusAlertSound == AlertSound.BELL,
        )
        assertTrue(
            "Acabar el descanso suena a cuenco: el más grave y el más largo de los cuatro",
            defaults.breakAlertSound == AlertSound.BOWL,
        )
        assertTrue(
            "Si los dos coincidieran, la mitad de la idea se pierde",
            defaults.focusAlertSound != defaults.breakAlertSound,
        )
    }

    @Test
    fun `the sound is keyed on the slot that ended, not the one coming up`() {
        val settings = TimerSettings(
            focusAlertSound = AlertSound.SOFT,
            breakAlertSound = AlertSound.BELL,
        )

        assertTrue(settings.alertSoundFor(SlotType.FOCUS) == AlertSound.SOFT)
        assertTrue(settings.alertSoundFor(SlotType.SHORT_BREAK) == AlertSound.BELL)
        assertTrue(settings.alertSoundFor(SlotType.LONG_BREAK) == AlertSound.BELL)
    }

    @Test
    fun `the repeat count is keyed on the slot that ended, like the sound is`() {
        val settings = TimerSettings(focusAlertRepeats = 2, breakAlertRepeats = 4)

        assertTrue(settings.alertRepeatsFor(SlotType.FOCUS) == 2)
        assertTrue(settings.alertRepeatsFor(SlotType.SHORT_BREAK) == 4)
        assertTrue(settings.alertRepeatsFor(SlotType.LONG_BREAK) == 4)
    }

    @Test
    fun `both repeat counts default to two plays`() {
        val settings = TimerSettings()

        // Dos y no una: una sola pasada se pierde desde la habitación de al lado, y encadenadas sin hueco
        // dos se leen como una alerta más larga. Es lo que pregunta la última página del onboarding.
        assertTrue(settings.alertRepeatsFor(SlotType.FOCUS) == 2)
        assertTrue(settings.alertRepeatsFor(SlotType.SHORT_BREAK) == 2)
        assertTrue(settings.alertRepeatsFor(SlotType.LONG_BREAK) == 2)
        assertTrue(TimerSettings.ALERT_REPEATS_RANGE == 1..10)
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
