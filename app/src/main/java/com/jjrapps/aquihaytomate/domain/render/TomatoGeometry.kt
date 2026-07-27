package com.jjrapps.aquihaytomate.domain.render

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin

/**
 * The shape of the draining liquid, as pure arithmetic with no Compose and no Android.
 *
 * The app draws it with a Compose `Canvas` and the widget rasterises it with an
 * `android.graphics.Canvas`. Both go through this file so the two renderings cannot drift — the same
 * reason `TimerMath` and `SlotPlanner` exist. See docs/design-spec.md §6.1.
 */
object TomatoGeometry {

    const val TWO_PI: Float = (2.0 * PI).toFloat()

    /** Wave periods across the width, for the front and back waves. Coprime-ish on purpose: two
     * waves whose periods do not divide each other stop the surface from looking like a loop. */
    const val PERIODS_FRONT = 1.25f
    const val PERIODS_BACK = 1.7f

    /** Wave amplitude as a fraction of the box height, before the edge damping below. */
    const val AMPLITUDE_FRACTION = 0.022f

    /** Polyline resolution. At 268 dp on xxhdpi this is ~67 segments, where faceting is invisible. */
    const val STEP_DP = 4f

    /** Where the surface sits with no wave: 0 at the top when full, [height] when empty. */
    fun baselineY(height: Float, fillFraction: Float): Float =
        height * (1f - fillFraction.coerceIn(0f, 1f))

    /**
     * The amplitude actually usable at this fill level.
     *
     * Damped by the distance to the nearest edge, which is what makes a full tomato read as full and
     * an empty one as empty: without this, a wave at `fillFraction = 1` would carve a trough out of
     * the top and the circle would never look brim-full.
     */
    fun effectiveAmplitude(height: Float, fillFraction: Float, amplitudePx: Float): Float {
        if (amplitudePx <= 0f || height <= 0f) return 0f
        val baseline = baselineY(height, fillFraction)
        val headroom = min(baseline, height - baseline)
        return min(amplitudePx, headroom).coerceAtLeast(0f)
    }

    /** Default amplitude for a box of this height. */
    fun amplitudeFor(height: Float): Float = height * AMPLITUDE_FRACTION

    /**
     * Points of the liquid surface, left to right, as `[x0, y0, x1, y1, …]` in box coordinates.
     *
     * A flat array rather than a list of points because this runs on every animation frame in two
     * renderers; boxing 68 pairs at 120 Hz is allocation we do not need.
     *
     * The first point always sits at `x = 0` and the last exactly at `x = width`, so callers can
     * close the path down the sides without a seam.
     *
     * @param fillFraction 1 is brim-full, 0 is empty.
     * @param phaseRad animation phase; 0 gives a wave rising to the right of centre.
     * @param periods how many full waves fit across [width].
     * @param stepPx horizontal resolution; clamped so a degenerate value cannot loop forever.
     */
    fun liquidSurfacePoints(
        width: Float,
        height: Float,
        fillFraction: Float,
        phaseRad: Float,
        amplitudePx: Float = amplitudeFor(height),
        periods: Float = PERIODS_FRONT,
        stepPx: Float = 4f,
    ): FloatArray {
        if (width <= 0f || height <= 0f) return FloatArray(0)

        val baseline = baselineY(height, fillFraction)
        val amplitude = effectiveAmplitude(height, fillFraction, amplitudePx)
        val step = stepPx.coerceIn(0.5f, width)
        val segments = ceil(width / step).toInt().coerceAtLeast(1)

        val points = FloatArray((segments + 1) * 2)
        for (i in 0..segments) {
            // Land the last sample exactly on the right edge instead of overshooting it.
            val x = if (i == segments) width else min(i * step, width)
            val angle = phaseRad + TWO_PI * periods * (x / width)
            val y = (baseline + amplitude * sin(angle)).coerceIn(0f, height)
            points[i * 2] = x
            points[i * 2 + 1] = y
        }
        return points
    }

    /**
     * Whether the surface is flat, i.e. the wave has nothing to say at this fill level. Callers use
     * it to skip building a polyline when a straight line will do.
     */
    fun isFlat(height: Float, fillFraction: Float, amplitudePx: Float): Boolean =
        abs(effectiveAmplitude(height, fillFraction, amplitudePx)) < 0.01f
}
