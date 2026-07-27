package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.TimerSettings

/**
 * Builds the end-of-slot vibration waveform.
 *
 * **Always finite, never `repeat >= 0`.** A repeating `VibrationEffect` whose `cancel()` is lost
 * because the process died leaves the phone buzzing until the user reboots it. The pattern is
 * therefore materialised in full, up to the configured number of seconds, and simply ends.
 */
object VibrationPatterns {

    const val PULSE_MS = 400L
    const val GAP_MS = 250L

    /**
     * `[0, pulse, gap, pulse, …]` — the first entry is the initial delay, which is always zero, and
     * the rest alternate on/off as `Vibrator.vibrate` expects.
     *
     * The total is clipped to [seconds]: the last pulse is shortened rather than overshooting, and a
     * trailing gap is never emitted. Returns an empty array when vibration is off, which callers
     * treat as "do not vibrate".
     */
    fun waveform(
        seconds: Int,
        pulseMs: Long = PULSE_MS,
        gapMs: Long = GAP_MS,
    ): LongArray {
        val budget = seconds.coerceIn(TimerSettings.VIBRATION_SECONDS_RANGE) * 1000L
        if (budget <= 0L || pulseMs <= 0L) return LongArray(0)

        val timings = mutableListOf(0L)
        var spent = 0L
        while (spent < budget) {
            val pulse = minOf(pulseMs, budget - spent)
            timings += pulse
            spent += pulse
            if (spent >= budget) break

            val gap = minOf(gapMs, budget - spent)
            // A trailing gap would only pad the effect with silence; stop instead.
            if (gap <= 0L || spent + gap >= budget) break
            timings += gap
            spent += gap
        }
        return timings.toLongArray()
    }

    /** Total wall time the waveform occupies, useful for bounding a wakelock. */
    fun durationMs(waveform: LongArray): Long = waveform.sum()
}
