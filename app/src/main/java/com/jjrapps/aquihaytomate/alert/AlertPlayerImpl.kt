package com.jjrapps.aquihaytomate.alert

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.repository.AlertPlayer
import com.jjrapps.aquihaytomate.domain.usecase.AlertPolicy
import com.jjrapps.aquihaytomate.domain.usecase.InterruptionFilter
import com.jjrapps.aquihaytomate.domain.usecase.RingerMode
import com.jjrapps.aquihaytomate.domain.usecase.VibrationPatterns
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Plays the end-of-slot alert. See docs/decisions/004-alerta-propia-en-vez-de-sonido-de-canal.md.
 *
 * Two things here are not obvious:
 *
 * - **`USAGE_ALARM`, not `USAGE_NOTIFICATION`.** It makes the clip follow the *alarm* volume, which is
 *   what a user expects of a timer, and it gets through Do Not Disturb configurations where alarms are
 *   allowed. A pomodoro the user started on purpose behaves like an alarm.
 * - **The waveform is always finite.** A repeating `VibrationEffect` whose `cancel()` is lost because
 *   the process died leaves the phone buzzing until reboot. There are apps on Play that have shipped
 *   that bug.
 */
@Singleton
class AlertPlayerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AlertPlayer {

    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)
    private val notificationManager: NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    private val mutex = Mutex()
    private var player: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null

    override suspend fun play(sound: AlertSound, vibrationSeconds: Int) {
        val decision = AlertPolicy.decide(
            sound = sound,
            vibrationSeconds = vibrationSeconds,
            ringerMode = currentRingerMode(),
            interruptionFilter = currentInterruptionFilter(),
            dndAllowsAlarms = dndAllowsAlarms(),
            alarmVolumeLevel = alarmVolumeLevel(),
        )
        Timber.d("Alert decision: %s for %s / %ds", decision, sound.id, vibrationSeconds)

        if (decision.vibrate) vibrate(vibrationSeconds)
        if (decision.playSound) mutex.withLock { startSound(sound) }
    }

    override fun stop() {
        releasePlayer()
        vibrator()?.cancel()
    }

    // ─── Sound ──────────────────────────────────────────────────────────────

    private fun startSound(sound: AlertSound) {
        val rawRes = SoundCatalog.rawResFor(sound) ?: return
        releasePlayer()

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Transient-may-duck: the user's music dips instead of being cut off.
        requestAudioFocus(attributes)

        try {
            player = MediaPlayer.create(context, rawRes)?.apply {
                setAudioAttributes(attributes)
                setOnCompletionListener { releasePlayer() }
                setOnErrorListener { _, what, extra ->
                    Timber.w("MediaPlayer error %d/%d", what, extra)
                    releasePlayer()
                    true
                }
                start()
            }
        } catch (e: Exception) {
            // A missing or corrupt clip must never take the timer down with it.
            Timber.e(e, "Could not play the alert sound %s", sound.id)
            releasePlayer()
        }
    }

    private fun requestAudioFocus(attributes: AudioAttributes) {
        val manager = audioManager ?: return
        val request = AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .build()
        focusRequest = request
        runCatching { manager.requestAudioFocus(request) }
    }

    private fun releasePlayer() {
        player?.let { current ->
            runCatching {
                if (current.isPlaying) current.stop()
            }
            runCatching { current.release() }
        }
        player = null

        focusRequest?.let { request ->
            runCatching { audioManager?.abandonAudioFocusRequest(request) }
        }
        focusRequest = null
    }

    // ─── Vibration ──────────────────────────────────────────────────────────

    private fun vibrate(seconds: Int) {
        val vibrator = vibrator() ?: return
        if (!vibrator.hasVibrator()) return

        val timings = VibrationPatterns.waveform(seconds)
        if (timings.isEmpty()) return

        // repeat = -1: play once and stop. Never a repeating pattern.
        val effect = VibrationEffect.createWaveform(timings, -1)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect)
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not vibrate")
        }
    }

    private fun vibrator(): Vibrator? =
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator

    // ─── Device state ───────────────────────────────────────────────────────

    private fun currentRingerMode(): RingerMode = when (audioManager?.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> RingerMode.SILENT
        AudioManager.RINGER_MODE_VIBRATE -> RingerMode.VIBRATE
        else -> RingerMode.NORMAL
    }

    private fun currentInterruptionFilter(): InterruptionFilter =
        when (notificationManager?.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_NONE -> InterruptionFilter.NONE
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> InterruptionFilter.PRIORITY
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> InterruptionFilter.ALARMS
            else -> InterruptionFilter.ALL
        }

    /**
     * Whether the user's Do Not Disturb policy lets alarms through. Reading the policy needs the
     * notification policy access grant, which we do not ask for, so the fallback is to assume alarms are
     * allowed — which is the Android default and matches what an alarm clock would do.
     */
    private fun dndAllowsAlarms(): Boolean {
        val manager = notificationManager ?: return true
        if (!manager.isNotificationPolicyAccessGranted) return true
        return runCatching {
            manager.notificationPolicy.priorityCategories and
                NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS != 0
        }.getOrDefault(true)
    }

    private fun alarmVolumeLevel(): Int =
        audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 1
}
