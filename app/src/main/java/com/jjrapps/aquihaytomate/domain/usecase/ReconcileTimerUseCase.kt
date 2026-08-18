package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import java.time.Clock
import javax.inject.Inject

/**
 * Repairs the state after the process was killed, the device rebooted, or the app simply was not
 * looking when a slot ran out. Called when the app opens, when the widget is tapped and from
 * `BootReceiver`.
 *
 * It is also where the day turns: a stopped timer left over from an earlier day is swept by
 * [StartFreshDayUseCase] before anything else, so the app never opens in the middle of yesterday's
 * cycle.
 *
 * **It never simulates more than one expired slot**, even with auto-start on. If the phone spent eight
 * hours off, the slot that expired is recorded, the timer stops there, and that is that — the rule that
 * stops opening the app in the morning from inserting sixteen pomodoros nobody worked. See CLAUDE.md §6
 * and docs/decisions/002-motor-del-temporizador-hibrido.md.
 */
class ReconcileTimerUseCase @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val completeSlot: CompleteSlotUseCase,
    private val startFreshDay: StartFreshDayUseCase,
    private val syncTimerRuntime: SyncTimerRuntimeUseCase,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    /** @return true when something had to be repaired. */
    suspend operator fun invoke(): Boolean {
        // First, because a slot that expired while the timer was running still has to be recorded on the
        // day it happened, and the sweep declines to touch anything that is running.
        val sweptStaleDay = startFreshDay()

        val state = timerStateRepository.current()
        if (state.status != TimerStatus.RUNNING) return sweptStaleDay

        val nowEpochMs = clock.millis()
        val nowElapsedRealtimeMs = elapsedRealtime.millis()
        if (!TimerMath.isExpired(state, nowEpochMs, nowElapsedRealtimeMs)) {
            // Still running, but the service may well be gone — the process could have been killed, or
            // this could be the first call after a reboot, where the old alarm died with the old uptime.
            syncTimerRuntime()
            return false
        }

        // How stale the expiry is decides whether the user hears about it. Ringing for something that
        // finished before breakfast is noise, but the pomodoro still deserves to be recorded.
        val overdueMs = overdueMs(state, nowEpochMs, nowElapsedRealtimeMs)
        val worthAlerting = overdueMs <= STALE_EXPIRY_MS

        val closed = completeSlot(alertUser = worthAlerting, allowAutoStart = false)

        // The slot is recorded on the day it ran out, but the cycle it left behind belongs to that day
        // too: a pomodoro that expired on Tuesday must not open Friday's app on `3/4`.
        if (closed && DayRollover.isEarlierDay(state.endAtEpochMs, nowEpochMs, clock.zone)) {
            startFreshDay(force = true)
        }
        return closed || sweptStaleDay
    }

    private fun overdueMs(
        state: TimerState,
        nowEpochMs: Long,
        nowElapsedRealtimeMs: Long,
    ): Long = if (TimerMath.hasRebooted(state, nowElapsedRealtimeMs)) {
        nowEpochMs - state.endAtEpochMs
    } else {
        nowElapsedRealtimeMs - state.endAtElapsedRealtimeMs
    }.coerceAtLeast(0L)

    companion object {
        /** Past this, a finished slot is recorded silently instead of ringing. */
        const val STALE_EXPIRY_MS = 30 * 60_000L
    }
}
