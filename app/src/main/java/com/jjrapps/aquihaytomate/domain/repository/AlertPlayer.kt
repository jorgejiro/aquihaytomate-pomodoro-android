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
     */
    suspend fun play(sound: AlertSound, vibrationSeconds: Int)

    /** Stops anything still sounding, e.g. because the user dismissed the alert. */
    fun stop()
}
