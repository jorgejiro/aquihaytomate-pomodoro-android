package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.model.StreakInfo
import com.jjrapps.aquihaytomate.domain.repository.StatsRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetStreakUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val clock: Clock,
) {

    /**
     * Only the dates of days with a completed pomodoro are read: the streak is a calendar question, and
     * pulling totals for years of history to answer it would be waste.
     */
    operator fun invoke(): Flow<StreakInfo> = statsRepository.completedDays().map { days ->
        StreakCalculator.calculate(
            days = days.map { DayStats(it, completedPomodoros = 1) },
            today = LocalDate.now(clock),
        )
    }
}
