package com.jjrapps.aquihaytomate.widget

import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.usecase.TimerMath

/**
 * Everything the widget shows for a given state, worked out without touching Android.
 *
 * @param figureMs the figure to freeze on screen, or null while running — there the `Chronometer` draws
 *   itself and the only thing needed is [tickingRemainingMs].
 * @param tickingRemainingMs what is left of the slot, for the chronometer's base. Null unless running.
 */
data class WidgetReadout(
    val fillFraction: Float,
    val dimmed: Boolean,
    val glyph: WidgetGlyph,
    val glyphLarge: Boolean,
    val figureMs: Long?,
    val tickingRemainingMs: Long?,
)

/**
 * The readout of each state, as a pure function.
 *
 * It lives apart from `WidgetUpdater` because it has changed three times over one afternoon of feedback —
 * the play glyph, its colour, what ringing looks like — and every change was invisible until the widget was
 * on a home screen. Here it is four cases that a unit test can walk through.
 *
 * The rules, which are all product decisions rather than technical ones:
 *
 * - **The glyph says what the tap will do**, not what the state is: play when the clock is stopped, pause
 *   while it runs. The state is already told by the level, the colour and — paused — the dimming.
 * - **Idle has no figure.** A stopped timer has no time to report, so the play glyph gets the whole tomato.
 * - **Ringing draws the slot that is about to run**, brim-full and in its own colour, with its length as the
 *   figure. It used to be an empty tomato with an exclamation mark, which read as an error; the state
 *   already points at the next slot, so this is simply what the tap will start.
 */
fun readoutFor(state: TimerState, remainingMs: Long): WidgetReadout = when (state.status) {
    TimerStatus.IDLE -> WidgetReadout(
        fillFraction = 1f,
        dimmed = false,
        glyph = WidgetGlyph.PLAY,
        glyphLarge = true,
        figureMs = null,
        tickingRemainingMs = null,
    )

    TimerStatus.RUNNING -> WidgetReadout(
        fillFraction = TimerMath.fillFraction(state.slotDurationMs, remainingMs),
        dimmed = false,
        glyph = WidgetGlyph.PAUSE,
        glyphLarge = false,
        figureMs = null,
        tickingRemainingMs = remainingMs,
    )

    TimerStatus.PAUSED -> WidgetReadout(
        fillFraction = TimerMath.fillFraction(state.slotDurationMs, remainingMs),
        dimmed = true,
        glyph = WidgetGlyph.PLAY,
        glyphLarge = false,
        figureMs = remainingMs,
        tickingRemainingMs = null,
    )

    TimerStatus.RINGING -> WidgetReadout(
        fillFraction = 1f,
        dimmed = false,
        glyph = WidgetGlyph.PLAY,
        glyphLarge = false,
        figureMs = state.slotDurationMs,
        tickingRemainingMs = null,
    )
}
