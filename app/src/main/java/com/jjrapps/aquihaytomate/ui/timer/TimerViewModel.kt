package com.jjrapps.aquihaytomate.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import com.jjrapps.aquihaytomate.domain.usecase.CompleteSlotUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ObserveSettingsUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ObserveTimerStateUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ReconcileTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ResetTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.SkipSlotUseCase
import com.jjrapps.aquihaytomate.domain.usecase.SlotPlanner
import com.jjrapps.aquihaytomate.domain.usecase.TimerMath
import com.jjrapps.aquihaytomate.domain.usecase.ToggleTimerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TimerViewModel @Inject constructor(
    observeTimerState: ObserveTimerStateUseCase,
    observeSettings: ObserveSettingsUseCase,
    private val toggleTimer: ToggleTimerUseCase,
    private val resetTimer: ResetTimerUseCase,
    private val skipSlot: SkipSlotUseCase,
    private val completeSlot: CompleteSlotUseCase,
    private val reconcileTimer: ReconcileTimerUseCase,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) : ViewModel() {

    private val timerState: Flow<TimerState> = observeTimerState()
    private val settings: Flow<TimerSettings> = observeSettings()

    /**
     * The screen state.
     *
     * The clock only ticks while the timer is running: outside `RUNNING` the remaining time cannot
     * change on its own, so a single emission per state change is all the screen needs. The tick is
     * aligned to the whole second, otherwise every iteration drifts a little further into it and the
     * figure eventually skips a number.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TimerUiState> =
        combine(timerState, settings) { state, currentSettings -> state to currentSettings }
            .flatMapLatest { (state, currentSettings) ->
                if (state.status == TimerStatus.RUNNING) {
                    secondTicker().map { render(state, currentSettings) }
                } else {
                    flowOf(render(state, currentSettings))
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = TimerUiState.Loading,
            )

    init {
        // The app opening is one of the moments the engine repairs itself: the process may have been
        // killed, or a slot may have run out while nobody was watching.
        viewModelScope.launch { reconcileTimer() }
        viewModelScope.launch { closeSlotsWhenTheyRunOut() }
    }

    fun onPrimaryControlClick() {
        viewModelScope.launch { toggleTimer() }
    }

    fun onResetClick() {
        viewModelScope.launch { resetTimer() }
    }

    fun onSkipClick() {
        viewModelScope.launch { skipSlot() }
    }

    /**
     * Closes the slot the moment it runs out, while the app is in the foreground.
     *
     * A single `delay` to the deadline rather than a check on every tick, so the transition lands on
     * the second instead of up to a second late. `collectLatest` cancels the pending delay whenever
     * the state changes — a pause, a reset, a skip — so there is no stale timer to guard against.
     *
     * This is the F3 mechanism, good enough while the app is open. F4 adds the foreground service and
     * the backup alarm for everything else; `CompleteSlotUseCase` is idempotent, so all three racing
     * for the same slot is harmless.
     */
    private suspend fun closeSlotsWhenTheyRunOut() {
        timerState.collectLatest { state ->
            if (state.status != TimerStatus.RUNNING) return@collectLatest
            val remainingMs =
                TimerMath.remainingMs(state, clock.millis(), elapsedRealtime.millis())
            delay(remainingMs)
            completeSlot()
        }
    }

    private fun secondTicker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TimerMath.msUntilNextSecond(clock.millis()))
        }
    }

    private fun render(state: TimerState, settings: TimerSettings): TimerUiState.Success {
        val durationMs = state.durationMsWith(settings)
        val remainingMs =
            TimerMath.remainingMs(state, clock.millis(), elapsedRealtime.millis(), settings)

        return TimerUiState.Success(
            status = state.status,
            slotType = state.slotType,
            timeText = TimerMath.formatRemaining(remainingMs),
            remainingMs = remainingMs,
            // Ringing shows an empty tomato: the slot it belonged to is over, even though the state
            // already describes the next one.
            fillFraction = if (state.status == TimerStatus.RINGING) {
                0f
            } else {
                TimerMath.fillFraction(durationMs, remainingMs)
            },
            primaryControl = primaryControlFor(state),
            completedInCycle = state.completedFocusInCycle,
            cyclePosition = state.cyclePosition,
            pomodorosPerCycle = state.pomodorosPerCycleWith(settings),
            keepScreenOn = settings.keepScreenOn && state.status == TimerStatus.RUNNING,
            nextSlot = nextSlotFor(state, settings),
        )
    }

    /**
     * The "up next" readout, through the same pure planner the engine uses when the slot really ends.
     *
     * Nothing to show while ringing: the state has already advanced to the next slot, so the planner would
     * answer with the one after that, and the primary control is naming the immediate one anyway.
     */
    private fun nextSlotFor(state: TimerState, settings: TimerSettings): NextSlot? {
        if (state.status == TimerStatus.RINGING) return null
        val planned = SlotPlanner.upcomingSlot(state, settings)

        return NextSlot(
            type = planned.type,
            minutes = (planned.durationMs / TimerSettings.MINUTE_MS).toInt(),
        )
    }

    /**
     * While ringing, the state already points at the *next* slot, so what just finished is implied: a
     * break coming up means a focus slot ended, and a focus slot coming up means a break did.
     */
    private fun primaryControlFor(state: TimerState): PrimaryControl = when (state.status) {
        TimerStatus.IDLE -> PrimaryControl.START
        TimerStatus.RUNNING -> PrimaryControl.PAUSE
        TimerStatus.PAUSED -> PrimaryControl.RESUME
        TimerStatus.RINGING ->
            if (state.slotType == SlotType.FOCUS) {
                PrimaryControl.BACK_TO_WORK
            } else {
                PrimaryControl.START_BREAK
            }
    }

    private companion object {
        /** Survives a configuration change without tearing the collectors down. */
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
