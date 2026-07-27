package com.jjrapps.aquihaytomate.domain.model

/**
 * Where the engine is in its state machine: `IDLE → RUNNING ⇄ PAUSED → RINGING → …`
 *
 * Persisted by name in the timer state DataStore, so the constants are a storage contract.
 */
enum class TimerStatus {
    /** Nothing is counting. The pending slot is shown full. */
    IDLE,

    /** Counting down towards the deadline. This is the only status where the service lives. */
    RUNNING,

    /** Stopped mid-slot, with the remaining time frozen in `remainingAtPauseMs`. */
    PAUSED,

    /** The slot ran out and the user has not moved on to the next one yet. */
    RINGING,
    ;

    /** True while a slot is under way, whether the clock is moving or not. */
    val isActive: Boolean get() = this == RUNNING || this == PAUSED

    companion object {
        fun fromName(name: String?): TimerStatus =
            entries.firstOrNull { it.name == name } ?: IDLE
    }
}
