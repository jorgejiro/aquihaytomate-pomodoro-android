package com.jjrapps.aquihaytomate.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.jjrapps.aquihaytomate.di.TimerStatePreferences
import javax.inject.Inject
import javax.inject.Singleton

/** What a tap on the widget turned out to mean. */
enum class TapKind {
    /** First tap inside the window: toggle now, and be ready to undo it. */
    SINGLE,

    /** Second tap inside the window: undo the toggle and reset instead. */
    DOUBLE,
}

/**
 * Resolves single versus double taps on the widget.
 *
 * **The timestamp is persisted, not held in memory.** The process can die between the two taps — it very
 * often does, since a widget tap may be the only thing keeping it alive — and an instance field would be
 * gone by the second one. It lives in the timer state DataStore, which is already open on this path.
 *
 * The single tap is applied *immediately* rather than waiting out the window: 400 ms of latency on a
 * button is felt, and feels like the widget is slow. If a second tap arrives in time, the toggle is undone
 * and the reset happens instead. See ADR 001, points 4 and 5.
 */
@Singleton
class TapGate @Inject constructor(
    @param:TimerStatePreferences private val dataStore: DataStore<Preferences>,
) {

    /**
     * Records this tap and says what it is.
     *
     * @param nowElapsedRealtimeMs monotonic, so a clock change cannot turn two taps into a double one.
     */
    suspend fun register(nowElapsedRealtimeMs: Long): TapKind {
        var kind = TapKind.SINGLE
        dataStore.edit { preferences ->
            val previous = preferences[LAST_TAP_KEY] ?: 0L
            val elapsedSincePrevious = nowElapsedRealtimeMs - previous
            kind = if (previous > 0L && elapsedSincePrevious in 0..DOUBLE_TAP_WINDOW_MS) {
                TapKind.DOUBLE
            } else {
                TapKind.SINGLE
            }
            // A double tap closes the window, so three quick taps are a double plus a single rather than
            // two overlapping doubles.
            preferences[LAST_TAP_KEY] = if (kind == TapKind.DOUBLE) 0L else nowElapsedRealtimeMs
        }
        return kind
    }

    companion object {
        /**
         * An estimate, to be calibrated on a real device during F8. If it produces false positives or
         * misses, this is the first number to touch. See §13 of CLAUDE.md.
         */
        const val DOUBLE_TAP_WINDOW_MS = 400L

        private val LAST_TAP_KEY = longPreferencesKey("widget_last_tap_elapsed_ms")
    }
}
