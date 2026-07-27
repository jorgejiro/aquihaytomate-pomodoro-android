package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.TimerSettings

/** The device's ringer, mapped off `AudioManager` so this file stays free of Android. */
enum class RingerMode { SILENT, VIBRATE, NORMAL }

/** What the player is actually allowed to do once the ringer and Do Not Disturb have their say. */
data class AlertDecision(
    val playSound: Boolean,
    val vibrate: Boolean,
) {
    val isSilent: Boolean get() = !playSound && !vibrate

    companion object {
        val NOTHING = AlertDecision(playSound = false, vibrate = false)
    }
}

/**
 * Decides whether the end-of-slot alert may make noise, vibrate, both or neither.
 *
 * The alert is played by the app rather than by the notification channel — see
 * docs/decisions/004-alerta-propia-en-vez-de-sonido-de-canal.md — which means honouring silent mode
 * and Do Not Disturb is our job, not the framework's.
 */
object AlertPolicy {

    /**
     * @param dndSuppressesAlarms true when the current Do Not Disturb policy filters out the alarm
     *   category. We play through `USAGE_ALARM`, so ordinary DND lets us through; only a policy that
     *   explicitly blocks alarms silences us.
     *
     * The rules, in order:
     * - DND blocking alarms wins over everything, including vibration. The user asked for quiet.
     * - Silent mode means silent: no sound, and no vibration either. A phone face-down on a meeting
     *   table buzzing for five seconds is exactly what silent mode exists to prevent.
     * - Vibrate mode drops the sound and keeps the buzz, even if the chosen sound is audible.
     * - Otherwise both are governed by the user's own settings.
     */
    fun decide(
        sound: AlertSound,
        vibrationSeconds: Int,
        ringerMode: RingerMode,
        dndSuppressesAlarms: Boolean,
    ): AlertDecision {
        if (dndSuppressesAlarms) return AlertDecision.NOTHING

        val wantsSound = sound.isAudible
        val wantsVibration = vibrationSeconds.coerceIn(TimerSettings.VIBRATION_SECONDS_RANGE) > 0

        return when (ringerMode) {
            RingerMode.SILENT -> AlertDecision.NOTHING
            RingerMode.VIBRATE -> AlertDecision(playSound = false, vibrate = wantsVibration)
            RingerMode.NORMAL -> AlertDecision(playSound = wantsSound, vibrate = wantsVibration)
        }
    }

    fun decide(
        settings: TimerSettings,
        ringerMode: RingerMode,
        dndSuppressesAlarms: Boolean,
    ): AlertDecision = decide(
        sound = settings.alertSound,
        vibrationSeconds = settings.vibrationSeconds,
        ringerMode = ringerMode,
        dndSuppressesAlarms = dndSuppressesAlarms,
    )
}
