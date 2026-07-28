package com.jjrapps.aquihaytomate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.jjrapps.aquihaytomate.widget.WidgetStateCollector
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class AquiHayTomateApplication : Application() {

    /**
     * Injected here because the widget has to keep up with the timer whether or not any Activity is alive.
     * It is the only collector with application scope, and the only place the widget is refreshed from.
     */
    @Inject lateinit var widgetStateCollector: WidgetStateCollector

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        createNotificationChannels()
        widgetStateCollector.start()
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
