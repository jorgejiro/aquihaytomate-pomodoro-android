package com.jjrapps.aquihaytomate.domain.model

import java.time.LocalDate

/**
 * Day streaks, counted over days with at least one completed pomodoro.
 *
 * @param current length of the run that is still alive today. A day with no activity yet does not
 *   break the streak until it is over, so a streak earned yesterday still counts this morning.
 * @param best longest run ever recorded.
 * @param lastActiveDay most recent day with activity, or null when there has never been one.
 */
data class StreakInfo(
    val current: Int = 0,
    val best: Int = 0,
    val lastActiveDay: LocalDate? = null,
) {
    companion object {
        val NONE = StreakInfo()
    }
}
