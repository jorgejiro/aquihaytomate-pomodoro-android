package com.jjrapps.aquihaytomate.timer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.jjrapps.aquihaytomate.AquiHayTomateApplication
import com.jjrapps.aquihaytomate.MainActivity
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.usecase.PlannedSlot
import com.jjrapps.aquihaytomate.domain.usecase.TimerMath
import com.jjrapps.aquihaytomate.ui.common.phaseNameRes
import com.jjrapps.aquihaytomate.ui.theme.TomateFill
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the two notifications. See docs/design-spec.md §8.
 *
 * **The countdown is drawn by SystemUI, not by us.** `setWhen(endAt)` plus `setUsesChronometer(true)`
 * plus `setChronometerCountDown(true)` makes the system tick the figure in its own process, so a
 * pomodoro costs about four `notify()` calls instead of 1500. No `setProgress()` either, for the same
 * reason: it would force a repaint a second.
 */
@Singleton
class TimerNotificationFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * The foreground notification of a running slot.
     *
     * @param nextSlot what follows this one, for the second line. Optional because the service publishes
     *   a placeholder in the first line of `onStartCommand`, before it has read anything off disk.
     */
    fun ongoingRunning(state: TimerState, nextSlot: PlannedSlot? = null): Notification =
        base(AquiHayTomateApplication.CHANNEL_TIMER_RUNNING)
            .setContentTitle(titleFor(state))
            .setContentText(nextUpText(nextSlot))
            .setOngoing(true)
            // The system draws the countdown itself from this deadline.
            .setWhen(state.endAtEpochMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            // Without IMMEDIATE, Android 12+ holds the notification back for up to ten seconds and the
            // user thinks the timer never started.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .withTimerActions(pauseOrResume = TimerActionReceiver.ACTION_PAUSE)
            .build()

    /**
     * A paused slot. No chronometer — the clock is stopped, so the remaining time is baked into the
     * text and stays there until something changes.
     */
    fun ongoingPaused(state: TimerState, remainingMs: Long): Notification =
        base(AquiHayTomateApplication.CHANNEL_TIMER_RUNNING)
            .setContentTitle(titleFor(state))
            .setContentText(
                context.getString(
                    R.string.notification_paused,
                    TimerMath.formatRemaining(remainingMs),
                ),
            )
            .setOngoing(true)
            .setUsesChronometer(false)
            .setShowWhen(false)
            .withTimerActions(pauseOrResume = TimerActionReceiver.ACTION_RESUME)
            .build()

    /**
     * The same three actions as the timer screen, in the same order: hold or release the clock, throw this
     * slot back to its start, move on to the next one.
     *
     * Three is the most a notification shows, and having the set be identical in both states means the
     * button under the finger does not move when the timer is paused from the shade.
     */
    private fun NotificationCompat.Builder.withTimerActions(
        pauseOrResume: String,
    ): NotificationCompat.Builder = this
        .addAction(0, context.getString(labelFor(pauseOrResume)), actionIntent(pauseOrResume))
        .addAction(
            0,
            context.getString(R.string.notification_action_reset),
            actionIntent(TimerActionReceiver.ACTION_RESET),
        )
        .addAction(
            0,
            context.getString(R.string.notification_action_skip),
            actionIntent(TimerActionReceiver.ACTION_SKIP),
        )

    private fun labelFor(action: String) = if (action == TimerActionReceiver.ACTION_RESUME) {
        R.string.notification_action_resume
    } else {
        R.string.notification_action_pause
    }

    /** `A continuación: Descanso · 5 min`, from the same string and the same planner as the screen. */
    private fun nextUpText(nextSlot: PlannedSlot?): String? {
        if (nextSlot == null) return null
        val minutes = (nextSlot.durationMs / TimerSettings.MINUTE_MS).toInt()

        return context.getString(
            R.string.next_up,
            context.getString(phaseNameRes(nextSlot.type)),
            context.resources.getQuantityString(R.plurals.settings_minutes, minutes, minutes),
        )
    }

    /**
     * The end-of-slot alert. [state] already describes the slot coming up, so what just finished is
     * implied by it.
     */
    fun slotFinished(state: TimerState): Notification {
        val nextIsBreak = state.slotType.isBreak
        // The length of the slot that just ended is not in the state any more — the snapshot has moved
        // on to the next one — and reading it back off Settings would lie whenever the user had changed
        // it mid-run. The cycle position is what the user actually wants to see here anyway.
        val body = if (nextIsBreak) {
            context.getString(
                R.string.notification_focus_finished,
                state.cyclePosition,
                state.pomodorosPerCycle,
            )
        } else {
            context.getString(R.string.notification_break_finished)
        }
        val actionLabel = if (nextIsBreak) {
            R.string.control_start_break
        } else {
            R.string.control_back_to_work
        }

        return base(AquiHayTomateApplication.CHANNEL_TIMER_ALERTS)
            .setContentTitle(context.getString(R.string.phase_ringing))
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                0,
                context.getString(actionLabel),
                actionIntent(TimerActionReceiver.ACTION_START_NEXT),
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_dismiss),
                actionIntent(TimerActionReceiver.ACTION_DISMISS),
            )
            .build()
    }

    private fun base(channelId: String) = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notif_tomate)
        .setColor(TomateFill.toArgb())
        .setContentIntent(openAppIntent())
        // Both channels are mute by design; the alert is played by AlertPlayer. See ADR 004.
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setShowWhen(false)

    private fun titleFor(state: TimerState): String = when (state.slotType) {
        SlotType.FOCUS -> context.getString(
            R.string.notification_title_focus,
            state.cyclePosition,
            state.pomodorosPerCycle,
        )

        SlotType.SHORT_BREAK -> context.getString(R.string.phase_short_break)
        SlotType.LONG_BREAK -> context.getString(R.string.phase_long_break)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN_APP,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun actionIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, TimerActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val NOTIFICATION_ID_ONGOING = 1
        const val NOTIFICATION_ID_ALERT = 2
        private const val REQUEST_OPEN_APP = 2001
    }
}
