package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.usecase.StatsAggregation
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.Caption
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.TomateFill
import com.jjrapps.aquihaytomate.ui.theme.TomateGhost
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private const val COLUMNS = 7
private val CELL_GAP = 6.dp
private val CELL_CORNER = 8.dp
private val TODAY_RING = 1.5.dp
private val LEGEND_GAP = 10.dp
private val LEGEND_CELL = 10.dp

/** 0 is empty, 4 is at or above the daily goal. From docs/design-spec.md §5.2. */
private val LEVEL_ALPHAS = floatArrayOf(0f, 0.25f, 0.45f, 0.70f, 1.0f)

/**
 * The monthly map: seven columns, five intensities.
 *
 * **One `Canvas` with a loop of `drawRoundRect`, not a `LazyGrid`.** There are at most 35 rectangles; a
 * Canvas is cheaper, and it lets today's ring be drawn without knocking the grid out of alignment. Taps
 * are resolved with arithmetic on the tap position.
 *
 * @param grid rows of seven, nulls for the days that fall outside the month.
 */
@Composable
fun MonthHeatmap(
    grid: List<List<DayStats?>>,
    dailyGoal: Int,
    today: LocalDate,
    totalPomodoros: Int,
    onDayClick: (DayStats) -> Unit,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
) {
    val rows = grid.size

    // La altura se mide con el ancho que hay de verdad, no con un tamaño de celda fijo. `drawGrid`
    // reparte el ancho disponible entre las siete columnas, así que en cuanto la pantalla da más de
    // ~46 dp por celda la rejilla dibujada es más alta que el hueco reservado y se derrama sobre la
    // leyenda: +39 dp en un móvil de 411 dp y +317 dp en una tablet de 800 dp. Ver gridHeight.
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val alto = gridHeight(maxWidth, rows)
        Column(Modifier.fillMaxWidth()) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(alto)
                    .pointerInput(grid) {
                        detectTapGestures { offset ->
                            cellAt(offset, size.width.toFloat(), grid)?.let(onDayClick)
                        }
                    }
                    .drawBehind { drawGrid(grid, dailyGoal, today, selectedDate) },
            )

            Spacer(Modifier.height(LEGEND_GAP))
            Legend(totalPomodoros)
        }
    }
}

/**
 * Alto exacto de la rejilla para un ancho dado: la misma aritmética que [drawGrid], que reparte el ancho
 * entre las siete columnas y usa celdas cuadradas.
 *
 * Aparte y `internal` para poder fijarlo con un test unitario. El bug que corrige era invisible en un
 * móvil estrecho —tres dp de derrame— y arrasaba la pantalla en una tablet, que es la clase de fallo que
 * solo se ve cuando ya está publicado.
 */
internal fun gridHeight(width: Dp, rows: Int): Dp {
    if (rows <= 0 || width <= 0.dp) return 0.dp
    val cell = (width - CELL_GAP * (COLUMNS - 1)) / COLUMNS
    return cell * rows + CELL_GAP * (rows - 1)
}

/**
 * Which day a tap landed on, or null for a gap or a padding cell.
 *
 * The cell pitch is derived from the actual width rather than assumed, so this stays correct at any font
 * scale or screen width.
 */
private fun cellAt(offset: Offset, widthPx: Float, grid: List<List<DayStats?>>): DayStats? {
    if (grid.isEmpty() || widthPx <= 0f) return null

    val pitch = widthPx / COLUMNS
    val column = (offset.x / pitch).toInt().coerceIn(0, COLUMNS - 1)
    val row = (offset.y / pitch).toInt()
    return grid.getOrNull(row)?.getOrNull(column)
}

private fun DrawScope.drawGrid(
    grid: List<List<DayStats?>>,
    dailyGoal: Int,
    today: LocalDate,
    selectedDate: LocalDate?,
) {
    if (grid.isEmpty()) return

    val gap = CELL_GAP.toPx()
    val cell = (size.width - gap * (COLUMNS - 1)) / COLUMNS
    val corner = CornerRadius(CELL_CORNER.toPx())

    grid.forEachIndexed { row, week ->
        week.forEachIndexed { column, day ->
            if (day == null) return@forEachIndexed

            val topLeft = Offset(column * (cell + gap), row * (cell + gap))
            val cellSize = Size(cell, cell)

            drawRoundRect(
                color = colorForLevel(StatsAggregation.heatLevel(day.completedPomodoros, dailyGoal)),
                topLeft = topLeft,
                size = cellSize,
                cornerRadius = corner,
            )

            if (day.date == today) {
                val stroke = TODAY_RING.toPx()
                drawRoundRect(
                    color = TextPrimary,
                    topLeft = Offset(topLeft.x + stroke / 2f, topLeft.y + stroke / 2f),
                    size = Size(cell - stroke, cell - stroke),
                    cornerRadius = corner,
                    style = Stroke(stroke),
                )
            }
            if (day.date == selectedDate) {
                drawRoundRect(
                    color = TextPrimary.copy(alpha = 0.28f),
                    topLeft = topLeft,
                    size = cellSize,
                    cornerRadius = corner,
                )
            }
        }
    }
}

private fun colorForLevel(level: Int): Color {
    val alpha = LEVEL_ALPHAS[level.coerceIn(0, LEVEL_ALPHAS.lastIndex)]
    return if (alpha <= 0f) TomateGhost else TomateFill.copy(alpha = alpha)
}

@Composable
private fun Legend(totalPomodoros: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.heatmap_less), style = Caption, color = TextMuted)
            Spacer(Modifier.width(6.dp))
            LEVEL_ALPHAS.indices.forEach { level ->
                Spacer(
                    Modifier
                        .size(LEGEND_CELL)
                        .drawBehind {
                            drawRoundRect(
                                color = colorForLevel(level),
                                cornerRadius = CornerRadius(2.dp.toPx()),
                            )
                        },
                )
                Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.width(3.dp))
            Text(text = stringResource(R.string.heatmap_more), style = Caption, color = TextMuted)
        }
        Text(
            text = pluralStringResource(
                R.plurals.stats_pomodoros_short,
                totalPomodoros,
                totalPomodoros,
            ),
            style = Caption,
            color = TextMuted,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun MonthHeatmapPreview() {
    val month = YearMonth.of(2026, 4)
    val days = (1..30).map { day ->
        DayStats(month.atDay(day), completedPomodoros = (day * 7) % 11)
    }
    val grid = StatsAggregation.monthGrid(
        days,
        month,
        StatsAggregation.weekFieldsFor(Locale.forLanguageTag("es-ES")),
    )

    AquiHayTomateTheme {
        MonthHeatmap(
            grid = grid,
            dailyGoal = 8,
            today = month.atDay(15),
            totalPomodoros = days.sumOf { it.completedPomodoros },
            onDayClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun MonthHeatmapEmptyPreview() {
    val month = YearMonth.of(2026, 4)
    val grid = StatsAggregation.monthGrid(
        emptyList(),
        month,
        StatsAggregation.weekFieldsFor(Locale.forLanguageTag("es-ES")),
    )

    AquiHayTomateTheme {
        MonthHeatmap(
            grid = grid,
            dailyGoal = 8,
            today = month.atDay(15),
            totalPomodoros = 0,
            onDayClick = {},
        )
    }
}
