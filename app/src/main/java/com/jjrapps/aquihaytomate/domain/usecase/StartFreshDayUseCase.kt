package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerNotifier
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Sweeps a stopped timer left over from an earlier day, so the app opens on a clean `1/4` focus slot
 * instead of on the middle of a cycle nobody is in.
 *
 * Called from [ReconcileTimerUseCase], which is to say from every entrypoint that can be the first
 * thing touched in the morning: opening the app, tapping the widget, or booting the phone.
 *
 * Two things it takes care of on the way out:
 * - **A pomodoro that was left paused is abandoned, not lost.** The time really focused is recorded as
 *   a partial slot first, under the same one-minute rule `REINICIAR` and `SALTAR` use.
 * - **A slot that was still ringing stops ringing.** Its notification is from another day and its two
 *   actions now describe a slot that no longer exists.
 *
 * See docs/decisions/015-el-ciclo-se-reinicia-al-cambiar-de-dia.md.
 */
class StartFreshDayUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val settingsRepository: SettingsRepository,
    private val recordFocusSlot: RecordFocusSlotUseCase,
    private val syncTimerRuntime: SyncTimerRuntimeUseCase,
    private val notifier: TimerNotifier,
    private val clock: Clock,
) {

    /**
     * @param force sweep without asking [DayRollover] whether the day turned. `reconcile()` passes true
     *   right after closing a slot that expired on an earlier day: the transition it just made stamped
     *   the state with the current time, so the rollover would no longer recognise it as stale — the
     *   pomodoro was yesterday's, but the state now says it was touched a millisecond ago.
     * @return true when yesterday's leftovers had to be swept.
     */
    suspend operator fun invoke(force: Boolean = false): Boolean {
        val nowEpochMs = clock.millis()
        val state = timerStateRepository.current()
        if (state.status == TimerStatus.RUNNING) return false
        if (!force && !DayRollover.startsNewDay(state, nowEpochMs, clock.zone)) return false

        val settings = settingsRepository.current()

        // Recorded before the sweep, and off `remainingAtPauseMs` rather than off the clocks: a paused
        // slot has no live deadline to read. The row is dated by `slotStartedAtEpochMs`, so the focus
        // time lands on the day it was actually done, not on today.
        if (state.status == TimerStatus.PAUSED) {
            recordFocusSlot(
                state = state,
                activeElapsedMs = TimerMath.activeElapsedMs(
                    state.slotDurationMs,
                    state.remainingAtPauseMs,
                ),
                completed = false,
            )
        }

        val changed = timerStateRepository.update { current ->
            val stillStale = current.status != TimerStatus.RUNNING &&
                (force || DayRollover.startsNewDay(current, nowEpochMs, clock.zone))
            if (!stillStale) {
                null
            } else {
                // A brand new batch: `sessionId` back to zero, so the next start opens one of today
                // rather than appending to yesterday's.
                TimerState.idle(settings).copy(lastActivityEpochMs = nowEpochMs)
            }
        }

        if (changed) {
            if (state.status == TimerStatus.RINGING) notifier.clearAlert()
            syncTimerRuntime()
        }
        return changed
    }
}
