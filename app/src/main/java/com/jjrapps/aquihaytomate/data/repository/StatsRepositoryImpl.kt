package com.jjrapps.aquihaytomate.data.repository

import com.jjrapps.aquihaytomate.data.local.db.FocusSessionDao
import com.jjrapps.aquihaytomate.data.local.db.toDomain
import com.jjrapps.aquihaytomate.data.local.db.toEntity
import com.jjrapps.aquihaytomate.domain.model.DayStats
import com.jjrapps.aquihaytomate.domain.model.FocusSession
import com.jjrapps.aquihaytomate.domain.repository.StatsRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val dao: FocusSessionDao,
) : StatsRepository {

    /**
     * `insert` returns `-1L` when `UNIQUE(session_id, slot_index)` rejected the row, which means the
     * other entrypoint got there first. Reporting that back is what lets the caller stay idempotent
     * without any shared flag.
     */
    override suspend fun record(session: FocusSession): Boolean =
        dao.insert(session.toEntity()) != -1L

    override suspend fun isRecorded(sessionId: Long, slotIndex: Int): Boolean =
        dao.find(sessionId, slotIndex) != null

    override fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<DayStats>> =
        dao.dailyTotals(from, to).map { totals -> totals.map { it.toDomain() } }

    override fun allDailyTotals(): Flow<List<DayStats>> =
        dao.allDailyTotals().map { totals -> totals.map { it.toDomain() } }

    override fun completedDays(): Flow<List<LocalDate>> = dao.completedDays()
}
