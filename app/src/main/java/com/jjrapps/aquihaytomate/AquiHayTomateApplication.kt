package com.jjrapps.aquihaytomate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AquiHayTomateApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        createNotificationChannels()
    }

    /**
     * Both channels are deliberately silent: sound and vibration are played by [AlertPlayer] so
     * that they stay configurable at runtime, which a channel does not allow once created.
     * See docs/decisions/004-alerta-propia-en-vez-de-sonido-de-canal.md.
     */
    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val running = NotificationChannel(
            CHANNEL_TIMER_RUNNING,
            getString(R.string.channel_timer_running_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.channel_timer_running_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        val alerts = NotificationChannel(
            CHANNEL_TIMER_ALERTS,
            getString(R.string.channel_timer_alerts_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.channel_timer_alerts_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }

        manager.createNotificationChannels(listOf(running, alerts))
    }

    companion object {
        const val CHANNEL_TIMER_RUNNING = "timer_running"
        const val CHANNEL_TIMER_ALERTS = "timer_alerts"
    }
}
