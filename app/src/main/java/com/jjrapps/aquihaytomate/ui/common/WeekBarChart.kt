package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.BorderStrong
import com.jjrapps.aquihaytomate.ui.theme.NumberSmall
import com.jjrapps.aquihaytomate.ui.theme.SurfaceSunken
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.TomateBright
import com.jjrapps.aquihaytomate.ui.theme.TomateFill
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val TRACK_HEIGHT = 96.dp
private val BAR_WIDTH = 26.dp
private val BAR_CORNER = 4.dp
private val LABEL_GAP = 8.dp
private const val BAR_ENTRY_STAGGER_MS = 40
private const val BAR_ENTRY_DURATION_MS = 320

/**
 * The daily bars of a week, with a dashed goal line and today picked out.
 *
 * No axes, no grid, no frame — the rule every chart here follows. The bars grow from a common scale so
 * that two weeks can be compared by eye: the tallest bar of the week reaches the top, unless the goal is
 * higher, in which case the goal does.
 *
 * **Survives an empty week**, which is the classic regression of hand-rolled charts.
 */
@Composable
fun WeekBarChart(
    days: List<DayStats>,
    dailyGoal: Int,
    today: LocalDate,
    onDayClick: (DayStats) -> Unit,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    locale: Locale = Locale.getDefault(),
) {
    val scaleMax = maxOf(days.maxOfOrNull { it.completedPomodoros } ?: 0, dailyGoal, 1)

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(TRACK_HEIGHT),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day ->
                Bar(
                    day = day,
                    scaleMax = scaleMax,
                    dailyGoal = dailyGoal,
                    isToday = day.date == today,
                    isSelected = day.date == selectedDate,
                    onClick = { onDayClick(day) },
                    index = days.indexOf(day),
                )
            }
        }

        Spacer(Modifier.height(LABEL_GAP))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            days.forEach { day ->
                Box(Modifier.width(BAR_WIDTH), contentAlignment = Alignment.Center) {
                    Text(
                        text = day.date.dayOfWeek
                            .getDisplayName(JavaTextStyle.NARROW, locale)
                            .uppercase(locale),
                        style = NumberSmall,
                        color = if (day.date == today) TomateBright else TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun Bar(
    day: DayStats,
    scaleMax: Int,
    dailyGoal: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    index: Int,
) {
    val targetFraction = day.completedPomodoros.toFloat() / scaleMax
    // Staggered by index so the week fans in rather than snapping into place all at once.
    val fraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(
            durationMillis = BAR_ENTRY_DURATION_MS,
            delayMillis = index * BAR_ENTRY_STAGGER_MS,
        ),
        label = "bar_$index",
    )
    val goalFraction = dailyGoal.toFloat() / scaleMax
    val barColor = if (isToday) TomateBright else TomateFill
    val description = "${day.date}: ${day.completedPomodoros}"

    Box(
        modifier = Modifier
            .width(BAR_WIDTH)
            .height(TRACK_HEIGHT)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .clearAndSetSemantics { contentDescription = description }
            .drawBehind {
                drawTrack()
                drawBar(fraction, barColor, isSelected)
                drawGoalLine(goalFraction)
            },
    )
}

private fun DrawScope.drawTrack() {
    // All four corners rounded rather than just the top two: it needs a Path to do only the top, and at
    // 4 dp on a 26 dp bar the difference is not visible.
    drawRoundRect(color = SurfaceSunken, cornerRadius = CornerRadius(BAR_CORNER.toPx()))
}

private fun DrawScope.drawBar(fraction: Float, color: Color, isSelected: Boolean) {
    val clamped = fraction.coerceIn(0f, 1f)
    if (clamped <= 0f) return

    val barHeight = size.height * clamped
    val topLeft = Offset(0f, size.height - barHeight)
    val barSize = Size(size.width, barHeight)
    val corner = CornerRadius(BAR_CORNER.toPx())

    drawRoundRect(color = color, topLeft = topLeft, size = barSize, cornerRadius = corner)
    if (isSelected) {
        // A wash rather than an outline: an outline on a 3 dp tall bar is unreadable.
        drawRoundRect(
            color = TextPrimary.copy(alpha = 0.22f),
            topLeft = topLeft,
            size = barSize,
            cornerRadius = corner,
        )
    }
}

/** 1 dp dashed line, drawn over the bar so it stays readable on a tall day. */
private fun DrawScope.drawGoalLine(goalFraction: Float) {
    if (goalFraction <= 0f || goalFraction > 1f) return

    val y = size.height * (1f - goalFraction)
    drawLine(
        color = BorderStrong,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun WeekBarChartPreview() {
    val monday = LocalDate.parse("2026-04-13")
    val days = listOf(2, 6, 8, 5, 7, 1, 3).mapIndexed { index, count ->
        DayStats(monday.plusDays(index.toLong()), count, count * 25L * 60_000L)
    }

    AquiHayTomateTheme {
        WeekBarChart(
            days = days,
            dailyGoal = 8,
            today = monday.plusDays(4),
            onDayClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun WeekBarChartEmptyPreview() {
    val monday = LocalDate.parse("2026-04-13")
    val days = (0..6).map { DayStats(monday.plusDays(it.toLong())) }

    AquiHayTomateTheme {
        WeekBarChart(days = days, dailyGoal = 8, today = monday, onDayClick = {})
    }
}
