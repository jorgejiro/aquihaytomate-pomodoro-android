package com.jjrapps.aquihaytomate.domain.model

import java.time.LocalDate

/**
 * A contiguous run of days — a week or a month — already padded so that [days] holds one entry per
 * calendar day from [start] to [end] inclusive, in order.
 */
data class PeriodStats(
    val start: LocalDate,
    val end: LocalDate,
    val days: List<DayStats>,
) {
    val totalPomodoros: Int get() = days.sumOf { it.completedPomodoros }

    val totalFocusedMs: Long get() = days.sumOf { it.focusedMs }

    val activeDays: Int get() = days.count { it.hasActivity }

    /** The busiest day, for scaling the bars. Null when the period is empty of activity. */
    val bestDay: DayStats? get() = days.filter { it.hasActivity }.maxByOrNull { it.completedPomodoros }

    val maxPomodoros: Int get() = days.maxOfOrNull { it.completedPomodoros } ?: 0

    companion object {
        fun empty(start: LocalDate, end: LocalDate) = PeriodStats(start, end, emptyList())
    }
}
