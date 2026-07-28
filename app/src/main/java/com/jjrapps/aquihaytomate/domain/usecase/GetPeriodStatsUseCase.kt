package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.PeriodStats
import com.jjrapps.aquihaytomate.domain.repository.StatsRepository
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Weeks, months and the 30-day trend.
 *
 * The DAO only groups by day; the rollup happens here in Kotlin with an injected [Locale], because
 * SQLite decides the first day of the week on its own and knows nothing about `WeekFields`. See
 * CLAUDE.md §5.
 */
class GetPeriodStatsUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val locale: Locale,
) {

    fun week(anchor: LocalDate): Flow<PeriodStats> {
        val range = StatsAggregation.weekRange(anchor, StatsAggregation.weekFieldsFor(locale))
        return statsRepository.dailyTotals(range.start, range.end)
            .map { StatsAggregation.period(it, range) }
    }

    fun month(month: YearMonth): Flow<PeriodStats> {
        val range = StatsAggregation.monthRange(month)
        return statsRepository.dailyTotals(range.start, range.end)
            .map { StatsAggregation.period(it, range) }
    }

    fun trend(today: LocalDate, days: Int = StatsAggregation.TREND_DAYS): Flow<PeriodStats> {
        val range = StatsAggregation.trendRange(today, days)
        return statsRepository.dailyTotals(range.start, range.end)
            .map { StatsAggregation.period(it, range) }
    }

    /** The month grid for the heatmap: rows of seven, aligned to the locale's first day of the week. */
    fun monthGrid(month: YearMonth) = statsRepository
        .dailyTotals(StatsAggregation.monthRange(month).start, StatsAggregation.monthRange(month).end)
        .map { StatsAggregation.monthGrid(it, month, StatsAggregation.weekFieldsFor(locale)) }
}
