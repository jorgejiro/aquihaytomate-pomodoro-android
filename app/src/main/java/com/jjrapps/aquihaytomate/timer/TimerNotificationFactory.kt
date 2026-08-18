package com.jjrapps.aquihaytomate.timer

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import com.jjrapps.aquihaytomate.AquiHayTomateApplication
import com.jjrapps.aquihaytomate.MainActivity
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import com.jjrapps.aquihaytomate.domain.usecase.TimerMath
import com.jjrapps.aquihaytomate.ui.common.phaseNameRes
import com.jjrapps.aquihaytomate.ui.theme.TomateFill
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the two notifications. See docs/design-spec.md §8.
 *
 * **The countdown is ticked by SystemUI, not by us.** The figure lives in a `Chronometer` inside our own
 * body view, counting down in the system's process, so a pomodoro still costs about four `notify()` calls
 * instead of 1500. No `setProgress()` either, for the same reason: it would force a repaint a second.
 */
@Singleton
class TimerNotificationFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    /** The foreground notification of a running slot. */
    fun ongoingRunning(state: TimerState): Notification =
        base(AquiHayTomateApplication.CHANNEL_TIMER_RUNNING)
            .setContentTitle(titleFor(state))
            .setSubText(titleFor(state))
            .setOngoing(true)
            .asPhoneOnly()
            .withRestoreOnDismissal()
            .withBody(state, TimerActionReceiver.ACTION_PAUSE) { chronometerOf(state) }
            // Without IMMEDIATE, Android 12+ holds the notification back for up to ten seconds and the
            // user thinks the timer never started.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .withTimerActions(pauseOrResume = TimerActionReceiver.ACTION_PAUSE)
            .build()

    /**
     * A paused slot. No chronometer — the clock is stopped, so the remaining time is baked into the
     * figure and stays there until something changes.
     */
    fun ongoingPaused(state: TimerState, remainingMs: Long): Notification =
        base(AquiHayTomateApplication.CHANNEL_TIMER_RUNNING)
            .setContentTitle(titleFor(state))
            .setSubText(expandedPhaseText(state, R.string.notification_paused_label))
            .setOngoing(true)
            .asPhoneOnly()
            .withRestoreOnDismissal()
            .withBody(state, TimerActionReceiver.ACTION_RESUME, R.string.notification_paused_label) {
                frozenFigureOf(state, remainingMs)
            }
            .withTimerActions(pauseOrResume = TimerActionReceiver.ACTION_RESUME)
            .build()

    /**
     * A stopped slot waiting to be started: the full length of what comes next and a single ▸.
     *
     * It exists because resetting from the shade used to leave the shade empty — the timer was still
     * there, at zero progress, with no way to start it again without opening the app. The figure is
     * frozen like the paused one, for the same reason: nothing is ticking.
     *
     * **Not `setOngoing`, and no restore on dismissal.** With the clock stopped, swiping it away is a
     * legitimate "I am done for now", and `RestoreOngoingNotificationUseCase` deliberately ignores
     * `IDLE`. Reset only ever hides it, never resurrects it.
     *
     * @param durationMs the whole length of the pending slot, read from the live settings by the caller.
     */
    fun ongoingIdle(state: TimerState, durationMs: Long): Notification =
        base(AquiHayTomateApplication.CHANNEL_TIMER_RUNNING)
            .setContentTitle(titleFor(state))
            .setSubText(expandedPhaseText(state, R.string.notification_ready_label))
            .asPhoneOnly()
            .withBody(state, TimerActionReceiver.ACTION_START, R.string.notification_ready_label) {
                frozenFigureOf(state, durationMs)
            }
            .withIdleActions(state)
            .build()

    /**
     * Silent and phone-only, which is what the three ongoing forms have in common.
     *
     * **Silent** because they are republished on every transition and must never make a sound of their own —
     * the alert is `AlertPlayer`'s job, see ADR 004.
     *
     * **Phone-only** because a paired watch has no business holding a permanent countdown in its
     * notification list. It is a deliberate trade: it also gives up pausing from the wrist, and the wrist
     * gets the one notification that matters there — the end-of-slot alert, which is *not* marked either
     * way and carries its two actions. See ADR 011.
     */
    private fun NotificationCompat.Builder.asPhoneOnly(): NotificationCompat.Builder =
        setSilent(true).setLocalOnly(true)

    /**
     * Brings the notification back if the user swipes it away with a slot still under way.
     *
     * `setOngoing(true)` used to make a notification undismissable; from Android 13 it does not, and the
     * countdown would carry on with nothing on screen to prove it and no controls to reach. The delete
     * intent republishes it — `RestoreOngoingNotificationUseCase` decides whether there is anything to
     * restore, so dismissing the end-of-slot alert or an idle timer still works normally.
     */
    private fun NotificationCompat.Builder.withRestoreOnDismissal(): NotificationCompat.Builder =
        setDeleteIntent(actionIntent(TimerActionReceiver.ACTION_ONGOING_DISMISSED))

    /**
     * The countdown as the biggest thing in the notification, with the phase and the cycle position
     * demoted to a line of 13 sp beside it.
     *
     * The standard template gives the figure the timestamp slot — 11 sp, top right, unstyleable — and hands
     * the whole visual weight to the title, which is how `Enfoque · 2/4` ended up shouting over the time
     * left. So the body is ours, and `DecoratedCustomViewStyle` keeps the system's header and action row
     * around it. See docs/decisions/009-*.
     *
     * `setContentTitle` stays set even though nothing shows it: it is the fallback for surfaces that refuse
     * custom views, and it is what a screen reader announces.
     */
    private fun NotificationCompat.Builder.withBody(
        state: TimerState,
        primaryAction: String,
        @StringRes suffixRes: Int? = null,
        applyFigure: RemoteViews.() -> Unit,
    ): NotificationCompat.Builder = setStyle(NotificationCompat.DecoratedCustomViewStyle())
        // Two bodies, because the collapsed one is capped at 48 dp and gets no system action row: there the
        // controls are icons of ours, and expanded they are the system's own labelled row from `addAction`.
        .setCustomContentView(
            // No phase text here at all: with the figure and three controls there is no room for it — in
            // One UI it was cut to "En…" — so the phase lives in the header's subText and the figure's
            // colour carries it in this form. The view stays as the spacer that pushes the controls right.
            body(
                layout = R.layout.notification_timer_collapsed,
                phase = "",
                applyFigure = applyFigure,
            ).apply { applyIconActions(state, primaryAction) },
        )
        .setCustomBigContentView(
            body(
                layout = R.layout.notification_timer,
                phase = expandedPhaseText(state, suffixRes),
                applyFigure = applyFigure,
            ),
        )

    private fun body(
        @LayoutRes layout: Int,
        phase: String,
        applyFigure: RemoteViews.() -> Unit,
    ): RemoteViews = RemoteViews(context.packageName, layout).apply {
        // Empty in the collapsed body, where this view is only a spacer.
        setTextViewText(R.id.notification_phase, phase)
        applyFigure()
    }

    /**
     * El color de la fase: rojo en enfoque, ámbar en descanso.
     *
     * En la forma colapsada es lo único que distingue una fase de otra, porque el texto no cabe — en One UI
     * se cortaba a «En…» y ese estado ni siquiera dibuja el encabezado. No queda como único indicador en
     * absoluto: el `subText` y el `contentTitle` llevan la fase escrita, que es lo que lee un lector de
     * pantalla, y la forma expandida la muestra entera.
     *
     * Los tonos salen de `values/colors.xml` y `values-night/colors.xml`, oscurecidos en tema claro para que
     * el ámbar no baje de 4,5:1 sobre un fondo casi blanco.
     */
    private fun figureColour(slotType: SlotType): Int = context.getColor(
        if (slotType.isBreak) R.color.notification_time_break else R.color.notification_time_focus,
    )

    /** `Enfoque · 2/4`, plus `· Pausado` when the clock is stopped. */
    private fun expandedPhaseText(state: TimerState, @StringRes suffixRes: Int?): String {
        val phase = titleFor(state)
        return if (suffixRes == null) {
            phase
        } else {
            context.getString(R.string.notification_phase_suffix, phase, context.getString(suffixRes))
        }
    }

    /**
     * The three icon controls of the collapsed body, with the same intents as the labelled row.
     *
     * They exist because the system only draws its own row when the notification is expanded, which meant
     * a tap to reach Pause. `contentDescription` carries the label a screen reader would have read off that
     * row.
     */
    private fun RemoteViews.applyIconActions(state: TimerState, primaryAction: String) {
        val tint = figureColour(state.slotType)
        listOf(
            R.id.notification_action_primary,
            R.id.notification_action_reset,
            R.id.notification_action_skip,
        ).forEach { setInt(it, "setColorFilter", tint) }

        val playing = primaryAction == TimerActionReceiver.ACTION_RESUME ||
            primaryAction == TimerActionReceiver.ACTION_START
        setImageViewResource(
            R.id.notification_action_primary,
            if (playing) R.drawable.ic_notif_play else R.drawable.ic_notif_pause,
        )
        setContentDescription(
            R.id.notification_action_primary,
            context.getString(labelFor(primaryAction)),
        )
        setOnClickPendingIntent(R.id.notification_action_primary, actionIntent(primaryAction))

        // The same two rules the Timer screen applies, straight off the state: nothing to throw away
        // when the slot has not started, and no skipping a first pomodoro that has not run.
        setViewVisibility(
            R.id.notification_action_reset,
            if (state.status == TimerStatus.IDLE) View.GONE else View.VISIBLE,
        )
        setContentDescription(
            R.id.notification_action_reset,
            context.getString(R.string.notification_action_reset),
        )
        setOnClickPendingIntent(
            R.id.notification_action_reset,
            actionIntent(TimerActionReceiver.ACTION_RESET),
        )

        setViewVisibility(
            R.id.notification_action_skip,
            if (state.offersSkip) View.VISIBLE else View.GONE,
        )
        setContentDescription(
            R.id.notification_action_skip,
            context.getString(R.string.notification_action_skip),
        )
        setOnClickPendingIntent(
            R.id.notification_action_skip,
            actionIntent(TimerActionReceiver.ACTION_SKIP),
        )
    }

    /** Counted down by SystemUI from a point on the monotonic clock: this process sleeps through the slot. */
    private fun RemoteViews.chronometerOf(state: TimerState) {
        setViewVisibility(R.id.notification_chronometer, View.VISIBLE)
        setViewVisibility(R.id.notification_static_time, View.GONE)
        setChronometer(
            R.id.notification_chronometer,
            SystemClock.elapsedRealtime() + remainingMsOf(state),
            null,
            true,
        )
        setChronometerCountDown(R.id.notification_chronometer, true)
        setTextColor(R.id.notification_chronometer, figureColour(state.slotType))
    }

    private fun RemoteViews.frozenFigureOf(state: TimerState, remainingMs: Long) {
        setViewVisibility(R.id.notification_chronometer, View.GONE)
        setViewVisibility(R.id.notification_static_time, View.VISIBLE)
        setTextViewText(R.id.notification_static_time, TimerMath.formatRemaining(remainingMs))
        setTextColor(R.id.notification_static_time, figureColour(state.slotType))
    }

    /**
     * What is left of the running slot, for the chronometer's base.
     *
     * Derived from the deadline rather than passed in, so the notification cannot disagree with the state
     * it is describing. See `TimerMath`.
     */
    private fun remainingMsOf(state: TimerState): Long =
        TimerMath.remainingMs(state, clock.millis(), elapsedRealtime.millis())

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

    /**
     * The expanded row of a stopped slot: start it, and skip it when what waits is a break.
     *
     * Reset is left out on purpose — the slot is already at its start — and so is a skip of a pomodoro
     * that has not run, which would hand out a break earned by nothing.
     */
    private fun NotificationCompat.Builder.withIdleActions(
        state: TimerState,
    ): NotificationCompat.Builder = this
        .addAction(
            0,
            context.getString(R.string.control_start),
            actionIntent(TimerActionReceiver.ACTION_START),
        )
        .apply {
            if (state.offersSkip) {
                addAction(
                    0,
                    context.getString(R.string.notification_action_skip),
                    actionIntent(TimerActionReceiver.ACTION_SKIP),
                )
            }
        }

    private fun labelFor(action: String) = when (action) {
        TimerActionReceiver.ACTION_RESUME -> R.string.notification_action_resume
        TimerActionReceiver.ACTION_START -> R.string.control_start
        else -> R.string.notification_action_pause
    }

    /**
     * The end-of-slot alert. [state] already describes the slot coming up, so what just finished is
     * implied by it.
     *
     * The copy has to stand on its own, because **a paired watch shows the title and the body and nothing
     * else** — no app name, no icon of ours. `¡Tiempo!` over `Se acabó el descanso` told the wrist neither
     * what had ended nor what came next. Now the title says what was completed and the body says what is
     * waiting, with its length.
     *
     * @param chained true when the next slot started by itself. There is nothing to tap then, so the body
     *   says it is already running and the actions offer skipping it instead of starting it.
     */
    fun slotFinished(state: TimerState, chained: Boolean = false): Notification {
        val nextIsBreak = state.slotType.isBreak
        val nextMinutes = (state.slotDurationMs / TimerSettings.MINUTE_MS).toInt()
        val nextSlot = context.getString(
            R.string.notification_slot_of,
            context.getString(phaseNameRes(state.slotType)),
            context.resources.getQuantityString(R.plurals.settings_minutes, nextMinutes, nextMinutes),
        )

        // The cycle position is the one thing the state cannot imply: the length of the slot that just
        // ended has already been replaced by the next one's.
        val title = if (nextIsBreak) {
            context.getString(
                R.string.notification_focus_done_title,
                state.cyclePosition,
                state.pomodorosPerCycle,
            )
        } else {
            context.getString(R.string.notification_break_done_title)
        }
        val body = context.getString(
            if (chained) R.string.notification_next_running else R.string.notification_next_waiting,
            nextSlot,
        )

        val builder = base(AquiHayTomateApplication.CHANNEL_TIMER_ALERTS)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            // Dismissing it from anywhere — the shade, a watch, "clear all" — has to stop the alarm, which
            // can be vibrating for up to 30 seconds. Without this, a dismissal from the wrist cancelled the
            // notification and left the phone buzzing.
            .setDeleteIntent(actionIntent(TimerActionReceiver.ACTION_DISMISS))

        return if (chained) {
            // Nothing to start, so the useful action is refusing the slot that just began. It also expires
            // on its own: nobody should have to dismiss a notice about something already under way.
            builder
                .setTimeoutAfter(CHAINED_ALERT_TIMEOUT_MS)
                .addAction(
                    0,
                    context.getString(R.string.notification_action_skip),
                    actionIntent(TimerActionReceiver.ACTION_SKIP),
                )
                .addAction(
                    0,
                    context.getString(R.string.notification_action_dismiss),
                    actionIntent(TimerActionReceiver.ACTION_DISMISS),
                )
                .build()
        } else {
            builder
                .addAction(
                    0,
                    context.getString(
                        if (nextIsBreak) R.string.control_start_break else R.string.control_back_to_work,
                    ),
                    actionIntent(TimerActionReceiver.ACTION_START_NEXT),
                )
                .addAction(
                    0,
                    context.getString(R.string.notification_action_dismiss),
                    actionIntent(TimerActionReceiver.ACTION_DISMISS),
                )
                .build()
        }
    }

    private fun base(channelId: String) = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notif_tomate)
        .setColor(TomateFill.toArgb())
        .setContentIntent(openAppIntent())
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
        /** Two minutes: long enough to notice on a wrist, short enough not to pile up. */
        private const val CHAINED_ALERT_TIMEOUT_MS = 2 * 60_000L

        const val NOTIFICATION_ID_ONGOING = 1
        const val NOTIFICATION_ID_ALERT = 2
        private const val REQUEST_OPEN_APP = 2001
    }
}
