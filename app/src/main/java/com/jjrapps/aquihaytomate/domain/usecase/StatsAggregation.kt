package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.model.PeriodStats
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale

/** An inclusive run of calendar days. */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    val dayCount: Int get() = (end.toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(0)

    fun days(): List<LocalDate> =
        (0 until dayCount).map { start.plusDays(it.toLong()) }

    operator fun contains(date: LocalDate): Boolean = date >= start && date <= end
}

/**
 * Rolls daily totals up into weeks and months.
 *
 * **This is deliberately Kotlin and not SQL.** SQLite's `strftime('%Y-%W')` picks its own first day
 * of the week and knows nothing about `WeekFields.of(locale)`, so a Spanish user's week would start
 * on Sunday. The DAO only groups by `local_date`; everything above that lives here, where a fixed
 * `Locale` makes it testable without an emulator and correct across daylight saving changes.
 */
object StatsAggregation {

    /** Levels used by the monthly heatmap: 0 is empty, 4 is "at or above the daily goal". */
    const val HEAT_LEVELS = 5

    /** How many days the sparkline and the streak strip cover. */
    const val TREND_DAYS = 30

    fun weekFieldsFor(locale: Locale): WeekFields = WeekFields.of(locale)

    /** The week [date] belongs to, per the locale's own idea of where a week starts. */
    fun weekRange(date: LocalDate, weekFields: WeekFields): DateRange {
        val start = date.with(weekFields.dayOfWeek(), 1L)
        return DateRange(start, start.plusDays(6))
    }

    fun monthRange(month: YearMonth): DateRange =
        DateRange(month.atDay(1), month.atEndOfMonth())

    /** The [TREND_DAYS]-day window ending on [today], inclusive. */
    fun trendRange(today: LocalDate, days: Int = TREND_DAYS): DateRange =
        DateRange(today.minusDays((days - 1).toLong()), today)

    /**
     * One [DayStats] per day in [range], in order, with zeros where there was no activity.
     *
     * Padding here rather than in the UI is what stops the charts from silently shifting a bar when
     * a day is missing — the classic off-by-one of hand-rolled graphs.
     */
    fun fillGaps(totals: List<DayStats>, range: DateRange): List<DayStats> {
        val byDate = totals.associateBy { it.date }
        return range.days().map { day -> byDate[day] ?: DayStats(day) }
    }

    fun period(totals: List<DayStats>, range: DateRange): PeriodStats =
        PeriodStats(range.start, range.end, fillGaps(totals, range))

    fun week(totals: List<DayStats>, date: LocalDate, weekFields: WeekFields): PeriodStats =
        period(totals, weekRange(date, weekFields))

    fun month(totals: List<DayStats>, month: YearMonth): PeriodStats =
        period(totals, monthRange(month))

    fun trend(totals: List<DayStats>, today: LocalDate, days: Int = TREND_DAYS): PeriodStats =
        period(totals, trendRange(today, days))

    /**
     * The monthly heatmap laid out as rows of seven, aligned to the locale's first day of the week.
     * Cells outside the month are null so the grid keeps its shape without inventing days.
     */
    fun monthGrid(
        totals: List<DayStats>,
        month: YearMonth,
        weekFields: WeekFields,
    ): List<List<DayStats?>> {
        val days = fillGaps(totals, monthRange(month))
        val firstDayOfWeek = weekFields.firstDayOfWeek
        // Monday-based 1..7 shifted so that the locale's first day lands on column 0.
        val leadingBlanks =
            (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val cells = List<DayStats?>(leadingBlanks) { null } + days
        val trailingBlanks = (7 - cells.size % 7) % 7
        return (cells + List<DayStats?>(trailingBlanks) { null }).chunked(7)
    }

    /**
     * Heat level `0..HEAT_LEVELS - 1` for a day, scaled against the user's daily goal so the map
     * means the same thing whether they aim for 4 pomodoros or 16.
     */
    fun heatLevel(completedPomodoros: Int, dailyGoal: Int): Int {
        if (completedPomodoros <= 0) return 0
        val goal = dailyGoal.coerceAtLeast(1)
        val steps = HEAT_LEVELS - 1
        return 1 + ((completedPomodoros - 1) * steps / goal).coerceAtMost(steps - 1)
    }
}
