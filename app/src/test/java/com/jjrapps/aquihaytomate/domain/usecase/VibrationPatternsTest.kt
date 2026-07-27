package com.jjrapps.aquihaytomate.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VibrationPatternsTest {

    @Test
    fun `zero seconds means no vibration at all`() {
        assertEquals(0, VibrationPatterns.waveform(0).size)
    }

    @Test
    fun `a negative value is treated as off`() {
        assertEquals(0, VibrationPatterns.waveform(-3).size)
    }

    @Test
    fun `the waveform starts with a zero delay`() {
        assertEquals(0L, VibrationPatterns.waveform(5).first())
    }

    @Test
    fun `the waveform alternates pulse and gap`() {
        val timings = VibrationPatterns.waveform(5)

        timings.drop(1).forEachIndexed { index, value ->
            val expected = if (index % 2 == 0) VibrationPatterns.PULSE_MS else VibrationPatterns.GAP_MS
            assertTrue(
                "entry $index should not exceed $expected but was $value",
                value <= expected,
            )
            assertTrue("entry $index must be positive", value > 0L)
        }
    }

    @Test
    fun `the waveform never overshoots the configured seconds`() {
        (1..30).forEach { seconds ->
            val total = VibrationPatterns.durationMs(VibrationPatterns.waveform(seconds))
            assertTrue(
                "$seconds s produced ${total}ms",
                total <= seconds * 1000L,
            )
        }
    }

    // The point of the setting is that five seconds feels like five seconds.
    @Test
    fun `the waveform gets close to the configured seconds`() {
        (1..30).forEach { seconds ->
            val budget = seconds * 1000L
            val total = VibrationPatterns.durationMs(VibrationPatterns.waveform(seconds))
            val shortfall = budget - total
            assertTrue(
                "$seconds s fell ${shortfall}ms short",
                shortfall < VibrationPatterns.PULSE_MS + VibrationPatterns.GAP_MS,
            )
        }
    }

    @Test
    fun `it never ends on a gap`() {
        (1..30).forEach { seconds ->
            val timings = VibrationPatterns.waveform(seconds)
            // Entries after the leading delay alternate pulse, gap, pulse… so an even count means
            // the last one is a pulse.
            assertTrue("$seconds s ended on a gap", timings.size % 2 == 0)
        }
    }

    @Test
    fun `a single short pulse fits one second`() {
        assertEquals(listOf(0L, 400L, 250L, 350L), VibrationPatterns.waveform(1).toList())
    }

    @Test
    fun `the value is clamped to the settings range`() {
        val beyondMax = VibrationPatterns.durationMs(VibrationPatterns.waveform(999))

        assertTrue(beyondMax <= 30_000L)
    }
}
