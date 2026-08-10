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
}
