package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings

/** The device's ringer, mapped off `AudioManager` so this file stays free of Android. */
enum class RingerMode { SILENT, VIBRATE, NORMAL }

/** Do Not Disturb, mapped off `NotificationManager.getCurrentInterruptionFilter()`. */
enum class InterruptionFilter {
    /** Nothing is being filtered. */
    ALL,

    /** Priority only: whether alarms get through depends on the user's DND policy. */
    PRIORITY,

    /** Alarms only — a mode that exists precisely to let timers like this one through. */
    ALARMS,

    /** Total silence. Nothing gets through, and that includes us. */
    NONE,
}

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
 * docs/decisions/004-alerta-propia-en-vez-de-sonido-de-canal.md — which means honouring silent mode and
 * Do Not Disturb is our job, not the framework's. Putting the whole decision in one pure function is
 * what keeps that responsibility from leaking into the player.
 */
object AlertPolicy {

    /**
     * @param dndAllowsAlarms whether the user's Do Not Disturb policy lets the alarm category through.
     *   Only consulted under [InterruptionFilter.PRIORITY].
     * @param alarmVolumeLevel current volume of the alarm stream. We play through `USAGE_ALARM`, so at
     *   zero there is literally nothing to hear and the vibration has to carry the alert on its own.
     *
     * The rules, in order of precedence:
     * - **Total DND wins over everything.** The user asked for absolute quiet.
     * - **Silent mode means silent**, vibration included. A phone face-down on a meeting table buzzing
     *   for five seconds is exactly what silent mode exists to prevent.
     * - **Priority DND that blocks alarms** silences us, the same way it silences an alarm clock.
     * - **Vibrate mode** drops the sound and keeps the buzz, whatever sound was chosen.
     * - **Alarm volume at zero** drops the sound too, and again vibration carries it.
     * - Otherwise the user's own two settings decide.
     */
    fun decide(
        sound: AlertSound,
        vibrationSeconds: Int,
        ringerMode: RingerMode,
        interruptionFilter: InterruptionFilter = InterruptionFilter.ALL,
        dndAllowsAlarms: Boolean = true,
        alarmVolumeLevel: Int = 1,
    ): AlertDecision {
        val wantsVibration = vibrationSeconds.coerceIn(TimerSettings.VIBRATION_SECONDS_RANGE) > 0

        if (interruptionFilter == InterruptionFilter.NONE) return AlertDecision.NOTHING
        if (interruptionFilter == InterruptionFilter.PRIORITY && !dndAllowsAlarms) {
            return AlertDecision.NOTHING
        }
        if (ringerMode == RingerMode.SILENT) return AlertDecision.NOTHING

        val audible = sound.isAudible &&
            ringerMode == RingerMode.NORMAL &&
            alarmVolumeLevel > 0

        return AlertDecision(playSound = audible, vibrate = wantsVibration)
    }

    /**
     * Igual, pero tomando los ajustes y el slot que **acaba**, que es lo que decide cuál de los dos
     * sonidos suena. Sin el slot no se puede: desde que hay uno por fase, unos ajustes solos no
     * determinan el sonido.
     */
    fun decide(
        settings: TimerSettings,
        finishedType: SlotType,
        ringerMode: RingerMode,
        interruptionFilter: InterruptionFilter = InterruptionFilter.ALL,
        dndAllowsAlarms: Boolean = true,
        alarmVolumeLevel: Int = 1,
    ): AlertDecision = decide(
        sound = settings.alertSoundFor(finishedType),
        vibrationSeconds = settings.vibrationSeconds,
        ringerMode = ringerMode,
        interruptionFilter = interruptionFilter,
        dndAllowsAlarms = dndAllowsAlarms,
        alarmVolumeLevel = alarmVolumeLevel,
    )
}
