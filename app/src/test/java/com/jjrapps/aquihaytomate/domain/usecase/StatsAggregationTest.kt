package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.DayStats
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsAggregationTest {

    private val spanish = WeekFields.of(Locale.forLanguageTag("es-ES"))
    private val usEnglish = WeekFields.of(Locale.US)

    private fun day(date: String, pomodoros: Int, minutes: Long = pomodoros * 25L) =
        DayStats(LocalDate.parse(date), pomodoros, minutes * 60_000L)

    @Test
    fun `a spanish week starts on monday`() {
        val range = StatsAggregation.weekRange(LocalDate.parse("2026-04-15"), spanish)

        assertEquals(LocalDate.parse("2026-04-13"), range.start)
        assertEquals(LocalDate.parse("2026-04-19"), range.end)
        assertEquals(DayOfWeek.MONDAY, range.start.dayOfWeek)
    }

    // This is exactly what SQLite's strftime('%Y-%W') cannot express, and why the rollup is Kotlin.
    @Test
    fun `a US week starts on sunday`() {
        val range = StatsAggregation.weekRange(LocalDate.parse("2026-04-15"), usEnglish)

        assertEquals(LocalDate.parse("2026-04-12"), range.start)
        assertEquals(DayOfWeek.SUNDAY, range.start.dayOfWeek)
    }

    @Test
    fun `a week always has seven days`() {
        val week = StatsAggregation.week(emptyList(), LocalDate.parse("2026-04-15"), spanish)

        assertEquals(7, week.days.size)
    }

    @Test
    fun `gaps are filled with zeros in date order`() {
        val range = DateRange(LocalDate.parse("2026-04-13"), LocalDate.parse("2026-04-19"))
        val filled = StatsAggregation.fillGaps(
            listOf(day("2026-04-15", 3), day("2026-04-18", 1)),
            range,
        )

        assertEquals(7, filled.size)
        assertEquals(range.days(), filled.map { it.date })
        assertEquals(3, filled[2].completedPomodoros)
        assertEquals(0, filled[0].completedPomodoros)
        assertEquals(1, filled[5].completedPomodoros)
    }

    @Test
    fun `totals ignore days outside the range`() {
        val week = StatsAggregation.week(
            listOf(day("2026-04-15", 3), day("2026-04-25", 8)),
            LocalDate.parse("2026-04-15"),
            spanish,
        )

        assertEquals(3, week.totalPomodoros)
    }

    @Test
    fun `an empty period reports zeros and no best day`() {
        val week = StatsAggregation.week(emptyList(), LocalDate.parse("2026-04-15"), spanish)

        assertEquals(0, week.totalPomodoros)
        assertEquals(0L, week.totalFocusedMs)
        assertEquals(0, week.activeDays)
        assertEquals(0, week.maxPomodoros)
        assertNull(week.bestDay)
    }

    @Test
    fun `a month covers every one of its days`() {
        val month = StatsAggregation.month(emptyList(), YearMonth.of(2026, 2))

        assertEquals(28, month.days.size)
        assertEquals(LocalDate.parse("2026-02-01"), month.start)
        assertEquals(LocalDate.parse("2026-02-28"), month.end)
    }

    @Test
    fun `the month grid pads to whole weeks`() {
        // 1 April 2026 is a Wednesday, so a Monday-based grid needs two leading blanks.
        val grid = StatsAggregation.monthGrid(emptyList(), YearMonth.of(2026, 4), spanish)

        assertTrue(grid.all { it.size == 7 })
        assertNull(grid.first()[0])
        assertNull(grid.first()[1])
        assertEquals(LocalDate.parse("2026-04-01"), grid.first()[2]?.date)
        assertEquals(30, grid.flatten().count { it != null })
    }

    @Test
    fun `the month grid shifts with the locale`() {
        val grid = StatsAggregation.monthGrid(emptyList(), YearMonth.of(2026, 4), usEnglish)

        // Sunday-based, so Wednesday 1 April lands on column 3.
        assertEquals(LocalDate.parse("2026-04-01"), grid.first()[3]?.date)
    }

    @Test
    fun `a month starting on the first day of the week has no leading blanks`() {
        // 1 June 2026 is a Monday.
        val grid = StatsAggregation.monthGrid(emptyList(), YearMonth.of(2026, 6), spanish)

        assertEquals(LocalDate.parse("2026-06-01"), grid.first()[0]?.date)
    }

    @Test
    fun `the trend window ends today and covers thirty days`() {
        val range = StatsAggregation.trendRange(LocalDate.parse("2026-04-15"))

        assertEquals(30, range.dayCount)
        assertEquals(LocalDate.parse("2026-04-15"), range.end)
        assertEquals(LocalDate.parse("2026-03-17"), range.start)
    }

    @Test
    fun `a day with no pomodoros has no heat`() {
        assertEquals(0, StatsAggregation.heatLevel(0, dailyGoal = 8))
    }

    @Test
    fun `heat rises with the count and tops out at the goal`() {
        val goal = 8
        val levels = (1..12).map { StatsAggregation.heatLevel(it, goal) }

        assertEquals(1, levels.first())
        assertEquals(StatsAggregation.HEAT_LEVELS - 1, StatsAggregation.heatLevel(goal, goal))
        assertTrue("levels must never decrease", levels.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(
            "levels must stay in range",
            levels.all { it in 1 until StatsAggregation.HEAT_LEVELS },
        )
    }

    @Test
    fun `heat scales with the goal`() {
        assertEquals(StatsAggregation.HEAT_LEVELS - 1, StatsAggregation.heatLevel(4, dailyGoal = 4))
        assertEquals(1, StatsAggregation.heatLevel(4, dailyGoal = 24))
    }

    @Test
    fun `a zero goal does not divide by zero`() {
        assertEquals(StatsAggregation.HEAT_LEVELS - 1, StatsAggregation.heatLevel(3, dailyGoal = 0))
    }

    @Test
    fun `a date range knows what it contains`() {
        val range = DateRange(LocalDate.parse("2026-04-13"), LocalDate.parse("2026-04-19"))

        assertTrue(LocalDate.parse("2026-04-13") in range)
        assertTrue(LocalDate.parse("2026-04-19") in range)
        assertTrue(LocalDate.parse("2026-04-20") !in range)
    }
}
