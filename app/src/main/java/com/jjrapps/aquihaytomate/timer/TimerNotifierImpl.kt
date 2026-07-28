package com.jjrapps.aquihaytomate.timer

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.repository.TimerNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Publishes the notifications for the states where no service is alive.
 *
 * Every `notify` is guarded: without `POST_NOTIFICATIONS` the call throws, and **the timer has to keep
 * working when the permission was denied** (checklist §10.6). A silently missing notification is a far
 * better outcome than a crash.
 */
@Singleton
class TimerNotifierImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val factory: TimerNotificationFactory,
) : TimerNotifier {

    private val manager = NotificationManagerCompat.from(context)

    /**
     * Covers both cases in one call, on every API level: the runtime permission denied on 13+, and the
     * user switching notifications off from the system settings.
     */
    private val canPost: Boolean
        get() = manager.areNotificationsEnabled()

    override fun showRunning(state: TimerState) {
        notify(TimerNotificationFactory.NOTIFICATION_ID_ONGOING) { factory.ongoingRunning(state) }
    }

    override fun showPaused(state: TimerState, remainingMs: Long) {
        notify(
            TimerNotificationFactory.NOTIFICATION_ID_ONGOING,
        ) { factory.ongoingPaused(state, remainingMs) }
    }

    override fun showSlotFinished(state: TimerState) {
        notify(TimerNotificationFactory.NOTIFICATION_ID_ALERT) { factory.slotFinished(state) }
    }

    override fun clearOngoing() {
        manager.cancel(TimerNotificationFactory.NOTIFICATION_ID_ONGOING)
    }

    override fun clearAlert() {
        manager.cancel(TimerNotificationFactory.NOTIFICATION_ID_ALERT)
    }

    private inline fun notify(id: Int, build: () -> android.app.Notification) {
        if (!canPost) return
        try {
            manager.notify(id, build())
        } catch (e: SecurityException) {
            // The permission can be revoked between the check and the call.
            Timber.w(e, "Could not post notification %d", id)
        }
    }
}
