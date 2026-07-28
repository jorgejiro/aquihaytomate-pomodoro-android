package com.jjrapps.aquihaytomate.widget

import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the widget shows in each of the four states.
 *
 * These rules changed three times over one afternoon of feedback and every mistake was invisible until the
 * widget was sitting on a home screen — an exclamation mark that read as an error, a glyph the same colour
 * as the liquid behind it. Now they are a pure function with a test.
 */
class WidgetReadoutTest {

    private val focusMs = 25 * 60_000L
    private val breakMs = 5 * 60_000L

    private fun state(status: TimerStatus, slotType: SlotType, durationMs: Long) = TimerState(
        status = status,
        slotType = slotType,
        slotDurationMs = durationMs,
    )

    /** A stopped timer is all glyph: nothing to report, so the play sign gets the whole tomato. */
    @Test
    fun `idle shows a full tomato with the large play glyph and no figure`() {
        val readout = readoutFor(state(TimerStatus.IDLE, SlotType.FOCUS, focusMs), remainingMs = focusMs)

        assertEquals(1f, readout.fillFraction, 0.0001f)
        assertEquals(WidgetGlyph.PLAY, readout.glyph)
        assertTrue(readout.glyphLarge)
        assertNull(readout.figureMs)
        assertNull(readout.tickingRemainingMs)
        assertFalse(readout.dimmed)
    }

    @Test
    fun `running hands the figure to the chronometer and offers pause`() {
        val readout = readoutFor(
            state(TimerStatus.RUNNING, SlotType.FOCUS, focusMs),
            remainingMs = 10 * 60_000L,
        )

        assertEquals(WidgetGlyph.PAUSE, readout.glyph)
        assertFalse(readout.glyphLarge)
        assertEquals(10 * 60_000L, readout.tickingRemainingMs)
        assertNull("The chronometer draws itself; a frozen figure would fight it", readout.figureMs)
        assertEquals(0.4f, readout.fillFraction, 0.0001f)
    }

    @Test
    fun `pausing freezes the figure, dims the liquid and offers resume`() {
        val readout = readoutFor(
            state(TimerStatus.PAUSED, SlotType.FOCUS, focusMs),
            remainingMs = 12 * 60_000L,
        )

        assertEquals(WidgetGlyph.PLAY, readout.glyph)
        assertEquals(12 * 60_000L, readout.figureMs)
        assertNull(readout.tickingRemainingMs)
        assertTrue("The dimmed liquid is what says 'stopped'", readout.dimmed)
    }

    /**
     * The one that came from real use: ringing used to draw an empty tomato with an exclamation mark, which
     * looked like something had gone wrong. The state already points at the next slot, so the widget shows
     * that slot ready to go — full, its own colour, its length, and a play sign.
     */
    @Test
    fun `ringing shows the next slot ready rather than a warning`() {
        val readout = readoutFor(
            state(TimerStatus.RINGING, SlotType.SHORT_BREAK, breakMs),
            remainingMs = 0L,
        )

        assertEquals("A full tomato, not an empty one", 1f, readout.fillFraction, 0.0001f)
        assertEquals("The length of the slot the tap will start", breakMs, readout.figureMs)
        assertEquals(WidgetGlyph.PLAY, readout.glyph)
        assertFalse("The figure needs the room, so the glyph is the small one", readout.glyphLarge)
        assertNull(readout.tickingRemainingMs)
    }

    // The colour comes from state.slotType, which in RINGING is already the slot coming up: amber for a
    // break, red for focus. This pins the assumption the readout depends on.
    @Test
    fun `ringing after a break points at focus`() {
        val afterBreak = state(TimerStatus.RINGING, SlotType.FOCUS, focusMs)

        assertEquals(focusMs, readoutFor(afterBreak, remainingMs = 0L).figureMs)
        assertEquals(SlotType.FOCUS, afterBreak.slotType)
    }
}
