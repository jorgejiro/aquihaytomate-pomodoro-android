package com.jjrapps.aquihaytomate.ui.stats

import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.model.PeriodStats
import com.jjrapps.aquihaytomate.domain.model.StreakInfo
import java.time.LocalDate
import java.time.YearMonth

sealed interface StatsUiState {

    data object Loading : StatsUiState

    data class Success(
        val today: DayStats,
        val streak: StreakInfo,
        val dailyGoal: Int,
        val currentDate: LocalDate,
        val week: PeriodStats,
        val weekAnchor: LocalDate,
        val monthGrid: List<List<DayStats?>>,
        val month: PeriodStats,
        val displayedMonth: YearMonth,
        val trend: PeriodStats,
        /** The day the user tapped on a bar or a cell, shown as a detail line under the chart. */
        val selectedDay: DayStats? = null,
    ) : StatsUiState {

        /** Whether the user can page forward: there is nothing to see in the future. */
        val canGoToNextWeek: Boolean get() = weekAnchor.plusWeeks(1) <= currentDate

        val canGoToNextMonth: Boolean
            get() = displayedMonth.plusMonths(1) <= YearMonth.from(currentDate)

        val isCurrentWeek: Boolean
            get() = weekAnchor.plusWeeks(1) > currentDate && weekAnchor <= currentDate
    }
}
