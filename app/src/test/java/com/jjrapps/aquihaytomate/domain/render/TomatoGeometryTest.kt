package com.jjrapps.aquihaytomate.domain.render

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TomatoGeometryTest {

    private val width = 268f
    private val height = 268f
    private val amplitude = TomatoGeometry.amplitudeFor(height)

    private fun points(
        fillFraction: Float,
        phaseRad: Float = 0f,
        amplitudePx: Float = amplitude,
        stepPx: Float = 4f,
    ) = TomatoGeometry.liquidSurfacePoints(
        width = width,
        height = height,
        fillFraction = fillFraction,
        phaseRad = phaseRad,
        amplitudePx = amplitudePx,
        stepPx = stepPx,
    )

    private fun FloatArray.xs() = filterIndexed { index, _ -> index % 2 == 0 }
    private fun FloatArray.ys() = filterIndexed { index, _ -> index % 2 == 1 }

    @Test
    fun `the polyline spans the full width edge to edge`() {
        val xs = points(0.5f).xs()

        assertEquals(0f, xs.first(), 0.0001f)
        assertEquals(width, xs.last(), 0.0001f)
    }

    @Test
    fun `x coordinates increase monotonically`() {
        val xs = points(0.5f).xs()

        assertTrue(xs.zipWithNext().all { (a, b) -> b > a })
    }

    @Test
    fun `the resolution follows the step`() {
        assertTrue(points(0.5f, stepPx = 4f).size > points(0.5f, stepPx = 16f).size)
    }

    @Test
    fun `every y stays inside the box`() {
        listOf(0f, 0.13f, 0.5f, 0.87f, 1f).forEach { fill ->
            (0..7).forEach { i ->
                val phase = i * TomatoGeometry.TWO_PI / 8f
                points(fill, phaseRad = phase).ys().forEach { y ->
                    assertTrue("y=$y out of bounds at fill=$fill", y in 0f..height)
                }
            }
        }
    }

    // A brim-full tomato must look brim-full: no trough carved out of the top.
    @Test
    fun `a full tomato leaves no gap at the top`() {
        points(1f, phaseRad = 1.3f).ys().forEach { y ->
            assertEquals(0f, y, 0.0001f)
        }
    }

    @Test
    fun `an empty tomato leaves no liquid at the bottom`() {
        points(0f, phaseRad = 1.3f).ys().forEach { y ->
            assertEquals(height, y, 0.0001f)
        }
    }

    @Test
    fun `the surface is continuous between samples`() {
        val ys = points(0.5f, phaseRad = 0.7f).ys()
        // With 4 px steps and an amplitude under 6 px, no neighbouring pair can jump far.
        val maxJump = ys.zipWithNext().maxOf { (a, b) -> abs(b - a) }

        assertTrue("neighbouring samples jumped ${maxJump}px", maxJump < amplitude)
    }

    @Test
    fun `the wave stays within the requested amplitude of the baseline`() {
        val baseline = TomatoGeometry.baselineY(height, 0.5f)

        points(0.5f, phaseRad = 2.1f).ys().forEach { y ->
            assertTrue(
                "y=$y strayed more than ${amplitude}px from baseline $baseline",
                abs(y - baseline) <= amplitude + 0.0001f,
            )
        }
    }

    @Test
    fun `the baseline drops as the tomato drains`() {
        assertEquals(0f, TomatoGeometry.baselineY(height, 1f), 0.0001f)
        assertEquals(height / 2f, TomatoGeometry.baselineY(height, 0.5f), 0.0001f)
        assertEquals(height, TomatoGeometry.baselineY(height, 0f), 0.0001f)
    }

    @Test
    fun `the baseline clamps an out of range fill fraction`() {
        assertEquals(0f, TomatoGeometry.baselineY(height, 1.4f), 0.0001f)
        assertEquals(height, TomatoGeometry.baselineY(height, -0.2f), 0.0001f)
    }

    @Test
    fun `the amplitude is damped near the edges`() {
        assertEquals(0f, TomatoGeometry.effectiveAmplitude(height, 1f, amplitude), 0.0001f)
        assertEquals(0f, TomatoGeometry.effectiveAmplitude(height, 0f, amplitude), 0.0001f)
        assertEquals(amplitude, TomatoGeometry.effectiveAmplitude(height, 0.5f, amplitude), 0.0001f)

        // Two pixels from the top there is room for two pixels of wave, not the full amplitude.
        val nearlyFull = 1f - 2f / height
        assertEquals(2f, TomatoGeometry.effectiveAmplitude(height, nearlyFull, amplitude), 0.01f)
    }

    @Test
    fun `the phase moves the surface`() {
        val atZero = points(0.5f, phaseRad = 0f).ys()
        val atHalfTurn = points(0.5f, phaseRad = TomatoGeometry.TWO_PI / 2f).ys()

        assertTrue(atZero.zip(atHalfTurn).any { (a, b) -> abs(a - b) > 0.5f })
    }

    @Test
    fun `a full turn of phase returns the same surface`() {
        val atZero = points(0.5f, phaseRad = 0f).ys()
        val atFullTurn = points(0.5f, phaseRad = TomatoGeometry.TWO_PI).ys()

        atZero.zip(atFullTurn).forEach { (a, b) -> assertEquals(a, b, 0.01f) }
    }

    @Test
    fun `a degenerate box yields no points`() {
        assertEquals(0, TomatoGeometry.liquidSurfacePoints(0f, height, 0.5f, 0f).size)
        assertEquals(0, TomatoGeometry.liquidSurfacePoints(width, 0f, 0.5f, 0f).size)
    }

    @Test
    fun `a degenerate step does not hang or explode`() {
        val tiny = TomatoGeometry.liquidSurfacePoints(width, height, 0.5f, 0f, stepPx = 0f)

        assertTrue(tiny.size >= 4)
        assertEquals(width, tiny.xs().last(), 0.0001f)
    }

    @Test
    fun `flatness is reported at the extremes`() {
        assertTrue(TomatoGeometry.isFlat(height, 1f, amplitude))
        assertTrue(TomatoGeometry.isFlat(height, 0f, amplitude))
        assertFalse(TomatoGeometry.isFlat(height, 0.5f, amplitude))
    }

    @Test
    fun `zero amplitude gives a flat surface at the baseline`() {
        val baseline = TomatoGeometry.baselineY(height, 0.4f)

        points(0.4f, phaseRad = 2f, amplitudePx = 0f).ys().forEach { y ->
            assertEquals(baseline, y, 0.0001f)
        }
    }
}
