package com.jjrapps.aquihaytomate.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.model.StreakInfo
import com.jjrapps.aquihaytomate.domain.usecase.GetPeriodStatsUseCase
import com.jjrapps.aquihaytomate.domain.usecase.GetStreakUseCase
import com.jjrapps.aquihaytomate.domain.usecase.GetTodayStatsUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StatsViewModel @Inject constructor(
    getTodayStats: GetTodayStatsUseCase,
    getStreak: GetStreakUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val getPeriodStats: GetPeriodStatsUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val today = LocalDate.now(clock)

    private val weekAnchor = MutableStateFlow(today)
    private val displayedMonth = MutableStateFlow(YearMonth.from(today))
    private val selectedDay = MutableStateFlow<DayStats?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = combine(
        getTodayStats(),
        getStreak(),
        observeSettings(),
        weekAnchor,
        displayedMonth,
    ) { todayStats, streak, settings, anchor, month ->
        Inputs(todayStats, streak, settings.dailyGoal, anchor, month)
    }.flatMapLatest { inputs ->
        combine(
            getPeriodStats.week(inputs.weekAnchor),
            getPeriodStats.monthGrid(inputs.month),
            getPeriodStats.month(inputs.month),
            getPeriodStats.trend(today),
            selectedDay,
        ) { week, grid, month, trend, selected ->
            StatsUiState.Success(
                today = inputs.today,
                streak = inputs.streak,
                dailyGoal = inputs.dailyGoal,
                currentDate = today,
                week = week,
                weekAnchor = inputs.weekAnchor,
                monthGrid = grid,
                month = month,
                displayedMonth = inputs.month,
                trend = trend,
                selectedDay = selected,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = StatsUiState.Loading,
    )

    fun onPreviousWeek() {
        weekAnchor.value = weekAnchor.value.minusWeeks(1)
        selectedDay.value = null
    }

    /** Paging past today is refused rather than hidden, so the arrow can stay put and just dim. */
    fun onNextWeek() {
        val next = weekAnchor.value.plusWeeks(1)
        if (next <= today) {
            weekAnchor.value = next
            selectedDay.value = null
        }
    }

    fun onPreviousMonth() {
        displayedMonth.value = displayedMonth.value.minusMonths(1)
        selectedDay.value = null
    }

    fun onNextMonth() {
        val next = displayedMonth.value.plusMonths(1)
        if (next <= YearMonth.from(today)) {
            displayedMonth.value = next
            selectedDay.value = null
        }
    }

    /** Tapping the selected day again clears the detail line, which is the obvious way to dismiss it. */
    fun onDaySelected(day: DayStats?) {
        selectedDay.value = if (day != null && day.date == selectedDay.value?.date) null else day
    }

    private data class Inputs(
        val today: DayStats,
        val streak: StreakInfo,
        val dailyGoal: Int,
        val weekAnchor: LocalDate,
        val month: YearMonth,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
