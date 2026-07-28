package com.jjrapps.aquihaytomate.domain.model

import java.time.LocalDate

/**
 * What one calendar day adds up to. Days with no activity are present too, with zeros: the charts
 * need a slot per day and filling gaps in the UI is how off-by-one bugs get in.
 *
 * @param completedPomodoros rows with `completed = 1`.
 * @param focusedMs sum of `actual_focus_ms` over **all** rows of the day, partial ones included.
 */
data class DayStats(
    val date: LocalDate,
    val completedPomodoros: Int = 0,
    val focusedMs: Long = 0L,
) {
    val hasActivity: Boolean get() = completedPomodoros > 0

    val focusedMinutes: Int get() = (focusedMs / TimerSettings.MINUTE_MS).toInt()
}
