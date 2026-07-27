package com.jjrapps.aquihaytomate.domain.repository

import com.jjrapps.aquihaytomate.domain.model.TimerState
import kotlinx.coroutines.flow.Flow

/**
 * Layer 1 of the engine: the persisted single source of truth. Four consumers read it — the UI, the
 * widget, the notification and the service — and it has to outlive the process, which is why it is
 * never a `StateFlow` in a service or a singleton. See CLAUDE.md §6.
 */
interface TimerStateRepository {

    val state: Flow<TimerState>

    suspend fun current(): TimerState

    suspend fun write(state: TimerState)

    /**
     * Atomic read-modify-write. The transform runs inside the DataStore transaction, so two
     * entrypoints racing to close the same slot cannot both win.
     *
     * @param transform returns the new state, or null to leave it untouched.
     * @return true when a change was written. Callers use this as their compare-and-set: only the
     *   caller that actually performed the transition plays the alert.
     */
    suspend fun update(transform: (TimerState) -> TimerState?): Boolean
}
