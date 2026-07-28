package com.jjrapps.aquihaytomate.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusSessionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: FocusSessionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.focusSessionDao()
    }

    @After
    fun tearDown() = database.close()

    private fun session(
        sessionId: Long = 1_000L,
        slotIndex: Int = 0,
        date: String = "2026-04-15",
        completed: Boolean = true,
        actualFocusMs: Long = 25 * 60_000L,
    ) = FocusSessionEntity(
        sessionId = sessionId,
        slotIndex = slotIndex,
        slotType = "FOCUS",
        startedAtEpochMs = 1_800_000_000_000L,
        endedAtEpochMs = 1_800_000_000_000L + actualFocusMs,
        plannedDurationMs = 25 * 60_000L,
        actualFocusMs = actualFocusMs,
        completed = completed,
        timezoneId = "Europe/Madrid",
        localDate = LocalDate.parse(date),
    )

    @Test
    fun insertingASlotStoresIt() = runTest {
        val id = dao.insert(session())

        assertTrue(id > 0L)
        assertNotNull(dao.find(1_000L, 0))
        assertEquals(1, dao.count())
    }

    /**
     * The heart of the idempotency story: the service and the backup alarm may both close the same
     * slot, and the unique index makes the second insert a no-op instead of a duplicate pomodoro.
     * See CLAUDE.md §5.
     */
    @Test
    fun insertingTheSameSlotTwiceIsIgnored() = runTest {
        val first = dao.insert(session())
        val second = dao.insert(session(actualFocusMs = 99L))

        assertTrue(first > 0L)
        assertEquals(-1L, second)
        assertEquals(1, dao.count())
        assertEquals(25 * 60_000L, dao.find(1_000L, 0)?.actualFocusMs)
    }

    @Test
    fun differentSlotsOfTheSameSessionCoexist() = runTest {
        dao.insert(session(slotIndex = 0))
        dao.insert(session(slotIndex = 1))

        assertEquals(2, dao.count())
    }

    @Test
    fun theSameSlotIndexInAnotherSessionCoexists() = runTest {
        dao.insert(session(sessionId = 1_000L))
        dao.insert(session(sessionId = 2_000L))

        assertEquals(2, dao.count())
    }

    @Test
    fun localDatesRoundTripThroughTheConverter() = runTest {
        dao.insert(session(date = "2026-02-28"))

        assertEquals(LocalDate.parse("2026-02-28"), dao.find(1_000L, 0)?.localDate)
    }

    @Test
    fun dailyTotalsCountCompletedSlotsAndSumEveryFocusedMillisecond() = runTest {
        dao.insert(session(slotIndex = 0, completed = true, actualFocusMs = 25 * 60_000L))
        dao.insert(session(slotIndex = 1, completed = true, actualFocusMs = 25 * 60_000L))
        // A partial slot adds focused time but is not a completed pomodoro.
        dao.insert(session(slotIndex = 2, completed = false, actualFocusMs = 7 * 60_000L))

        val totals = dao.dailyTotals(
            LocalDate.parse("2026-04-01"),
            LocalDate.parse("2026-04-30"),
        ).first()

        assertEquals(1, totals.size)
        assertEquals(2, totals.first().completedCount)
        assertEquals(3, totals.first().totalCount)
        assertEquals(57 * 60_000L, totals.first().focusedMs)
    }

    @Test
    fun dailyTotalsGroupByDayAndSortAscending() = runTest {
        dao.insert(session(sessionId = 1L, date = "2026-04-16"))
        dao.insert(session(sessionId = 2L, date = "2026-04-14"))
        dao.insert(session(sessionId = 3L, date = "2026-04-15"))

        val dates = dao.allDailyTotals().first().map { it.localDate }

        assertEquals(
            listOf("2026-04-14", "2026-04-15", "2026-04-16").map(LocalDate::parse),
            dates,
        )
    }

    @Test
    fun dailyTotalsExcludeDaysOutsideTheRange() = runTest {
        dao.insert(session(sessionId = 1L, date = "2026-03-31"))
        dao.insert(session(sessionId = 2L, date = "2026-04-15"))
        dao.insert(session(sessionId = 3L, date = "2026-05-01"))

        val totals = dao.dailyTotals(
            LocalDate.parse("2026-04-01"),
            LocalDate.parse("2026-04-30"),
        ).first()

        assertEquals(1, totals.size)
        assertEquals(LocalDate.parse("2026-04-15"), totals.first().localDate)
    }

    @Test
    fun dailyTotalsOnAnEmptyTableIsAnEmptyList() = runTest {
        val totals = dao.dailyTotals(
            LocalDate.parse("2026-04-01"),
            LocalDate.parse("2026-04-30"),
        ).first()

        assertTrue(totals.isEmpty())
    }

    @Test
    fun completedDaysOnlyListsDaysWithAFullPomodoro() = runTest {
        dao.insert(session(sessionId = 1L, date = "2026-04-14", completed = true))
        dao.insert(session(sessionId = 2L, date = "2026-04-15", completed = false))
        dao.insert(session(sessionId = 3L, date = "2026-04-16", completed = true))
        // A second completed slot on a day already listed must not duplicate the day.
        dao.insert(session(sessionId = 4L, date = "2026-04-16", completed = true))

        val days = dao.completedDays().first()

        assertEquals(
            listOf("2026-04-14", "2026-04-16").map(LocalDate::parse),
            days,
        )
    }

    @Test
    fun deleteAllEmptiesTheTable() = runTest {
        dao.insert(session())
        dao.deleteAll()

        assertEquals(0, dao.count())
        assertFalse(dao.completedDays().first().isNotEmpty())
    }
}
