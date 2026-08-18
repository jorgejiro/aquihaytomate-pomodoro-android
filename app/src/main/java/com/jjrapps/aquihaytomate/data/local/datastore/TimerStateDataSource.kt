package com.jjrapps.aquihaytomate.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.di.TimerStatePreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The timer state, in its own DataStore file (`timer_state.preferences_pb`).
 *
 * This is layer 1 of the engine — the only source of truth about the countdown. Nothing here ticks:
 * the file holds deadlines, and everybody derives the remaining time from them through `TimerMath`.
 */
@Singleton
class TimerStateDataSource @Inject constructor(
    @param:TimerStatePreferences private val dataStore: DataStore<Preferences>,
) {

    val state: Flow<TimerState> = dataStore.data.map { it.toState() }

    suspend fun current(): TimerState = state.first()

    suspend fun write(state: TimerState) {
        dataStore.edit { it.put(state) }
    }

    /**
     * Atomic read-modify-write.
     *
     * `DataStore.edit` serialises transactions on a single file, so this is a real compare-and-set:
     * when the service and the backup alarm both try to close the same slot, exactly one transform
     * sees a `RUNNING` state and the other sees the already-transitioned one and bails out with null.
     * That is what keeps the alert from firing twice without any in-memory flag.
     *
     * @return true when [transform] produced a state different from the stored one.
     */
    suspend fun update(transform: (TimerState) -> TimerState?): Boolean {
        var changed = false
        dataStore.edit { preferences ->
            val currentState = preferences.toState()
            val next = transform(currentState)
            if (next != null && next != currentState) {
                preferences.put(next)
                changed = true
            }
        }
        return changed
    }

    private fun Preferences.toState(): TimerState {
        val defaults = TimerState.EMPTY
        return TimerState(
            status = TimerStatus.fromName(this[Keys.STATUS]),
            slotType = SlotType.fromName(this[Keys.SLOT_TYPE]),
            sessionId = this[Keys.SESSION_ID] ?: defaults.sessionId,
            slotIndex = this[Keys.SLOT_INDEX] ?: defaults.slotIndex,
            completedFocusInCycle = this[Keys.COMPLETED_FOCUS_IN_CYCLE]
                ?: defaults.completedFocusInCycle,
            pomodorosPerCycle = (this[Keys.POMODOROS_PER_CYCLE] ?: defaults.pomodorosPerCycle)
                .coerceIn(TimerSettings.POMODOROS_PER_CYCLE_RANGE),
            slotDurationMs = this[Keys.SLOT_DURATION_MS] ?: defaults.slotDurationMs,
            slotStartedAtEpochMs = this[Keys.SLOT_STARTED_AT_EPOCH_MS]
                ?: defaults.slotStartedAtEpochMs,
            endAtEpochMs = this[Keys.END_AT_EPOCH_MS] ?: defaults.endAtEpochMs,
            endAtElapsedRealtimeMs = this[Keys.END_AT_ELAPSED_REALTIME_MS]
                ?: defaults.endAtElapsedRealtimeMs,
            bootEpochMs = this[Keys.BOOT_EPOCH_MS] ?: defaults.bootEpochMs,
            remainingAtPauseMs = this[Keys.REMAINING_AT_PAUSE_MS] ?: defaults.remainingAtPauseMs,
            lastActivityEpochMs = this[Keys.LAST_ACTIVITY_EPOCH_MS] ?: defaults.lastActivityEpochMs,
        )
    }

    private fun MutablePreferences.put(state: TimerState) {
        this[Keys.STATUS] = state.status.name
        this[Keys.SLOT_TYPE] = state.slotType.name
        this[Keys.SESSION_ID] = state.sessionId
        this[Keys.SLOT_INDEX] = state.slotIndex
        this[Keys.COMPLETED_FOCUS_IN_CYCLE] = state.completedFocusInCycle
        this[Keys.POMODOROS_PER_CYCLE] = state.pomodorosPerCycle
        this[Keys.SLOT_DURATION_MS] = state.slotDurationMs
        this[Keys.SLOT_STARTED_AT_EPOCH_MS] = state.slotStartedAtEpochMs
        this[Keys.END_AT_EPOCH_MS] = state.endAtEpochMs
        this[Keys.END_AT_ELAPSED_REALTIME_MS] = state.endAtElapsedRealtimeMs
        this[Keys.BOOT_EPOCH_MS] = state.bootEpochMs
        this[Keys.REMAINING_AT_PAUSE_MS] = state.remainingAtPauseMs
        this[Keys.LAST_ACTIVITY_EPOCH_MS] = state.lastActivityEpochMs
    }

    /** Preference keys are a storage contract shared with the widget and the service. */
    private object Keys {
        val STATUS = stringPreferencesKey("status")
        val SLOT_TYPE = stringPreferencesKey("slot_type")
        val SESSION_ID = longPreferencesKey("session_id")
        val SLOT_INDEX = intPreferencesKey("slot_index")
        val COMPLETED_FOCUS_IN_CYCLE = intPreferencesKey("completed_focus_in_cycle")
        val POMODOROS_PER_CYCLE = intPreferencesKey("pomodoros_per_cycle")
        val SLOT_DURATION_MS = longPreferencesKey("slot_duration_ms")
        val SLOT_STARTED_AT_EPOCH_MS = longPreferencesKey("slot_started_at_epoch_ms")
        val END_AT_EPOCH_MS = longPreferencesKey("end_at_epoch_ms")
        val END_AT_ELAPSED_REALTIME_MS = longPreferencesKey("end_at_elapsed_realtime_ms")
        val BOOT_EPOCH_MS = longPreferencesKey("boot_epoch_ms")
        val REMAINING_AT_PAUSE_MS = longPreferencesKey("remaining_at_pause_ms")
        val LAST_ACTIVITY_EPOCH_MS = longPreferencesKey("last_activity_epoch_ms")
    }
}
