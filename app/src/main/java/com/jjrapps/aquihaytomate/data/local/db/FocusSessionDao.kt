package com.jjrapps.aquihaytomate.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    /**
     * Idempotent insert. `IGNORE` plus `UNIQUE(session_id, slot_index)` is the whole concurrency
     * story: the service and the backup alarm may both close the same slot, and the loser simply
     * gets `-1L`. Callers use the return value to decide whether *they* were the one that closed it,
     * and therefore whether to play the alert.
     *
     * @return the new row id, or `-1L` when the slot was already recorded.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: FocusSessionEntity): Long

    @Query("SELECT * FROM focus_session WHERE session_id = :sessionId AND slot_index = :slotIndex")
    suspend fun find(sessionId: Long, slotIndex: Int): FocusSessionEntity?

    /**
     * Daily rollup over a closed date range. This is the only aggregation done in SQL; see
     * [DayTotals] for why.
     */
    @Query(
        """
        SELECT local_date,
               SUM(CASE WHEN completed THEN 1 ELSE 0 END) AS completed_count,
               COUNT(*) AS total_count,
               SUM(actual_focus_ms) AS focused_ms
        FROM focus_session
        WHERE local_date BETWEEN :from AND :to
        GROUP BY local_date
        ORDER BY local_date
        """,
    )
    fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotals>>

    @Query(
        """
        SELECT local_date,
               SUM(CASE WHEN completed THEN 1 ELSE 0 END) AS completed_count,
               COUNT(*) AS total_count,
               SUM(actual_focus_ms) AS focused_ms
        FROM focus_session
        GROUP BY local_date
        ORDER BY local_date
        """,
    )
    fun allDailyTotals(): Flow<List<DayTotals>>

    /**
     * Every day that has activity, for the streak. Only the dates are read: the streak is a
     * calendar question, and pulling totals for years of history to answer it would be wasteful.
     */
    @Query(
        """
        SELECT DISTINCT local_date
        FROM focus_session
        WHERE completed = 1
        ORDER BY local_date
        """,
    )
    fun completedDays(): Flow<List<LocalDate>>

    @Query("SELECT COUNT(*) FROM focus_session")
    suspend fun count(): Int

    @Query("DELETE FROM focus_session")
    suspend fun deleteAll()
}
