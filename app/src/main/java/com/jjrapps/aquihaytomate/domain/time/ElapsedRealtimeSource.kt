package com.jjrapps.aquihaytomate.domain.time

/**
 * The monotonic counterpart of the injected `java.time.Clock`: `SystemClock.elapsedRealtime()` behind
 * an interface.
 *
 * `Clock` covers wall-clock time, but the engine also needs a reading that no user can move and that
 * keeps advancing while the device is suspended. Both are injected for the same reason — a timer whose
 * arithmetic can only be exercised on a device is a timer with untested arithmetic.
 */
fun interface ElapsedRealtimeSource {

    /** Milliseconds since boot, including deep sleep. */
    fun millis(): Long
}
