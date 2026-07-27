package com.jjrapps.aquihaytomate.ui.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.model.PeriodStats
import com.jjrapps.aquihaytomate.domain.model.StreakInfo
import com.jjrapps.aquihaytomate.domain.usecase.StatsAggregation
import com.jjrapps.aquihaytomate.ui.common.HairlineDivider
import com.jjrapps.aquihaytomate.ui.common.MonthHeatmap
import com.jjrapps.aquihaytomate.ui.common.SectionLabel
import com.jjrapps.aquihaytomate.ui.common.Sparkline
import com.jjrapps.aquihaytomate.ui.common.StatCell
import com.jjrapps.aquihaytomate.ui.common.StreakStrip
import com.jjrapps.aquihaytomate.ui.common.WeekBarChart
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.Caption
import com.jjrapps.aquihaytomate.ui.theme.TextGhost
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

private val SCREEN_PADDING = 20.dp
private val SECTION_TOP = 20.dp
private val SECTION_BOTTOM = 8.dp
private val SECTION_GAP = 28.dp
private val ARROW_SIZE = 28.dp

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    StatsContent(
        state = state,
        onPreviousWeek = viewModel::onPreviousWeek,
        onNextWeek = viewModel::onNextWeek,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onDaySelected = viewModel::onDaySelected,
        modifier = modifier,
    )
}

@Composable
private fun StatsContent(
    state: StatsUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDaySelected: (DayStats) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        StatsUiState.Loading -> Box(modifier.fillMaxSize())
        is StatsUiState.Success -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SCREEN_PADDING),
        ) {
            TodaySection(state)
            Spacer(Modifier.height(SECTION_GAP))
            HairlineDivider()

            WeekSection(state, onPreviousWeek, onNextWeek, onDaySelected)
            HairlineDivider()

            MonthSection(state, onPreviousMonth, onNextMonth, onDaySelected)
            HairlineDivider()

            TrendSection(state)
            Spacer(Modifier.height(SECTION_GAP))
        }
    }
}

@Composable
private fun TodaySection(state: StatsUiState.Success) {
    Spacer(Modifier.height(SECTION_TOP))
    SectionLabel(stringResource(R.string.stats_today))
    Spacer(Modifier.height(SECTION_BOTTOM))

    Row(Modifier.fillMaxWidth()) {
        StatCell(
            value = state.today.completedPomodoros.toString(),
            label = pluralStringResource(
                R.plurals.stats_pomodoros_label,
                state.today.completedPomodoros,
            ),
            modifier = Modifier.weight(1f),
        )
        StatCell(
            value = formatDuration(state.today.focusedMs),
            label = stringResource(R.string.stats_focused),
            modifier = Modifier.weight(1f),
        )
        StatCell(
            value = state.streak.current.toString(),
            label = stringResource(R.string.stats_streak),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WeekSection(
    state: StatsUiState.Success,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDaySelected: (DayStats) -> Unit,
) {
    Spacer(Modifier.height(SECTION_TOP))
    SectionLabel(
        text = weekLabel(state),
        trailing = {
            PagerArrows(
                onPrevious = onPrevious,
                onNext = onNext,
                nextEnabled = state.canGoToNextWeek,
            )
        },
    )
    Spacer(Modifier.height(SECTION_BOTTOM))

    WeekBarChart(
        days = state.week.days,
        dailyGoal = state.dailyGoal,
        today = state.currentDate,
        onDayClick = onDaySelected,
        selectedDate = state.selectedDay?.date,
    )
    DayDetail(state.selectedDay)
    Spacer(Modifier.height(SECTION_GAP))
}

@Composable
private fun MonthSection(
    state: StatsUiState.Success,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDaySelected: (DayStats) -> Unit,
) {
    Spacer(Modifier.height(SECTION_TOP))
    SectionLabel(
        text = monthLabel(state.displayedMonth),
        trailing = {
            PagerArrows(
                onPrevious = onPrevious,
                onNext = onNext,
                nextEnabled = state.canGoToNextMonth,
            )
        },
    )
    Spacer(Modifier.height(SECTION_BOTTOM))

    MonthHeatmap(
        grid = state.monthGrid,
        dailyGoal = state.dailyGoal,
        today = state.currentDate,
        totalPomodoros = state.month.totalPomodoros,
        onDayClick = onDaySelected,
        selectedDate = state.selectedDay?.date,
    )
    Spacer(Modifier.height(SECTION_GAP))
}

@Composable
private fun TrendSection(state: StatsUiState.Success) {
    Spacer(Modifier.height(SECTION_TOP))
    SectionLabel(
        pluralStringResource(
            R.plurals.stats_last_days,
            StatsAggregation.TREND_DAYS,
            StatsAggregation.TREND_DAYS,
        ),
    )
    Spacer(Modifier.height(SECTION_BOTTOM))

    StatCell(
        value = state.streak.best.toString(),
        label = stringResource(R.string.stats_best_streak),
    )
    Spacer(Modifier.height(SECTION_BOTTOM))

    StreakStrip(
        days = state.trend.days,
        contentDescription = stringResource(
            R.string.a11y_streak_strip,
            state.trend.activeDays,
            state.trend.days.size,
        ),
    )
    Spacer(Modifier.height(SECTION_GAP))

    Sparkline(
        days = state.trend.days,
        contentDescription = stringResource(
            R.string.a11y_trend,
            state.trend.totalPomodoros,
            state.trend.days.size,
        ),
    )
}

/** The detail line that replaces a floating tooltip. See docs/design-spec.md §5.2. */
@Composable
private fun DayDetail(day: DayStats?) {
    if (day == null) return

    Spacer(Modifier.height(SECTION_BOTTOM))
    Text(
        text = stringResource(
            R.string.stats_day_detail,
            day.date.format(DateTimeFormatter.ofPattern(DAY_PATTERN)),
            pluralStringResource(
                R.plurals.stats_pomodoros_short,
                day.completedPomodoros,
                day.completedPomodoros,
            ),
            formatDuration(day.focusedMs),
        ),
        style = Caption,
        color = TextMuted,
    )
}

@Composable
private fun PagerArrows(onPrevious: () -> Unit, onNext: () -> Unit, nextEnabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Arrow(
            glyph = "‹",
            description = stringResource(R.string.a11y_previous_period),
            onClick = onPrevious,
            enabled = true,
        )
        Arrow(
            glyph = "›",
            description = stringResource(R.string.a11y_next_period),
            onClick = onNext,
            enabled = nextEnabled,
        )
    }
}

@Composable
private fun Arrow(
    glyph: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Box(
        modifier = Modifier
            .size(ARROW_SIZE)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClickLabel = description,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            style = Caption,
            color = if (enabled) TextMuted else TextGhost,
            textAlign = TextAlign.Center,
        )
    }
}

/** `3 h 20 m`, or `45 m` under the hour. */
@Composable
private fun formatDuration(millis: Long): String {
    val totalMinutes = (millis / 60_000L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

@Composable
private fun weekLabel(state: StatsUiState.Success): String =
    if (state.isCurrentWeek) {
        stringResource(R.string.stats_this_week)
    } else {
        stringResource(
            R.string.stats_week_range,
            state.week.start.format(DateTimeFormatter.ofPattern(DAY_PATTERN)),
            state.week.end.format(DateTimeFormatter.ofPattern(DAY_PATTERN)),
        )
    }

private fun monthLabel(month: YearMonth): String {
    val locale = Locale.getDefault()
    val name = month.month.getDisplayName(JavaTextStyle.FULL_STANDALONE, locale)
    return "$name ${month.year}"
}

private const val DAY_PATTERN = "d MMM"

private fun previewState() = StatsUiState.Success(
    today = DayStats(LocalDate.parse("2026-04-15"), 8, 3 * 3_600_000L + 20 * 60_000L),
    streak = StreakInfo(current = 12, best = 21, lastActiveDay = LocalDate.parse("2026-04-15")),
    dailyGoal = 8,
    currentDate = LocalDate.parse("2026-04-15"),
    week = PeriodStats(
        start = LocalDate.parse("2026-04-13"),
        end = LocalDate.parse("2026-04-19"),
        days = listOf(2, 6, 8, 5, 7, 1, 3).mapIndexed { index, count ->
            DayStats(
                LocalDate.parse("2026-04-13").plusDays(index.toLong()),
                count,
                count * 1_500_000L,
            )
        },
    ),
    weekAnchor = LocalDate.parse("2026-04-15"),
    monthGrid = StatsAggregation.monthGrid(
        (1..30).map { DayStats(YearMonth.of(2026, 4).atDay(it), (it * 7) % 11) },
        YearMonth.of(2026, 4),
        StatsAggregation.weekFieldsFor(Locale.forLanguageTag("es-ES")),
    ),
    month = PeriodStats(
        start = LocalDate.parse("2026-04-01"),
        end = LocalDate.parse("2026-04-30"),
        days = (1..30).map { DayStats(YearMonth.of(2026, 4).atDay(it), (it * 7) % 11) },
    ),
    displayedMonth = YearMonth.of(2026, 4),
    trend = PeriodStats(
        start = LocalDate.parse("2026-03-17"),
        end = LocalDate.parse("2026-04-15"),
        days = (0 until 30).map {
            DayStats(
                LocalDate.parse("2026-03-17").plusDays(it.toLong()),
                listOf(3, 5, 8, 6, 2, 0, 4)[it % 7],
            )
        },
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 1400)
@Composable
private fun StatsScreenPreview() {
    AquiHayTomateTheme {
        StatsContent(
            state = previewState(),
            onPreviousWeek = {},
            onNextWeek = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onDaySelected = {},
        )
    }
}

/** Every chart has to survive an empty history; it is the classic regression of hand-rolled graphs. */
@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 1400)
@Composable
private fun StatsScreenEmptyPreview() {
    val state = previewState()
    val empty = state.copy(
        today = DayStats(state.currentDate),
        streak = StreakInfo.NONE,
        week = state.week.copy(days = state.week.days.map { DayStats(it.date) }),
        month = state.month.copy(days = state.month.days.map { DayStats(it.date) }),
        monthGrid = StatsAggregation.monthGrid(
            emptyList(),
            YearMonth.of(2026, 4),
            StatsAggregation.weekFieldsFor(Locale.forLanguageTag("es-ES")),
        ),
        trend = state.trend.copy(days = state.trend.days.map { DayStats(it.date) }),
    )

    AquiHayTomateTheme {
        StatsContent(
            state = empty,
            onPreviousWeek = {},
            onNextWeek = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onDaySelected = {},
        )
    }
}
