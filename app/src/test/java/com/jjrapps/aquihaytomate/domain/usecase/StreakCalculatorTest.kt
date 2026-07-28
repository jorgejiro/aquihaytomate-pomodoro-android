package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.model.StreakInfo
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today = LocalDate.parse("2026-04-15")

    private fun active(vararg dates: String) =
        dates.map { DayStats(LocalDate.parse(it), completedPomodoros = 2, focusedMs = 3_000_000L) }

    @Test
    fun `no history means no streak`() {
        assertEquals(StreakInfo.NONE, StreakCalculator.calculate(emptyList(), today))
    }

    @Test
    fun `a day with focused minutes but no completed pomodoro does not count`() {
        val partialOnly = listOf(
            DayStats(today, completedPomodoros = 0, focusedMs = 900_000L),
        )

        assertEquals(0, StreakCalculator.calculate(partialOnly, today).current)
        assertEquals(0, StreakCalculator.calculate(partialOnly, today).best)
    }

    @Test
    fun `consecutive days ending today count`() {
        val streak = StreakCalculator.calculate(
            active("2026-04-13", "2026-04-14", "2026-04-15"),
            today,
        )

        assertEquals(3, streak.current)
        assertEquals(3, streak.best)
        assertEquals(today, streak.lastActiveDay)
    }

    // A morning with no pomodoros yet must not read as a broken streak.
    @Test
    fun `a streak that ended yesterday is still alive today`() {
        val streak = StreakCalculator.calculate(active("2026-04-13", "2026-04-14"), today)

        assertEquals(2, streak.current)
    }

    @Test
    fun `a gap of two days breaks the current streak`() {
        val streak = StreakCalculator.calculate(active("2026-04-11", "2026-04-12"), today)

        assertEquals(0, streak.current)
        assertEquals(2, streak.best)
    }

    @Test
    fun `the best streak survives being broken`() {
        val streak = StreakCalculator.calculate(
            active(
                "2026-03-01", "2026-03-02", "2026-03-03", "2026-03-04", "2026-03-05",
                "2026-04-14", "2026-04-15",
            ),
            today,
        )

        assertEquals(2, streak.current)
        assertEquals(5, streak.best)
    }

    @Test
    fun `a single day is a streak of one`() {
        val streak = StreakCalculator.calculate(active("2026-04-15"), today)

        assertEquals(1, streak.current)
        assertEquals(1, streak.best)
    }

    @Test
    fun `unordered input is handled`() {
        val streak = StreakCalculator.calculate(
            active("2026-04-15", "2026-04-13", "2026-04-14"),
            today,
        )

        assertEquals(3, streak.current)
    }

    @Test
    fun `days in the future do not extend the current streak`() {
        val streak = StreakCalculator.calculate(
            active("2026-04-15", "2026-04-20"),
            today,
        )

        assertEquals(1, streak.current)
        assertEquals(LocalDate.parse("2026-04-20"), streak.lastActiveDay)
    }

    @Test
    fun `a streak spanning a month boundary is continuous`() {
        val streak = StreakCalculator.calculate(
            active("2026-03-30", "2026-03-31", "2026-04-01"),
            LocalDate.parse("2026-04-01"),
        )

        assertEquals(3, streak.current)
    }

    // The classic off-by-one: February in a non-leap year.
    @Test
    fun `a streak spanning the end of february is continuous`() {
        val streak = StreakCalculator.calculate(
            active("2026-02-27", "2026-02-28", "2026-03-01"),
            LocalDate.parse("2026-03-01"),
        )

        assertEquals(3, streak.current)
    }
}
