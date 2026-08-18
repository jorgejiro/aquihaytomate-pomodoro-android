package com.jjrapps.aquihaytomate.ui.timer

import app.cash.turbine.test
import com.jjrapps.aquihaytomate.domain.model.KeepScreenOnMode
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.usecase.CompleteSlotUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ObserveSettingsUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ObserveTimerStateUseCase
import com.jjrapps.aquihaytomate.domain.usecase.PauseTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ReconcileTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.RecordFocusSlotUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ResetTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.SkipSlotUseCase
import com.jjrapps.aquihaytomate.domain.usecase.StartFreshDayUseCase
import com.jjrapps.aquihaytomate.domain.usecase.StartTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.SyncTimerRuntimeUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ToggleTimerUseCase
import com.jjrapps.aquihaytomate.testing.FakeAlertPlayer
import com.jjrapps.aquihaytomate.testing.FakeChargingMonitor
import com.jjrapps.aquihaytomate.testing.FakeSettingsRepository
import com.jjrapps.aquihaytomate.testing.FakeStatsRepository
import com.jjrapps.aquihaytomate.testing.FakeTimerRuntime
import com.jjrapps.aquihaytomate.testing.FakeTimerStateRepository
import com.jjrapps.aquihaytomate.testing.MutableClock
import com.jjrapps.aquihaytomate.testing.MutableElapsedRealtime
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val startEpochMs = 1_800_000_000_000L
    private val startElapsedMs = 90_000_000L
    private val focusMs = 25 * 60_000L

    private val dispatcher = StandardTestDispatcher()
    private val clock = MutableClock(Instant.ofEpochMilli(startEpochMs), ZoneId.of("Europe/Madrid"))
    private val elapsed = MutableElapsedRealtime(startElapsedMs)
    private val timerState = FakeTimerStateRepository()
    private val settings = FakeSettingsRepository()
    private val stats = FakeStatsRepository()
    private val alerts = FakeAlertPlayer()
    private val charging = FakeChargingMonitor()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(): TimerViewModel {
        val runtime = FakeTimerRuntime()
        val recordFocusSlot = RecordFocusSlotUseCase(stats, clock)
        val sync = SyncTimerRuntimeUseCase(timerState, settings, runtime, runtime, runtime, clock, elapsed)
        val start = StartTimerUseCase(timerState, settings, sync, clock, elapsed)
        val pause = PauseTimerUseCase(timerState, sync, clock, elapsed)
        val complete =
            CompleteSlotUseCase(timerState, settings, recordFocusSlot, alerts, runtime, sync, clock, elapsed)
        return TimerViewModel(
            observeTimerState = ObserveTimerStateUseCase(timerState),
            observeSettings = ObserveSettingsUseCase(settings),
            chargingMonitor = charging,
            toggleTimer = ToggleTimerUseCase(timerState, start, pause),
            resetTimer = ResetTimerUseCase(
                timerState,
                settings,
                recordFocusSlot,
                sync,
                clock,
                elapsed,
            ),
            skipSlot = SkipSlotUseCase(timerState, settings, recordFocusSlot, sync, clock, elapsed),
            completeSlot = complete,
            reconcileTimer = ReconcileTimerUseCase(
                timerState,
                complete,
                StartFreshDayUseCase(timerState, settings, recordFocusSlot, sync, runtime, clock),
                sync,
                clock,
                elapsed,
            ),
            clock = clock,
            elapsedRealtime = elapsed,
        )
    }

    /** Moves the test scheduler and both clocks together, so virtual time stays consistent. */
    private fun TestScope.advanceAll(millis: Long) {
        clock.advanceBy(millis)
        elapsed.advanceBy(millis)
        advanceTimeBy(millis)
        runCurrent()
    }

    @Test
    fun `it starts idle showing a full tomato and the start control`() = runTest(dispatcher) {
        viewModel().uiState.test {
            assertEquals(TimerUiState.Loading, awaitItem())

            val state = awaitItem() as TimerUiState.Success
            assertEquals(TimerStatus.IDLE, state.status)
            assertEquals("25:00", state.timeText)
            assertEquals(1f, state.fillFraction, 0.0001f)
            assertEquals(PrimaryControl.START, state.primaryControl)
            assertFalse(state.showReset)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the primary control starts, pauses and resumes`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            assertEquals(PrimaryControl.START, (awaitItem() as TimerUiState.Success).primaryControl)

            vm.onPrimaryControlClick()
            runCurrent()
            assertEquals(PrimaryControl.PAUSE, (awaitItem() as TimerUiState.Success).primaryControl)

            vm.onPrimaryControlClick()
            runCurrent()
            val paused = awaitItem() as TimerUiState.Success
            assertEquals(PrimaryControl.RESUME, paused.primaryControl)
            assertTrue(paused.showReset)

            vm.onPrimaryControlClick()
            runCurrent()
            assertEquals(PrimaryControl.PAUSE, (awaitItem() as TimerUiState.Success).primaryControl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the countdown ticks once a second while running`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            awaitItem()

            vm.onPrimaryControlClick()
            runCurrent()
            assertEquals("25:00", (awaitItem() as TimerUiState.Success).timeText)

            advanceAll(1_000L)
            assertEquals("24:59", (awaitItem() as TimerUiState.Success).timeText)

            advanceAll(1_000L)
            assertEquals("24:58", (awaitItem() as TimerUiState.Success).timeText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the tomato drains as the slot runs`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            awaitItem()
            vm.onPrimaryControlClick()
            runCurrent()
            skipItems(1)

            advanceAll(focusMs / 2)
            val halfway = expectMostRecentItem() as TimerUiState.Success
            assertEquals(0.5f, halfway.fillFraction, 0.01f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** The F3 milestone: a whole pomodoro completes with the app open. */
    @Test
    fun `a slot that runs out completes itself and offers the break`() = runTest(dispatcher) {
        // Auto-starting the break is the default; this test is about the slot waiting for a tap instead.
        settings.set(TimerSettings(autoStartBreak = false))
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            awaitItem()
            vm.onPrimaryControlClick()
            runCurrent()

            advanceAll(focusMs)

            val ringing = expectMostRecentItem() as TimerUiState.Success
            assertEquals(TimerStatus.RINGING, ringing.status)
            assertEquals(SlotType.SHORT_BREAK, ringing.slotType)
            assertEquals(PrimaryControl.START_BREAK, ringing.primaryControl)
            assertEquals(0f, ringing.fillFraction, 0.0001f)

            assertEquals(1, stats.recorded.size)
            assertEquals(1, alerts.playCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `after a break ends the control invites going back to work`() = runTest(dispatcher) {
        settings.set(TimerSettings(autoStartBreak = false))
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            awaitItem()

            vm.onPrimaryControlClick()
            runCurrent()
            advanceAll(focusMs)

            // Start the break, then let it run out too.
            vm.onPrimaryControlClick()
            runCurrent()
            advanceAll(5 * 60_000L)

            val state = expectMostRecentItem() as TimerUiState.Success
            assertEquals(SlotType.FOCUS, state.slotType)
            assertEquals(PrimaryControl.BACK_TO_WORK, state.primaryControl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetting brings back a full tomato and hides the reset control`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            awaitItem()
            vm.onPrimaryControlClick()
            runCurrent()
            skipItems(1)
            advanceAll(3 * 60_000L)
            skipItems(1)

            vm.onResetClick()
            runCurrent()

            val state = expectMostRecentItem() as TimerUiState.Success
            assertEquals(TimerStatus.IDLE, state.status)
            assertEquals("25:00", state.timeText)
            assertEquals(1f, state.fillFraction, 0.0001f)
            assertFalse(state.showReset)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skipping a focus slot moves to the break`() = runTest(dispatcher) {
        settings.set(TimerSettings(autoStartBreak = false))
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            awaitItem()
            vm.onPrimaryControlClick()
            runCurrent()
            skipItems(1)

            vm.onSkipClick()
            runCurrent()

            val state = expectMostRecentItem() as TimerUiState.Success
            assertEquals(SlotType.SHORT_BREAK, state.slotType)
            assertEquals(TimerStatus.IDLE, state.status)
            assertEquals(PrimaryControl.START, state.primaryControl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an idle timer follows a duration change in settings`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            assertEquals("25:00", (awaitItem() as TimerUiState.Success).timeText)

            settings.set(TimerSettings(focusMinutes = 45))
            runCurrent()

            assertEquals("45:00", (expectMostRecentItem() as TimerUiState.Success).timeText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the cycle readout follows the completed pomodoros`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            (awaitItem() as TimerUiState.Success).let { state ->
                assertEquals(0, state.completedInCycle)
                assertEquals(1, state.cyclePosition)
                assertEquals(4, state.pomodorosPerCycle)
            }

            vm.onPrimaryControlClick()
            runCurrent()
            advanceAll(focusMs)

            (expectMostRecentItem() as TimerUiState.Success).let { state ->
                assertEquals(1, state.completedInCycle)
                assertEquals(1, state.cyclePosition)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the up next readout announces the break a running focus leads to`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            (awaitItem() as TimerUiState.Success).let { state ->
                assertEquals(NextSlot(SlotType.SHORT_BREAK, 5), state.nextSlot)
            }

            vm.onPrimaryControlClick()
            runCurrent()

            (expectMostRecentItem() as TimerUiState.Success).let { state ->
                assertEquals(NextSlot(SlotType.SHORT_BREAK, 5), state.nextSlot)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Ringing already points at the next slot, so a readout here would name the one after it.
    @Test
    fun `there is no up next readout while ringing`() = runTest(dispatcher) {
        settings.set(TimerSettings(autoStartBreak = false))
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            awaitItem()
            vm.onPrimaryControlClick()
            runCurrent()
            advanceAll(focusMs)

            val state = expectMostRecentItem() as TimerUiState.Success
            assertEquals(TimerStatus.RINGING, state.status)
            assertEquals(null, state.nextSlot)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Skip is offered for a break that has not started, but not for the untouched first focus slot.
    @Test
    fun `skip is hidden on the first idle focus and offered on an idle break`() =
        runTest(dispatcher) {
            settings.set(TimerSettings(autoStartBreak = false))
            val vm = viewModel()
            vm.uiState.test {
                skipItems(1)
                assertFalse((awaitItem() as TimerUiState.Success).showSkip)

                vm.onPrimaryControlClick()
                runCurrent()
                vm.onSkipClick()
                runCurrent()

                (expectMostRecentItem() as TimerUiState.Success).let { state ->
                    assertEquals(SlotType.SHORT_BREAK, state.slotType)
                    assertEquals(TimerStatus.IDLE, state.status)
                    assertTrue(state.showSkip)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `keep screen on while charging follows the charger`() = runTest(dispatcher) {
        settings.set(TimerSettings(keepScreenOn = KeepScreenOnMode.WHILE_CHARGING))
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            assertFalse((awaitItem() as TimerUiState.Success).keepScreenOn)

            charging.set(true)
            runCurrent()
            assertTrue((awaitItem() as TimerUiState.Success).keepScreenOn)

            charging.set(false)
            runCurrent()
            assertFalse((awaitItem() as TimerUiState.Success).keepScreenOn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** The rule is about the screen being open, not about the timer running. Idle counts. */
    @Test
    fun `keep screen on while charging applies with the timer idle`() = runTest(dispatcher) {
        settings.set(TimerSettings(keepScreenOn = KeepScreenOnMode.WHILE_CHARGING))
        charging.set(true)
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            (awaitItem() as TimerUiState.Success).let { state ->
                assertEquals(TimerStatus.IDLE, state.status)
                assertTrue(state.keepScreenOn)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keep screen on never ignores the charger`() = runTest(dispatcher) {
        settings.set(TimerSettings(keepScreenOn = KeepScreenOnMode.NEVER))
        charging.set(true)
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            assertFalse((awaitItem() as TimerUiState.Success).keepScreenOn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keep screen on always applies without a charger`() = runTest(dispatcher) {
        settings.set(TimerSettings(keepScreenOn = KeepScreenOnMode.ALWAYS))
        val vm = viewModel()
        vm.uiState.test {
            skipItems(1)
            assertTrue((awaitItem() as TimerUiState.Success).keepScreenOn)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
