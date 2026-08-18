package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.testing.FakeAlertPlayer
import com.jjrapps.aquihaytomate.testing.FakeSettingsRepository
import com.jjrapps.aquihaytomate.testing.FakeStatsRepository
import com.jjrapps.aquihaytomate.testing.FakeTimerRuntime
import com.jjrapps.aquihaytomate.testing.FakeTimerStateRepository
import com.jjrapps.aquihaytomate.testing.MutableClock
import com.jjrapps.aquihaytomate.testing.MutableElapsedRealtime
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The engine driven end to end over in-memory repositories: a full pomodoro, the awkward transitions,
 * and the idempotency guarantees that the service, the alarm and `reconcile()` all depend on.
 */
class TimerEngineTest {

    private val startEpochMs = 1_800_000_000_000L
    private val startElapsedMs = 90_000_000L
    private val focusMs = 25 * 60_000L
    private val shortBreakMs = 5 * 60_000L
    private val longBreakMs = 15 * 60_000L

    private val clock = MutableClock(Instant.ofEpochMilli(startEpochMs), ZoneId.of("Europe/Madrid"))
    private val elapsed = MutableElapsedRealtime(startElapsedMs)
    private val timerState = FakeTimerStateRepository()
    private val settings = FakeSettingsRepository()
    private val stats = FakeStatsRepository()
    private val alerts = FakeAlertPlayer()

    private val runtime = FakeTimerRuntime()

    private val recordFocusSlot = RecordFocusSlotUseCase(stats, clock)
    private val sync =
        SyncTimerRuntimeUseCase(timerState, runtime, runtime, runtime, clock, elapsed)
    private val start = StartTimerUseCase(timerState, settings, sync, clock, elapsed)
    private val pause = PauseTimerUseCase(timerState, sync, clock, elapsed)
    private val resume = ResumeTimerUseCase(timerState, sync, clock, elapsed)
    private val toggle = ToggleTimerUseCase(timerState, start, pause)
    private val reset =
        ResetTimerUseCase(timerState, settings, recordFocusSlot, sync, clock, elapsed)
    private val skip = SkipSlotUseCase(timerState, settings, recordFocusSlot, sync, clock, elapsed)
    private val complete =
        CompleteSlotUseCase(timerState, settings, recordFocusSlot, alerts, runtime, sync, clock, elapsed)
    private val freshDay =
        StartFreshDayUseCase(timerState, settings, recordFocusSlot, sync, runtime, clock)
    private val reconcile =
        ReconcileTimerUseCase(timerState, complete, freshDay, sync, clock, elapsed)
    private val restoreNotification =
        RestoreOngoingNotificationUseCase(timerState, runtime, clock, elapsed)

    private fun advance(millis: Long) {
        clock.advanceBy(millis)
        elapsed.advanceBy(millis)
    }

    private suspend fun state() = timerState.current()

    private suspend fun remaining() =
        TimerMath.remainingMs(state(), clock.millis(), elapsed.millis(), settings.current())

    // ─── Los dos sonidos ────────────────────────────────────────────────────

    @Test
    fun `a pomodoro and a break do not sound the same`() = runTest {
        settings.set(
            TimerSettings(
                focusAlertSound = AlertSound.BOWL,
                breakAlertSound = AlertSound.DIGITAL,
                autoStartBreak = false,
            ),
        )

        start()
        advance(focusMs)
        assertTrue(complete())
        assertEquals("El pomodoro acaba con el sonido suave", AlertSound.BOWL, alerts.lastSound)

        start()                       // el descanso, que estaba en RINGING esperando
        advance(shortBreakMs)
        assertTrue(complete())
        assertEquals("El descanso acaba con el duro", AlertSound.DIGITAL, alerts.lastSound)

        assertEquals(listOf(AlertSound.BOWL, AlertSound.DIGITAL), alerts.playedSounds)
    }

    @Test
    fun `each end of the slot carries its own repeat count`() = runTest {
        settings.set(
            TimerSettings(
                focusAlertRepeats = 2,
                breakAlertRepeats = 5,
                autoStartBreak = false,
            ),
        )

        start()
        advance(focusMs)
        assertTrue(complete())
        assertEquals("El pomodoro suena las veces del pomodoro", 2, alerts.lastRepeats)

        start()                       // el descanso, que estaba en RINGING esperando
        advance(shortBreakMs)
        assertTrue(complete())
        assertEquals("El descanso, las del descanso", 5, alerts.lastRepeats)

        assertEquals(listOf(2, 5), alerts.playedRepeats)
    }

    @Test
    fun `repeats default to a single play`() = runTest {
        settings.set(TimerSettings(autoStartBreak = false))

        start()
        advance(focusMs)
        assertTrue(complete())

        assertEquals("Sin tocar el ajuste, la alerta suena una vez", 1, alerts.lastRepeats)
    }

    @Test
    fun `chaining into the break still uses the sound of the slot that ended`() = runTest {
        // Con el auto-inicio puesto, el descanso arranca solo; lo que acaba de terminar es el pomodoro, así
        // que tiene que sonar el suyo y no el del descanso que empieza.
        settings.set(
            TimerSettings(
                focusAlertSound = AlertSound.SOFT,
                breakAlertSound = AlertSound.DIGITAL,
                autoStartBreak = true,
            ),
        )

        start()
        advance(focusMs)
        assertTrue(complete())

        assertEquals(TimerStatus.RUNNING, state().status)
        assertEquals(SlotType.SHORT_BREAK, state().slotType)
        assertEquals(AlertSound.SOFT, alerts.lastSound)
    }

    @Test
    fun `silencing one end leaves the other alone`() = runTest {
        settings.set(
            TimerSettings(
                focusAlertSound = AlertSound.SILENT,
                breakAlertSound = AlertSound.BELL,
                autoStartBreak = false,
            ),
        )

        start()
        advance(focusMs)
        complete()

        // Sigue habiendo alerta: la decisión de no hacer ruido es del reproductor, según la política, no
        // del motor. Lo que cambia es qué sonido se le pide.
        assertEquals(AlertSound.SILENT, alerts.lastSound)
    }

    // ─── Starting, pausing, resuming ────────────────────────────────────────

    @Test
    fun `starting from idle arms both deadlines`() = runTest {
        assertTrue(start())

        val state = state()
        assertEquals(TimerStatus.RUNNING, state.status)
        assertEquals(SlotType.FOCUS, state.slotType)
        assertEquals(focusMs, state.slotDurationMs)
        assertEquals(startEpochMs + focusMs, state.endAtEpochMs)
        assertEquals(startElapsedMs + focusMs, state.endAtElapsedRealtimeMs)
        assertEquals(startEpochMs, state.sessionId)
    }

    @Test
    fun `starting an already running timer changes nothing`() = runTest {
        start()
        val before = state()

        assertFalse(start())
        assertEquals(before, state())
    }

    @Test
    fun `pausing freezes the remaining time and resuming picks it up`() = runTest {
        start()
        advance(10 * 60_000L)

        assertTrue(pause())
        assertEquals(TimerStatus.PAUSED, state().status)
        assertEquals(15 * 60_000L, state().remainingAtPauseMs)

        // Ten minutes of coffee must not eat into the pomodoro.
        advance(10 * 60_000L)
        assertEquals(15 * 60_000L, remaining())

        assertTrue(resume())
        assertEquals(TimerStatus.RUNNING, state().status)
        assertEquals(15 * 60_000L, remaining())
    }

    @Test
    fun `the active time excludes pauses`() = runTest {
        start()
        advance(4 * 60_000L)
        pause()
        advance(60 * 60_000L)
        resume()
        advance(6 * 60_000L)

        assertEquals(10 * 60_000L, TimerMath.activeElapsedMs(state().slotDurationMs, remaining()))
    }

    @Test
    fun `pausing a timer that is not running does nothing`() = runTest {
        assertFalse(pause())
        assertEquals(TimerStatus.IDLE, state().status)
    }

    @Test
    fun `resuming a timer that is not paused does nothing`() = runTest {
        start()
        assertFalse(resume())
    }

    @Test
    fun `toggle walks start pause resume`() = runTest {
        toggle()
        assertEquals(TimerStatus.RUNNING, state().status)

        toggle()
        assertEquals(TimerStatus.PAUSED, state().status)

        toggle()
        assertEquals(TimerStatus.RUNNING, state().status)
    }

    @Test
    fun `the slot start time survives a pause`() = runTest {
        start()
        val startedAt = state().slotStartedAtEpochMs
        advance(60_000L)
        pause()
        advance(60_000L)
        resume()

        assertEquals(startedAt, state().slotStartedAtEpochMs)
    }

    // ─── Completing a slot ──────────────────────────────────────────────────

    @Test
    fun `a finished focus slot is recorded, rings and plans the short break`() = runTest {
        // Auto-starting the break is the default now, and this test is about the slot *waiting* instead.
        settings.set(TimerSettings(autoStartBreak = false))
        start()
        advance(focusMs)

        assertTrue(complete())

        assertEquals(1, stats.recorded.size)
        stats.recorded.single().let { session ->
            assertTrue(session.completed)
            assertEquals(focusMs, session.actualFocusMs)
            assertEquals(SlotType.FOCUS, session.slotType)
        }
        assertEquals(1, alerts.playCount)

        // RINGING already describes the next slot, ready to start.
        val state = state()
        assertEquals(TimerStatus.RINGING, state.status)
        assertEquals(SlotType.SHORT_BREAK, state.slotType)
        assertEquals(shortBreakMs, state.slotDurationMs)
        assertEquals(1, state.completedFocusInCycle)
        assertEquals(1, state.slotIndex)
    }

    @Test
    fun `completing before the deadline is refused`() = runTest {
        start()
        advance(focusMs - 1_000L)

        assertFalse(complete())
        assertEquals(TimerStatus.RUNNING, state().status)
        assertTrue(stats.recorded.isEmpty())
        assertEquals(0, alerts.playCount)
    }

    @Test
    fun `completing when nothing is running is refused`() = runTest {
        assertFalse(complete())
    }

    /**
     * The race the whole design turns on: the service and the backup alarm both fire for the same
     * slot. One row, one alert, one transition.
     */
    @Test
    fun `completing the same slot twice records and alerts once`() = runTest {
        start()
        advance(focusMs)

        assertTrue(complete())
        assertFalse(complete())

        assertEquals(1, stats.recorded.size)
        assertEquals(1, alerts.playCount)
    }

    @Test
    fun `a finished break is not recorded but still moves on`() = runTest {
        start()
        advance(focusMs)
        complete()
        // Now on the short break.
        start()
        advance(shortBreakMs)

        assertTrue(complete())

        assertEquals(1, stats.recorded.size)
        assertEquals(SlotType.FOCUS, state().slotType)
        assertEquals(TimerStatus.RINGING, state().status)
    }

    @Test
    fun `auto start chains straight into the next slot without ringing idle`() = runTest {
        settings.set(TimerSettings(autoStartBreak = true))
        start()
        advance(focusMs)

        assertTrue(complete())

        val state = state()
        assertEquals(TimerStatus.RUNNING, state.status)
        assertEquals(SlotType.SHORT_BREAK, state.slotType)
        assertEquals(shortBreakMs, remaining())
        assertEquals(1, alerts.playCount)
    }

    // The reason the single switch was split: rolling into the break is welcome, rolling back into work
    // is not, because a break often runs long on purpose.
    @Test
    fun `auto starting the break leaves the next pomodoro waiting`() = runTest {
        settings.set(TimerSettings(autoStartBreak = true, autoStartFocus = false))
        start()
        advance(focusMs)
        complete()

        // The break is already running, and it still rang and buzzed on the way in.
        assertEquals(TimerStatus.RUNNING, state().status)
        assertEquals(SlotType.SHORT_BREAK, state().slotType)
        assertEquals(1, alerts.playCount)

        advance(shortBreakMs)
        complete()

        // The pomodoro is not: it waits for the user, however long the break really took.
        assertEquals(TimerStatus.RINGING, state().status)
        assertEquals(SlotType.FOCUS, state().slotType)
        assertEquals(2, alerts.playCount)
    }

    @Test
    fun `auto starting the pomodoro leaves the break waiting`() = runTest {
        settings.set(TimerSettings(autoStartBreak = false, autoStartFocus = true))
        start()
        advance(focusMs)
        complete()

        assertEquals(TimerStatus.RINGING, state().status)
        assertEquals(SlotType.SHORT_BREAK, state().slotType)

        // Take the break by hand, and the pomodoro after it chains on its own.
        start()
        advance(shortBreakMs)
        complete()

        assertEquals(TimerStatus.RUNNING, state().status)
        assertEquals(SlotType.FOCUS, state().slotType)
    }

    // Skipping goes through the same switch as finishing, so the two cannot diverge.
    @Test
    fun `skipping a break honours the pomodoro switch and not the break one`() = runTest {
        settings.set(TimerSettings(autoStartBreak = true, autoStartFocus = false))
        start()
        advance(focusMs)
        complete()

        skip()

        assertEquals(SlotType.FOCUS, state().slotType)
        assertEquals(TimerStatus.IDLE, state().status)
    }

    @Test
    fun `the fourth completed focus leads to the long break`() = runTest {
        repeat(3) {
            start()
            advance(focusMs)
            complete()
            start()
            advance(shortBreakMs)
            complete()
        }

        start()
        advance(focusMs)
        complete()

        val state = state()
        assertEquals(SlotType.LONG_BREAK, state.slotType)
        assertEquals(longBreakMs, state.slotDurationMs)
        assertEquals(4, state.completedFocusInCycle)
        assertEquals(4, stats.recorded.count { it.completed })
    }

    @Test
    fun `the cycle counter resets after the long break`() = runTest {
        repeat(4) {
            start()
            advance(focusMs)
            complete()
            start()
            advance(if (it == 3) longBreakMs else shortBreakMs)
            complete()
        }

        assertEquals(SlotType.FOCUS, state().slotType)
        assertEquals(0, state().completedFocusInCycle)
    }

    @Test
    fun `every slot of a batch shares the session id and gets its own index`() = runTest {
        start()
        advance(focusMs)
        complete()
        start()
        advance(shortBreakMs)
        complete()
        start()
        advance(focusMs)
        complete()

        assertEquals(listOf(startEpochMs, startEpochMs), stats.recorded.map { it.sessionId })
        assertEquals(listOf(0, 2), stats.recorded.map { it.slotIndex })
    }

    // ─── Skipping ───────────────────────────────────────────────────────────

    @Test
    fun `skipping a focus slot does not advance the cycle and leads to a short break`() = runTest {
        settings.set(TimerSettings(autoStartBreak = false))
        start()
        advance(10 * 60_000L)

        assertTrue(skip())

        val state = state()
        assertEquals(SlotType.SHORT_BREAK, state.slotType)
        assertEquals(0, state.completedFocusInCycle)
        assertEquals(TimerStatus.IDLE, state.status)
    }

    @Test
    fun `skipping records the focus time already spent as partial`() = runTest {
        start()
        advance(10 * 60_000L)
        skip()

        stats.recorded.single().let { session ->
            assertFalse(session.completed)
            assertEquals(10 * 60_000L, session.actualFocusMs)
        }
    }

    // An accidental start/stop must not show up in the statistics at all.
    @Test
    fun `skipping under a minute records nothing`() = runTest {
        start()
        advance(59_000L)
        skip()

        assertTrue(stats.recorded.isEmpty())
    }

    @Test
    fun `skipping the last focus of the cycle still leads to a short break`() = runTest {
        repeat(3) {
            start()
            advance(focusMs)
            complete()
            start()
            advance(shortBreakMs)
            complete()
        }
        start()
        advance(5 * 60_000L)

        skip()

        assertEquals(SlotType.SHORT_BREAK, state().slotType)
        assertEquals(3, state().completedFocusInCycle)
    }

    @Test
    fun `skipping a break needs no record`() = runTest {
        start()
        advance(focusMs)
        complete()
        start()
        advance(60_000L)

        skip()

        assertEquals(1, stats.recorded.size)
        assertEquals(SlotType.FOCUS, state().slotType)
    }

    @Test
    fun `skipping honours auto start`() = runTest {
        settings.set(TimerSettings(autoStartBreak = true))
        start()
        advance(60_000L)

        skip()

        assertEquals(TimerStatus.RUNNING, state().status)
        assertEquals(SlotType.SHORT_BREAK, state().slotType)
    }

    // ─── The ongoing notification coming back ───────────────────────────────

    // From Android 13 the user can swipe away the notification of a foreground service and the service
    // survives it: the countdown would keep running with nothing on screen and no controls to reach.
    @Test
    fun `a dismissed notification comes back while the slot runs`() = runTest {
        start()
        advance(60_000L)

        restoreNotification()

        assertEquals(1, runtime.runningNotificationCount)
    }

    @Test
    fun `a dismissed notification comes back while the slot is paused`() = runTest {
        start()
        advance(60_000L)
        pause()
        val afterPausing = runtime.pausedNotificationCount

        restoreNotification()

        assertEquals(afterPausing + 1, runtime.pausedNotificationCount)
    }

    // Dismissing the end-of-slot alert is the user acknowledging it, and an idle timer has nothing to
    // report. Bringing either back would be the notification that will not die.
    @Test
    fun `nothing comes back when ringing or idle`() = runTest {
        settings.set(TimerSettings(autoStartBreak = false))
        start()
        advance(focusMs)
        complete()
        val ringingCounts = runtime.runningNotificationCount to runtime.pausedNotificationCount

        restoreNotification()
        assertEquals(ringingCounts, runtime.runningNotificationCount to runtime.pausedNotificationCount)

        reset()
        restoreNotification()
        assertEquals(ringingCounts, runtime.runningNotificationCount to runtime.pausedNotificationCount)
    }

    // ─── Resetting ──────────────────────────────────────────────────────────

    @Test
    fun `resetting sends the slot back to the start without touching the cycle`() = runTest {
        start()
        advance(focusMs)
        complete()
        start()
        advance(shortBreakMs)
        complete()
        start()
        advance(3 * 60_000L)

        assertTrue(reset())

        val state = state()
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(SlotType.FOCUS, state.slotType)
        assertEquals(1, state.completedFocusInCycle)
        assertEquals(focusMs, TimerMath.remainingMs(state, clock.millis(), elapsed.millis()))
    }

    @Test
    fun `resetting an idle timer does nothing`() = runTest {
        assertFalse(reset())
    }

    @Test
    fun `resetting records the partial focus slot`() = runTest {
        start()
        advance(9 * 60_000L)
        reset()

        stats.recorded.single().let { session ->
            assertFalse(session.completed)
            assertEquals(9 * 60_000L, session.actualFocusMs)
        }
    }

    /**
     * Without burning the slot index, `UNIQUE(session_id, slot_index)` would swallow the row for the
     * retried slot and a pomodoro the user genuinely completed would go unrecorded.
     */
    @Test
    fun `a slot retried after a recorded reset is still recorded when completed`() = runTest {
        start()
        advance(9 * 60_000L)
        reset()

        start()
        advance(focusMs)
        complete()

        assertEquals(2, stats.recorded.size)
        assertEquals(1, stats.recorded.count { it.completed })
        assertEquals(listOf(0, 1), stats.recorded.map { it.slotIndex })
    }

    @Test
    fun `resetting under a minute records nothing and reuses the index`() = runTest {
        start()
        advance(30_000L)
        reset()

        assertTrue(stats.recorded.isEmpty())
        assertEquals(0, state().slotIndex)
    }

    @Test
    fun `resetting from ringing does not invent a pomodoro`() = runTest {
        start()
        advance(focusMs)
        complete()
        val recordedAfterComplete = stats.recorded.size

        assertTrue(reset())

        assertEquals(recordedAfterComplete, stats.recorded.size)
        assertEquals(TimerStatus.IDLE, state().status)
        assertEquals(SlotType.SHORT_BREAK, state().slotType)
    }

    @Test
    fun `resetting a paused slot records what was focused before the pause`() = runTest {
        start()
        advance(8 * 60_000L)
        pause()
        advance(30 * 60_000L)

        reset()

        assertEquals(8 * 60_000L, stats.recorded.single().actualFocusMs)
    }

    // ─── Reconciling ────────────────────────────────────────────────────────

    @Test
    fun `reconciling a timer that is still running changes nothing`() = runTest {
        start()
        advance(60_000L)

        assertFalse(reconcile())
        assertEquals(TimerStatus.RUNNING, state().status)
    }

    @Test
    fun `reconciling an idle timer changes nothing`() = runTest {
        assertFalse(reconcile())
    }

    @Test
    fun `reconciling a slot that just expired records it and rings`() = runTest {
        start()
        advance(focusMs + 5_000L)

        assertTrue(reconcile())

        assertEquals(1, stats.recorded.size)
        assertEquals(TimerStatus.RINGING, state().status)
        assertEquals(1, alerts.playCount)
    }

    // Eight hours with the phone off must yield one pomodoro, not sixteen.
    @Test
    fun `reconciling after hours records one slot silently and stops`() = runTest {
        settings.set(TimerSettings(autoStartBreak = true))
        start()
        advance(8 * 60 * 60_000L)

        assertTrue(reconcile())

        assertEquals(1, stats.recorded.size)
        assertEquals(0, alerts.playCount)
        assertEquals(TimerStatus.IDLE, state().status)
        assertEquals(SlotType.SHORT_BREAK, state().slotType)
    }

    @Test
    fun `reconciling never chains even with auto start on`() = runTest {
        settings.set(TimerSettings(autoStartBreak = true))
        start()
        advance(focusMs + 1_000L)

        reconcile()

        // RINGING, not RUNNING: reconcile refuses to start the next slot on the user's behalf.
        assertEquals(TimerStatus.RINGING, state().status)
        assertEquals(1, stats.recorded.size)
    }

    @Test
    fun `reconciling twice records once`() = runTest {
        start()
        advance(focusMs + 1_000L)

        assertTrue(reconcile())
        assertFalse(reconcile())

        assertEquals(1, stats.recorded.size)
        assertEquals(1, alerts.playCount)
    }

    /** Checklist §10.3: a reboot mid-slot must reconcile to exactly one recorded pomodoro. */
    @Test
    fun `reconciling after a reboot records exactly one slot`() = runTest {
        start()
        // The device was off for half an hour: the wall clock moved, uptime restarted near zero.
        clock.advanceBy(30 * 60_000L)
        elapsed.setMillis(8_000L)

        assertTrue(reconcile())

        assertEquals(1, stats.recorded.size)
        assertEquals(TimerStatus.RINGING, state().status)
    }

    @Test
    fun `a reboot before the slot was due leaves it running`() = runTest {
        start()
        clock.advanceBy(5 * 60_000L)
        elapsed.setMillis(8_000L)

        assertFalse(reconcile())
        assertEquals(TimerStatus.RUNNING, state().status)
        assertEquals(focusMs - 5 * 60_000L, remaining())
    }

    /** Checklist §10.4: moving the system clock must not make the pomodoro jump. */
    @Test
    fun `moving the system clock forward does not complete the slot`() = runTest {
        start()
        advance(60_000L)
        clock.advanceBy(60 * 60_000L)

        assertFalse(reconcile())
        assertEquals(TimerStatus.RUNNING, state().status)
        assertEquals(focusMs - 60_000L, remaining())
    }

    // ─── El día siguiente ───────────────────────────────────────────────────

    private val oneDayMs = 24 * 60 * 60_000L

    @Test
    fun `a cycle left from another day starts again at zero`() = runTest {
        settings.set(TimerSettings(autoStartBreak = false))
        start()
        advance(focusMs)
        complete()
        assertEquals(1, state().completedFocusInCycle)

        advance(oneDayMs)
        assertTrue(reconcile())

        val state = state()
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals("El día empieza con un pomodoro, no con el descanso de ayer", SlotType.FOCUS, state.slotType)
        assertEquals(0, state.completedFocusInCycle)
        assertEquals("La tanda de ayer queda cerrada", 0L, state.sessionId)
    }

    @Test
    fun `the cycle survives the same day`() = runTest {
        settings.set(TimerSettings(autoStartBreak = false))
        start()
        advance(focusMs)
        complete()

        advance(6 * 60 * 60_000L)
        assertFalse(reconcile())

        assertEquals(1, state().completedFocusInCycle)
        assertEquals(SlotType.SHORT_BREAK, state().slotType)
    }

    @Test
    fun `a slot that expired days ago leaves no cycle behind`() = runTest {
        start()
        advance(3 * oneDayMs)

        assertTrue(reconcile())

        assertEquals("El pomodoro vencido se registra igual", 1, stats.recorded.size)
        assertEquals(0, alerts.playCount)
        assertEquals(TimerStatus.IDLE, state().status)
        assertEquals(SlotType.FOCUS, state().slotType)
        assertEquals(0, state().completedFocusInCycle)
    }

    /** El pomodoro vencido se apunta en el día en que se hizo, no en el que se abre la app. */
    @Test
    fun `a slot that expired days ago is recorded on its own day`() = runTest {
        start()
        val startedDay = LocalDate.ofInstant(clock.instant(), clock.zone)
        advance(3 * oneDayMs)

        reconcile()

        assertEquals(startedDay, stats.recorded.single().localDate)
    }

    @Test
    fun `a pomodoro left paused overnight is recorded and swept`() = runTest {
        start()
        advance(10 * 60_000L)
        pause()

        advance(oneDayMs)
        assertTrue(reconcile())

        val recorded = stats.recorded.single()
        assertFalse("Abandonado, no completado", recorded.completed)
        assertEquals(10 * 60_000L, recorded.actualFocusMs)
        assertEquals(TimerStatus.IDLE, state().status)
        assertEquals(SlotType.FOCUS, state().slotType)
        assertEquals(0, state().completedFocusInCycle)
    }

    @Test
    fun `a pause of seconds left overnight records nothing`() = runTest {
        start()
        advance(30_000L)
        pause()

        advance(oneDayMs)
        assertTrue(reconcile())

        assertTrue(stats.recorded.isEmpty())
        assertEquals(TimerStatus.IDLE, state().status)
    }

    @Test
    fun `a timer paused this morning is not swept`() = runTest {
        start()
        advance(10 * 60_000L)
        pause()

        advance(60 * 60_000L)
        assertFalse(reconcile())

        assertEquals(TimerStatus.PAUSED, state().status)
        assertTrue(stats.recorded.isEmpty())
    }

    @Test
    fun `a timer running across midnight is left alone`() = runTest {
        // Empieza a las 23:50 y sigue corriendo pasada la medianoche: el pomodoro es de quien lo
        // empezó, y el barrido no toca nada que esté en marcha.
        val midnight = LocalDate.ofInstant(clock.instant(), clock.zone)
            .plusDays(1)
            .atStartOfDay(clock.zone)
            .toInstant()
            .toEpochMilli()
        advance(midnight - 10 * 60_000L - clock.millis())

        start()
        advance(15 * 60_000L)

        assertFalse(reconcile())

        assertEquals(TimerStatus.RUNNING, state().status)
        assertEquals(focusMs - 15 * 60_000L, remaining())
    }

    @Test
    fun `sweeping an idle timer twice changes nothing the second time`() = runTest {
        settings.set(TimerSettings(autoStartBreak = false))
        start()
        advance(focusMs)
        complete()

        advance(oneDayMs)
        assertTrue(reconcile())
        assertFalse(reconcile())
    }

    // ─── Settings interaction ───────────────────────────────────────────────

    @Test
    fun `an idle timer follows the duration just set in settings`() = runTest {
        settings.set(TimerSettings(focusMinutes = 40))

        assertEquals(40 * 60_000L, remaining())

        start()
        assertEquals(40 * 60_000L, state().slotDurationMs)
    }

    // The frozen snapshot: changing Settings must not reshape a run already under way.
    @Test
    fun `changing the duration mid slot does not change the running slot`() = runTest {
        start()
        advance(5 * 60_000L)

        settings.set(TimerSettings(focusMinutes = 60))

        assertEquals(focusMs, state().slotDurationMs)
        assertEquals(focusMs - 5 * 60_000L, remaining())
    }

    @Test
    fun `changing the cycle length mid run does not reshape the batch`() = runTest {
        start()
        settings.set(TimerSettings(pomodorosPerCycle = 2))

        assertEquals(TimerSettings.DEFAULT_POMODOROS_PER_CYCLE, state().pomodorosPerCycle)
    }

    @Test
    fun `the cycle position reads one based during focus`() = runTest {
        start()
        assertEquals(1, state().cyclePosition)

        advance(focusMs)
        complete()
        // On the break that follows, the dots still report the pomodoro just finished.
        assertEquals(1, state().cyclePosition)

        start()
        advance(shortBreakMs)
        complete()
        assertEquals(2, state().cyclePosition)
    }

    // ─── The runtime: service, alarm and notification ───────────────────────

    @Test
    fun `starting arms the alarm at the monotonic deadline and starts the service`() = runTest {
        start()

        assertEquals(state().endAtElapsedRealtimeMs, runtime.armedDeadlineMs)
        assertTrue(runtime.serviceRunning)
    }

    // No point burning a wakelock and a foreground notification on a stopped clock.
    @Test
    fun `pausing stops the service, cancels the alarm and posts a paused notification`() = runTest {
        start()
        advance(60_000L)
        pause()

        assertFalse(runtime.serviceRunning)
        assertEquals(null, runtime.armedDeadlineMs)
        assertEquals(1, runtime.pausedNotificationCount)
    }

    @Test
    fun `resuming arms the alarm again`() = runTest {
        start()
        advance(60_000L)
        pause()
        resume()

        assertEquals(state().endAtElapsedRealtimeMs, runtime.armedDeadlineMs)
        assertTrue(runtime.serviceRunning)
    }

    @Test
    fun `ringing tears the service down and posts the alert notification`() = runTest {
        settings.set(TimerSettings(autoStartBreak = false))
        start()
        advance(focusMs)
        complete()

        assertFalse(runtime.serviceRunning)
        assertEquals(null, runtime.armedDeadlineMs)
        assertEquals(1, runtime.finishedNotificationCount)
    }

    @Test
    fun `auto start rearms the alarm for the next slot`() = runTest {
        settings.set(TimerSettings(autoStartBreak = true))
        start()
        advance(focusMs)
        complete()

        assertTrue(runtime.serviceRunning)
        assertEquals(state().endAtElapsedRealtimeMs, runtime.armedDeadlineMs)
    }

    /**
     * Chaining still tells you the pomodoro ended.
     *
     * It used not to: with auto-start on there was no `RINGING` state, so no alert was published and the
     * phone just buzzed. On a paired watch — the reason this changed — a pomodoro would end with nothing on
     * the wrist to say so, which is precisely when the phone is in another room.
     */
    @Test
    fun `chaining into the break still publishes the alert`() = runTest {
        settings.set(TimerSettings(autoStartBreak = true))
        start()
        advance(focusMs)

        complete()

        assertEquals(TimerStatus.RUNNING, state().status)
        assertEquals(SlotType.SHORT_BREAK, state().slotType)
        assertEquals(1, runtime.finishedNotificationCount)
        assertEquals("It has to know the slot is already under way", 1, runtime.chainedNotificationCount)
        assertEquals(1, alerts.playCount)
    }

    @Test
    fun `a slot that waits for the user is not announced as chained`() = runTest {
        settings.set(TimerSettings(autoStartBreak = false))
        start()
        advance(focusMs)

        complete()

        assertEquals(1, runtime.finishedNotificationCount)
        assertEquals(0, runtime.chainedNotificationCount)
    }

    @Test
    fun `resetting releases the runtime`() = runTest {
        start()
        advance(60_000L)
        reset()

        assertFalse(runtime.serviceRunning)
        assertEquals(null, runtime.armedDeadlineMs)
    }

    /**
     * Level 3 of the degradation ladder in ADR 002: the OEM refuses the foreground start, and the timer
     * still works off the persisted state plus the alarm.
     */
    @Test
    fun `a refused foreground start still leaves the alarm armed`() = runTest {
        runtime.serviceStartAllowed = false

        assertTrue(start())

        assertFalse(runtime.serviceRunning)
        assertEquals(state().endAtElapsedRealtimeMs, runtime.armedDeadlineMs)
        assertEquals(TimerStatus.RUNNING, state().status)
    }

    @Test
    fun `reconciling a live slot rearms the runtime after the process was killed`() = runTest {
        start()
        runtime.cancel()
        runtime.stop()
        advance(60_000L)

        assertFalse(reconcile())

        assertEquals(state().endAtElapsedRealtimeMs, runtime.armedDeadlineMs)
        assertTrue(runtime.serviceRunning)
    }

    @Test
    fun `a state written from scratch is read back verbatim`() = runTest {
        val written = TimerState(
            status = TimerStatus.PAUSED,
            slotType = SlotType.LONG_BREAK,
            sessionId = 42L,
            slotIndex = 7,
            completedFocusInCycle = 4,
            pomodorosPerCycle = 4,
            slotDurationMs = longBreakMs,
            slotStartedAtEpochMs = startEpochMs,
            endAtEpochMs = startEpochMs + longBreakMs,
            endAtElapsedRealtimeMs = startElapsedMs + longBreakMs,
            bootEpochMs = startEpochMs - startElapsedMs,
            remainingAtPauseMs = 12_345L,
        )
        timerState.write(written)

        assertEquals(written, state())
    }
}
