package com.jjrapps.aquihaytomate.domain.repository

import com.jjrapps.aquihaytomate.domain.model.AlertSound

/**
 * Plays the end-of-slot alert. Behind an interface because the alert is the app's job rather than the
 * notification channel's — see docs/decisions/004-alerta-propia-en-vez-de-sonido-de-canal.md — and
 * because it lets the engine be finished and tested in F3 while the real player lands in F5.
 */
interface AlertPlayer {

    /**
     * Fires the alert, honouring the ringer and Do Not Disturb via `AlertPolicy`.
     *
     * @param vibrationSeconds 0 means no vibration. The waveform is always finite; a repeating one
     *   whose `cancel()` is lost with the process would buzz until reboot.
     * @param repeats how many times the clip plays back to back, 1..10. Returns as soon as the first
     *   play starts: the chain runs on the player's completion callback, so a ten-times alert does not
     *   hold up whoever fired it. [stop] cuts the chain wherever it is.
     */
    suspend fun play(sound: AlertSound, vibrationSeconds: Int, repeats: Int = 1)

    /** Stops anything still sounding, e.g. because the user dismissed the alert. */
    fun stop()
}
