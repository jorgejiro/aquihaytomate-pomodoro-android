package com.jjrapps.aquihaytomate.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepScreenOnModeTest {

    @Test
    fun `never keeps the screen on under no circumstances`() {
        assertFalse(KeepScreenOnMode.NEVER.shouldKeepScreenOn(charging = false))
        assertFalse(KeepScreenOnMode.NEVER.shouldKeepScreenOn(charging = true))
    }

    @Test
    fun `while charging tracks the charger`() {
        assertFalse(KeepScreenOnMode.WHILE_CHARGING.shouldKeepScreenOn(charging = false))
        assertTrue(KeepScreenOnMode.WHILE_CHARGING.shouldKeepScreenOn(charging = true))
    }

    @Test
    fun `always ignores the charger`() {
        assertTrue(KeepScreenOnMode.ALWAYS.shouldKeepScreenOn(charging = false))
        assertTrue(KeepScreenOnMode.ALWAYS.shouldKeepScreenOn(charging = true))
    }

    /** Only the middle mode justifies registering a battery receiver. */
    @Test
    fun `only while charging depends on the charger`() {
        assertFalse(KeepScreenOnMode.NEVER.dependsOnCharging)
        assertTrue(KeepScreenOnMode.WHILE_CHARGING.dependsOnCharging)
        assertFalse(KeepScreenOnMode.ALWAYS.dependsOnCharging)
    }

    @Test
    fun `ids are stable and round-trip`() {
        KeepScreenOnMode.entries.forEach { mode ->
            assertEquals(mode, KeepScreenOnMode.fromId(mode.id))
        }
    }

    @Test
    fun `an unknown or missing id falls back to the default`() {
        assertEquals(KeepScreenOnMode.DEFAULT, KeepScreenOnMode.fromId(null))
        assertEquals(KeepScreenOnMode.DEFAULT, KeepScreenOnMode.fromId("whatever"))
    }

    /** The default the author asked for, and what a fresh install gets. */
    @Test
    fun `the default is while charging`() {
        assertEquals(KeepScreenOnMode.WHILE_CHARGING, KeepScreenOnMode.DEFAULT)
        assertEquals(KeepScreenOnMode.WHILE_CHARGING, TimerSettings().keepScreenOn)
    }
    @Test
    fun `the toggle turns any other mode into always and remembers it`() {
        listOf(KeepScreenOnMode.NEVER, KeepScreenOnMode.WHILE_CHARGING).forEach { mode ->
            val toggle = mode.toggleAlways(restore = null)
            assertEquals(KeepScreenOnMode.ALWAYS, toggle.mode)
            assertEquals(mode, toggle.restore)
        }
    }

    /** Switching it back off must not take away "while charging", nor force it on someone who had never. */
    @Test
    fun `the toggle turns always back into the remembered mode`() {
        listOf(KeepScreenOnMode.NEVER, KeepScreenOnMode.WHILE_CHARGING).forEach { mode ->
            val toggle = KeepScreenOnMode.ALWAYS.toggleAlways(restore = mode)
            assertEquals(mode, toggle.mode)
            assertEquals(null, toggle.restore)
        }
    }

    /** Always chosen in Settings leaves nothing to go back to: the default is the safe answer. */
    @Test
    fun `turning always off with nothing remembered falls back to the default`() {
        assertEquals(KeepScreenOnMode.DEFAULT, KeepScreenOnMode.ALWAYS.toggleAlways(restore = null).mode)
        assertEquals(
            KeepScreenOnMode.DEFAULT,
            KeepScreenOnMode.ALWAYS.toggleAlways(restore = KeepScreenOnMode.ALWAYS).mode,
        )
    }
}
