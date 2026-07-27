package com.jjrapps.aquihaytomate.domain.repository

import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.model.FocusSession
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface StatsRepository {

    /**
     * Records a focus slot.
     *
     * @return true when this call is the one that inserted the row, false when the slot had already
     *   been recorded by the other entrypoint. Only the winner should alert the user.
     */
    suspend fun record(session: FocusSession): Boolean

    suspend fun isRecorded(sessionId: Long, slotIndex: Int): Boolean

    /** Daily totals over a closed range, zeros for missing days filled in by the caller. */
    fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<DayStats>>

    fun allDailyTotals(): Flow<List<DayStats>>

    /** Days with at least one completed pomodoro, for the streak. */
    fun completedDays(): Flow<List<LocalDate>>
}
