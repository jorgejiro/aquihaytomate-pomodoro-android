package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.AlertPlayer
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerNotifier
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import java.time.Clock
import javax.inject.Inject

/**
 * Closes a slot that has run out: records it, moves the state on and fires the alert.
 *
 * **The only place a slot is ever closed, and idempotent by construction.** Three mechanisms can call
 * it for the same slot — the foreground service, the backup alarm and `reconcile()` — and the result
 * has to be one history row and one alert regardless of how many of them arrive:
 *
 * 1. The row is written first, through `UNIQUE(session_id, slot_index)` with `IGNORE`. Writing before
 *    the state transition is deliberate: if the process dies in between, the row is already safe and
 *    `reconcile()` will redo the transition, where the duplicate insert is a no-op. The other order
 *    would lose the pomodoro.
 * 2. The transition is a compare-and-set inside the DataStore transaction. Exactly one caller sees a
 *    `RUNNING` state matching the slot it means to close; the rest get null back and do nothing.
 * 3. **Only the caller that won the compare-and-set alerts.** That is why the alert comes last and is
 *    gated on the return value of the update, rather than on having written the row.
 *
 * See CLAUDE.md §6 and docs/decisions/002-motor-del-temporizador-hibrido.md.
 */
class CompleteSlotUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val settingsRepository: SettingsRepository,
    private val recordFocusSlot: RecordFocusSlotUseCase,
    private val alertPlayer: AlertPlayer,
    private val notifier: TimerNotifier,
    private val syncTimerRuntime: SyncTimerRuntimeUseCase,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    /**
     * @param alertUser false to close the slot quietly, which is what `reconcile()` does for a slot
     *   that expired long ago: recording it is right, waking the user about it is not.
     * @param allowAutoStart false to refuse to chain into the next slot even when the setting is on.
     *   `reconcile()` passes false, because auto-starting a slot that expired while the phone was off
     *   would cascade through a whole morning of fake pomodoros.
     * @return true when this call is the one that closed the slot.
     */
    suspend operator fun invoke(
        alertUser: Boolean = true,
        allowAutoStart: Boolean = true,
    ): Boolean {
        val state = timerStateRepository.current()
        if (state.status != TimerStatus.RUNNING) return false

        val settings = settingsRepository.current()
        val nowEpochMs = clock.millis()
        val nowElapsedRealtimeMs = elapsedRealtime.millis()

        // A caller that arrives early — a mis-set alarm, a spurious service tick — must not truncate
        // the slot.
        if (!TimerMath.isExpired(state, nowEpochMs, nowElapsedRealtimeMs)) return false

        recordFocusSlot(
            state = state,
            activeElapsedMs = state.slotDurationMs,
            completed = true,
        )

        val planned = SlotPlanner.planNextSlot(
            currentType = state.slotType,
            completedFocusInCycle = state.completedFocusInCycle,
            currentSlotIndex = state.slotIndex,
            completedFully = true,
            settings = settings,
        )
        // Auto-start is decided by what comes *next*, not by what just ended: see
        // TimerSettings.autoStartsInto. Note that the alert still fires below when it chains — the point
        // of auto-starting the break is not to be silent about it, it is to not have to tap.
        val nextStatus = when {
            allowAutoStart && settings.autoStartsInto(planned.type) -> TimerStatus.RUNNING
            alertUser -> TimerStatus.RINGING
            else -> TimerStatus.IDLE
        }

        val closedByUs = timerStateRepository.update { current ->
            val stillOurs = current.status == TimerStatus.RUNNING &&
                current.sessionId == state.sessionId &&
                current.slotIndex == state.slotIndex
            if (!stillOurs) {
                null
            } else {
                TimerTransitions.advanceTo(
                    state = current,
                    planned = planned,
                    status = nextStatus,
                    settings = settings,
                    nowEpochMs = nowEpochMs,
                    nowElapsedRealtimeMs = nowElapsedRealtimeMs,
                )
            }
        }

        if (closedByUs) {
            // Rearm or tear down before alerting: the alert can take seconds of vibration, and the
            // next slot's alarm should already be armed by then.
            syncTimerRuntime()

            if (alertUser) {
                // When the next slot chained on its own, `syncTimerRuntime` cleared the alert on its way
                // through RUNNING and nothing else would publish one — so the phone would buzz and leave no
                // trace, and a paired watch would never hear about the pomodoro ending. Published after the
                // sync, precisely so it survives that clear.
                if (nextStatus == TimerStatus.RUNNING) {
                    notifier.showSlotFinished(timerStateRepository.current(), chained = true)
                }
                // El sonido lo elige el slot que **acaba**, no el que empieza: acabar un pomodoro suena
                // suave y acabar un descanso suena duro. `state.slotType` sigue siendo el que se cierra
                // aquí, porque el avance se hizo sobre una copia del estado.
                alertPlayer.play(
                    settings.alertSoundFor(state.slotType),
                    settings.vibrationSeconds,
                )
            }
        }
        return closedByUs
    }
}
