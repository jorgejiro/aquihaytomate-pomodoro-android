package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.render.TomatoGeometry
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.LocalReducedMotion
import com.jjrapps.aquihaytomate.ui.theme.PhaseColors
import com.jjrapps.aquihaytomate.ui.theme.SurfaceHighlight
import com.jjrapps.aquihaytomate.ui.theme.phaseColorsOf
import kotlin.math.min

private const val FRONT_WAVE_PERIOD_MS = 3200
private const val BACK_WAVE_PERIOD_MS = 4700
private const val BACK_WAVE_ALPHA = 0.35f

private val OUTLINE_WIDTH = 2.dp
private val SURFACE_LINE_WIDTH = 1.5.dp

/** The wave phases, read inside a draw lambda so that only the draw phase is invalidated. */
class LiquidPhases(
    internal val front: State<Float>,
    internal val back: State<Float>,
)

/**
 * The two animated phases of the liquid.
 *
 * Hoisted out of [LiquidTomato] so that several nodes — the tomato itself and the knocked-out digits
 * clipped to it — can read the same phases and stay in register frame by frame.
 *
 * Periods that do not divide each other on purpose: 3200 ms and 4700 ms break the visible loop and the
 * surface reads as liquid rather than as a repeating sine. See docs/design-spec.md §6.2.
 */
@Composable
fun rememberLiquidPhases(): LiquidPhases {
    val reduced = LocalReducedMotion.current
    if (reduced) {
        // No infinite transition at all: with reduced motion the surface is flat and still.
        val zero = remember { mutableFloatStateOf(0f) }
        return remember { LiquidPhases(zero, zero) }
    }

    val transition = rememberInfiniteTransition(label = "liquid")
    val front = transition.animateFloat(
        initialValue = 0f,
        targetValue = TomatoGeometry.TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(FRONT_WAVE_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseFront",
    )
    val back = transition.animateFloat(
        initialValue = 0f,
        targetValue = TomatoGeometry.TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(BACK_WAVE_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseBack",
    )
    return remember(front, back) { LiquidPhases(front, back) }
}

/**
 * The circle of liquid that drains: hollow, two waves, the highlight on the surface, and the outline
 * where the liquid is gone.
 *
 * Three performance decisions, in order of importance (docs/design-spec.md §6.2):
 *
 * 1. **The phases are read inside the [Modifier.drawBehind] lambda, never outside.** Reading them in
 *    composition and passing them down as parameters would recompose this node on every frame; read
 *    inside the draw lambda, the animation invalidates the draw phase only. That is the difference
 *    between 0.3 ms and 4 ms a frame.
 * 2. **The polyline is built with `lineTo` every 4 dp**, not with béziers. At 268 dp on xxhdpi that is
 *    ~67 segments and the faceting is invisible.
 * 3. **The `Path` objects are remembered and rewound**, so the frame loop allocates nothing.
 */
@Composable
fun LiquidTomato(
    fillFraction: Float,
    colors: PhaseColors,
    modifier: Modifier = Modifier,
    phases: LiquidPhases = rememberLiquidPhases(),
) {
    val reduced = LocalReducedMotion.current
    val circlePath = remember { Path() }
    val frontPath = remember { Path() }
    val backPath = remember { Path() }

    Spacer(
        modifier.drawBehind {
            clipPath(circlePath.asCircle(size)) {
                drawRect(colors.ghost)

                if (!reduced) {
                    buildLiquidPath(
                        path = backPath,
                        fillFraction = fillFraction,
                        phaseRad = phases.back.value,
                        periods = TomatoGeometry.PERIODS_BACK,
                    )
                    drawPath(backPath, colors.fill.copy(alpha = BACK_WAVE_ALPHA))
                }

                buildLiquidPath(
                    path = frontPath,
                    fillFraction = fillFraction,
                    phaseRad = phases.front.value,
                    periods = TomatoGeometry.PERIODS_FRONT,
                )
                drawPath(frontPath, colors.fill)
                drawSurfaceLine(fillFraction, phases.front.value)
            }

            if (fillFraction < 1f) {
                val stroke = OUTLINE_WIDTH.toPx()
                drawCircle(
                    color = colors.deep,
                    radius = min(size.width, size.height) / 2f - stroke / 2f,
                    style = Stroke(width = stroke),
                )
            }
        },
    )
}

/** Reshapes [this] into the largest circle that fits [size]. Cheaper than allocating a Path a frame. */
private fun Path.asCircle(size: Size): Path {
    rewind()
    val diameter = min(size.width, size.height)
    val left = (size.width - diameter) / 2f
    val top = (size.height - diameter) / 2f
    addOval(Rect(left, top, left + diameter, top + diameter))
    return this
}

/**
 * The filled area under the wave: the surface polyline, then down the right edge, across the bottom
 * and back up the left.
 *
 * `internal` because [LiquidCountdown] builds the very same path to clip the knocked-out digits. It
 * recomputes rather than sharing a Path instance so the two nodes cannot depend on which one Compose
 * happens to draw first.
 */
internal fun DrawScope.buildLiquidPath(
    path: Path,
    fillFraction: Float,
    phaseRad: Float,
    periods: Float,
): Path {
    path.rewind()
    if (fillFraction <= 0f) return path

    val points = TomatoGeometry.liquidSurfacePoints(
        width = size.width,
        height = size.height,
        fillFraction = fillFraction,
        phaseRad = phaseRad,
        periods = periods,
        stepPx = TomatoGeometry.STEP_DP * density,
    )
    if (points.isEmpty()) return path

    path.moveTo(points[0], points[1])
    var i = 2
    while (i < points.size) {
        path.lineTo(points[i], points[i + 1])
        i += 2
    }
    path.lineTo(size.width, size.height)
    path.lineTo(0f, size.height)
    path.close()
    return path
}

/** The bright line where liquid meets air. Skipped when the tomato is full or empty. */
private fun DrawScope.drawSurfaceLine(fillFraction: Float, phaseRad: Float) {
    if (fillFraction <= 0f || fillFraction >= 1f) return

    val amplitude = TomatoGeometry.amplitudeFor(size.height)
    val points = TomatoGeometry.liquidSurfacePoints(
        width = size.width,
        height = size.height,
        fillFraction = fillFraction,
        phaseRad = phaseRad,
        amplitudePx = amplitude,
        periods = TomatoGeometry.PERIODS_FRONT,
        stepPx = TomatoGeometry.STEP_DP * density,
    )
    val strokeWidth = SURFACE_LINE_WIDTH.toPx()
    var i = 0
    while (i + 3 < points.size) {
        drawLine(
            color = SurfaceHighlight,
            start = Offset(points[i], points[i + 1]),
            end = Offset(points[i + 2], points[i + 3]),
            strokeWidth = strokeWidth,
        )
        i += 2
    }
}



@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LiquidTomatoFocusPreview() {
    AquiHayTomateTheme {
        LiquidTomato(
            fillFraction = 0.62f,
            colors = phaseColorsOf(SlotType.FOCUS),
            modifier = Modifier.size(268.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LiquidTomatoBreakPreview() {
    AquiHayTomateTheme {
        LiquidTomato(
            fillFraction = 0.35f,
            colors = phaseColorsOf(SlotType.SHORT_BREAK),
            modifier = Modifier.size(268.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LiquidTomatoFullPreview() {
    AquiHayTomateTheme {
        LiquidTomato(
            fillFraction = 1f,
            colors = phaseColorsOf(SlotType.FOCUS),
            modifier = Modifier.size(268.dp),
        )
    }
}
