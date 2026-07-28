package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.TomateBright
import com.jjrapps.aquihaytomate.ui.theme.TomateFill
import java.time.LocalDate

private val CHART_HEIGHT = 56.dp
private val STROKE_WIDTH = 1.5.dp
private val END_DOT_RADIUS = 4.dp
private const val FILL_ALPHA = 0.18f

/**
 * The thirty-day trend: a polyline with a gradient fade underneath and a dot on the last point.
 *
 * No axes and no grid, like every chart here. The vertical scale is the busiest day of the window, so the
 * shape shows the rhythm rather than absolute numbers — which is what a sparkline is for.
 *
 * **Survives an empty window and a flat one**: with every day at zero the line sits on the baseline
 * instead of dividing by zero.
 */
@Composable
fun Sparkline(
    days: List<DayStats>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val linePath = remember { Path() }
    val fillPath = remember { Path() }

    Spacer(
        modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT)
            .clearAndSetSemantics { this.contentDescription = contentDescription }
            .drawBehind {
                if (days.size < 2) return@drawBehind

                val max = days.maxOf { it.completedPomodoros }.coerceAtLeast(1)
                val points = pointsFor(days, max)

                buildPaths(points, linePath, fillPath)

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        listOf(TomateFill.copy(alpha = FILL_ALPHA), Color.Transparent),
                    ),
                )
                drawPath(
                    path = linePath,
                    color = TomateBright,
                    style = Stroke(width = STROKE_WIDTH.toPx(), cap = StrokeCap.Round),
                )
                drawCircle(
                    color = TomateBright,
                    radius = END_DOT_RADIUS.toPx() / 2f,
                    center = points.last(),
                )
            },
    )
}

private fun DrawScope.pointsFor(days: List<DayStats>, max: Int): List<Offset> {
    // Inset by the stroke so the line and the end dot are not clipped at the edges.
    val inset = STROKE_WIDTH.toPx() + END_DOT_RADIUS.toPx() / 2f
    val usableHeight = (size.height - inset * 2).coerceAtLeast(1f)
    val step = size.width / (days.size - 1)

    return days.mapIndexed { index, day ->
        val fraction = day.completedPomodoros.toFloat() / max
        Offset(
            x = (index * step).coerceIn(0f, size.width),
            y = inset + usableHeight * (1f - fraction),
        )
    }
}

private fun DrawScope.buildPaths(points: List<Offset>, line: Path, fill: Path) {
    line.rewind()
    fill.rewind()

    line.moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { line.lineTo(it.x, it.y) }

    fill.moveTo(points.first().x, size.height)
    points.forEach { fill.lineTo(it.x, it.y) }
    fill.lineTo(points.last().x, size.height)
    fill.close()
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun SparklinePreview() {
    val today = LocalDate.parse("2026-04-15")
    val days = (0 until 30).map { index ->
        DayStats(
            today.minusDays((29 - index).toLong()),
            completedPomodoros = listOf(3, 5, 8, 6, 2, 0, 4)[index % 7],
        )
    }

    AquiHayTomateTheme {
        Sparkline(days = days, contentDescription = "Tendencia de 30 días")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun SparklineEmptyPreview() {
    val today = LocalDate.parse("2026-04-15")
    val days = (0 until 30).map { DayStats(today.minusDays(it.toLong())) }

    AquiHayTomateTheme {
        Sparkline(days = days, contentDescription = "Sin actividad")
    }
}
