package com.jjrapps.aquihaytomate.data.local.db

import androidx.room.ColumnInfo
import com.jjrapps.aquihaytomate.domain.model.DayStats
import java.time.LocalDate

/**
 * Projection of the daily aggregation. This is as far as SQL goes: weeks, months and streaks are
 * computed in Kotlin, because `strftime('%Y-%W')` decides the first day of the week on its own and
 * knows nothing about `WeekFields.of(locale)`. See CLAUDE.md §5.
 */
data class DayTotals(
    @ColumnInfo(name = "local_date")
    val localDate: LocalDate,

    /** Slots that ran to zero. */
    @ColumnInfo(name = "completed_count")
    val completedCount: Int,

    /** Rows recorded that day, partial ones included. */
    @ColumnInfo(name = "total_count")
    val totalCount: Int,

    /** Sum of `actual_focus_ms` over every row of the day, partial ones included. */
    @ColumnInfo(name = "focused_ms")
    val focusedMs: Long,
)

fun DayTotals.toDomain() = DayStats(
    date = localDate,
    completedPomodoros = completedCount,
    focusedMs = focusedMs,
)
