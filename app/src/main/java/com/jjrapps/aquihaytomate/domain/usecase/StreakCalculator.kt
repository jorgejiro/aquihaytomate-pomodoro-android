package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.model.StreakInfo
import java.time.LocalDate

/**
 * Current and best day streaks, counted over days with at least one **completed** pomodoro. Partial
 * focus slots add focused minutes but do not keep a streak alive.
 *
 * Pure and `Clock`-free by taking [today] as a parameter, so the awkward cases — a streak that ends
 * today, a day that has not started yet — are testable without an emulator.
 */
object StreakCalculator {

    fun calculate(days: List<DayStats>, today: LocalDate): StreakInfo {
        val active = days.filter { it.hasActivity }.map { it.date }.toSortedSet()
        if (active.isEmpty()) return StreakInfo.NONE

        return StreakInfo(
            current = currentStreak(active, today),
            best = bestStreak(active),
            lastActiveDay = active.last(),
        )
    }

    /**
     * The run that is still alive.
     *
     * A day with no pomodoros yet does not break anything until it is over, so a streak earned
     * through yesterday still reads as live this morning. Counting starts at [today] when there is
     * activity today and at yesterday otherwise; if neither day has activity the streak is zero.
     */
    private fun currentStreak(active: Set<LocalDate>, today: LocalDate): Int {
        var cursor = when {
            today in active -> today
            today.minusDays(1) in active -> today.minusDays(1)
            else -> return 0
        }
        var length = 0
        while (cursor in active) {
            length++
            cursor = cursor.minusDays(1)
        }
        return length
    }

    private fun bestStreak(active: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        active.sorted().forEach { day ->
            run = if (previous != null && previous == day.minusDays(1)) run + 1 else 1
            if (run > best) best = run
            previous = day
        }
        return best
    }
}
