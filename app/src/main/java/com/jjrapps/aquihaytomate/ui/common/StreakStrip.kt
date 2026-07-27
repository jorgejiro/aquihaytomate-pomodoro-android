package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.usecase.StatsAggregation
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.TomateBright
import com.jjrapps.aquihaytomate.ui.theme.TomateGhost
import java.time.LocalDate

private val STRIP_HEIGHT = 8.dp
private val SEGMENT_GAP = 2.dp

/**
 * The last thirty days as a row of ticks: lit for a day with a completed pomodoro, dim for a day without.
 *
 * A plain visual rhythm, deliberately not another chart — the number beside it is the information, and
 * this is the texture of it.
 */
@Composable
fun StreakStrip(
    days: List<DayStats>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(STRIP_HEIGHT)
                .clearAndSetSemantics { this.contentDescription = contentDescription }
                .drawBehind {
                    if (days.isEmpty()) return@drawBehind

                    val gap = SEGMENT_GAP.toPx()
                    val width = (size.width - gap * (days.size - 1)) / days.size
                    days.forEachIndexed { index, day ->
                        drawRoundRect(
                            color = if (day.hasActivity) TomateBright else TomateGhost,
                            topLeft = Offset(index * (width + gap), 0f),
                            size = Size(width, size.height),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                        )
                    }
                },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun StreakStripPreview() {
    val today = LocalDate.parse("2026-04-15")
    val days = (0 until StatsAggregation.TREND_DAYS).map { index ->
        val date = today.minusDays((StatsAggregation.TREND_DAYS - 1 - index).toLong())
        DayStats(date, completedPomodoros = if (index % 7 == 3) 0 else 4)
    }

    AquiHayTomateTheme {
        StreakStrip(days = days, contentDescription = "Racha de 12 días")
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun StreakStripEmptyPreview() {
    AquiHayTomateTheme {
        StreakStrip(days = emptyList(), contentDescription = "Sin racha")
    }
}
