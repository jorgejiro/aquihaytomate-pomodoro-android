package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.repository.StatsRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class GetTodayStatsUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val clock: Clock,
) {

    /**
     * Today's figures, with zeros before the first pomodoro of the day.
     *
     * The date is resolved inside a flow rather than captured once, so a `StateFlow` that survives past
     * midnight starts reporting the new day instead of freezing on yesterday.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<DayStats> = flow { emit(LocalDate.now(clock)) }
        .flatMapLatest { today ->
            statsRepository.dailyTotals(today, today).map { days ->
                days.firstOrNull() ?: DayStats(today)
            }
        }
}
