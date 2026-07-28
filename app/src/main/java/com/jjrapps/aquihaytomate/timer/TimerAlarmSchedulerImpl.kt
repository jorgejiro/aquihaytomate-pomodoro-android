package com.jjrapps.aquihaytomate.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jjrapps.aquihaytomate.domain.repository.TimerAlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class TimerAlarmSchedulerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TimerAlarmScheduler {

    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExactAlarms(): Boolean =
        alarmManager?.canScheduleExactAlarms() == true

    /**
     * `ELAPSED_REALTIME_WAKEUP` rather than an RTC alarm: the monotonic clock cannot be moved by the
     * user, so changing the system time does not retime the slot.
     *
     * With the permission granted, `setExactAndAllowWhileIdle` fires to the second even in Doze. Without
     * it we fall back to `setAndAllowWhileIdle`, which the system may delay — that is the fourth and
     * least precise degradation level in ADR 002, and Settings warns about it.
     */
    override fun arm(deadlineElapsedRealtimeMs: Long): Boolean {
        val manager = alarmManager ?: return false
        val pending = pendingIntent() ?: return false
        val exact = canScheduleExactAlarms()

        return try {
            if (exact) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    deadlineElapsedRealtimeMs,
                    pending,
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    deadlineElapsedRealtimeMs,
                    pending,
                )
            }
            exact
        } catch (e: SecurityException) {
            // The permission can be revoked between the check and the call.
            Timber.w(e, "Could not arm the backup alarm")
            false
        }
    }

    override fun cancel() {
        val pending = pendingIntent() ?: return
        alarmManager?.cancel(pending)
    }

    /**
     * A single alarm slot: the same request code every time, so arming twice replaces the previous
     * alarm instead of stacking a second one.
     */
    private fun pendingIntent(): PendingIntent? {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarmReceiver.ACTION_SLOT_DEADLINE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val REQUEST_CODE = 1001
    }
}
