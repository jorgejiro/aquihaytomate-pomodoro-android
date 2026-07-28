package com.jjrapps.aquihaytomate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.model.WidgetBackground
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import com.jjrapps.aquihaytomate.domain.usecase.TimerMath
import com.jjrapps.aquihaytomate.ui.theme.DoradoBright
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Paints the widget from the persisted state.
 *
 * Callable from anywhere — `onUpdate`, the application-wide state collector, a tap — so there is exactly
 * one place that knows how the widget looks.
 *
 * **The seconds are not painted here.** While the timer runs, a `Chronometer` counts down inside the
 * launcher's process, so a 25 minute pomodoro costs one update instead of 1500. See
 * docs/decisions/001-widget-con-remoteviews-y-chronometer.md.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val timerStateRepository: TimerStateRepository,
    private val settingsRepository: SettingsRepository,
    private val renderer: TomatoBitmapRenderer,
    private val clock: Clock,
    private val elapsedRealtime: ElapsedRealtimeSource,
) {

    suspend fun updateAll() {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, PomodoroWidgetProvider::class.java))
        if (ids.isEmpty()) return

        val state = timerStateRepository.current()
        val settings = settingsRepository.current()
        val views = buildViews(state, settings.widgetBackground, ringingHighlight = false)

        runCatching { manager.updateAppWidget(ids, views) }
            .onFailure { Timber.w(it, "Could not update the widget") }
    }

    /**
     * One frame of the ringing blink. The widget cannot animate itself, so the alternation is driven from
     * outside — the only case where the widget is refreshed on a timer, and it is capped. See ADR 001.
     */
    suspend fun updateAllRingingFrame(highlighted: Boolean) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, PomodoroWidgetProvider::class.java))
        if (ids.isEmpty()) return

        val state = timerStateRepository.current()
        if (state.status != TimerStatus.RINGING) return
        val settings = settingsRepository.current()

        runCatching {
            manager.updateAppWidget(
                ids,
                buildViews(state, settings.widgetBackground, ringingHighlight = highlighted),
            )
        }.onFailure { Timber.w(it, "Could not blink the widget") }
    }

    private fun buildViews(
        state: TimerState,
        background: WidgetBackground,
        ringingHighlight: Boolean,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_tomate)
        val settingsDuration = state.slotDurationMs
        val remainingMs =
            TimerMath.remainingMs(state, clock.millis(), elapsedRealtime.millis())
        val fillFraction = when (state.status) {
            TimerStatus.IDLE -> 1f
            TimerStatus.RINGING -> 0f
            else -> TimerMath.fillFraction(settingsDuration, remainingMs)
        }

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            when {
                background == WidgetBackground.TRANSPARENT -> R.drawable.widget_plate_transparent
                else -> R.drawable.widget_plate
            },
        )

        // The glyph names the action a tap performs, exactly like the primary control of the app: play when
        // the clock is stopped, pause while it runs. It is what makes a 40 dp square read as a button
        // rather than as a readout.
        val glyphLarge = state.status == TimerStatus.IDLE
        views.setImageViewBitmap(
            R.id.widget_tomato,
            renderer.render(
                slotType = state.slotType,
                fillFraction = fillFraction,
                dimmed = state.status == TimerStatus.PAUSED,
                showCalyx = state.slotType.isBreak,
                glyph = glyphFor(state.status),
                glyphLarge = glyphLarge,
            ),
        )

        applyReadout(views, state, remainingMs, ringingHighlight)
        views.setOnClickPendingIntent(R.id.widget_root, tapIntent())
        return views
    }

    /**
     * What a tap does from each state. Pausing and resuming are the *action*, not the state: a paused timer
     * is already saying so with its liquid at 45%, so the glyph is free to say "resume" instead of
     * repeating "paused". See docs/design-spec.md §7.3.
     */
    private fun glyphFor(status: TimerStatus): WidgetGlyph = when (status) {
        TimerStatus.RUNNING -> WidgetGlyph.PAUSE
        TimerStatus.IDLE, TimerStatus.PAUSED, TimerStatus.RINGING -> WidgetGlyph.PLAY
    }

    /**
     * Picks between the self-ticking chronometer and a static figure.
     *
     * A `Chronometer` cannot be frozen at an arbitrary value, so every state other than `RUNNING` uses the
     * plain `TextView`: the remaining time when paused, the alert mark when ringing, and nothing at all
     * when idle — there the large play glyph on the tomato is the whole readout.
     */
    private fun applyReadout(
        views: RemoteViews,
        state: TimerState,
        remainingMs: Long,
        ringingHighlight: Boolean,
    ) {
        when (state.status) {
            TimerStatus.RUNNING -> {
                views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
                views.setViewVisibility(R.id.widget_static_text, View.GONE)
                // The base is a point in the future on the monotonic clock; SystemUI counts down to it
                // on its own, without ever waking this process.
                views.setChronometer(
                    R.id.widget_chronometer,
                    SystemClock.elapsedRealtime() + remainingMs,
                    null,
                    true,
                )
                views.setChronometerCountDown(R.id.widget_chronometer, true)
                views.setTextColor(R.id.widget_chronometer, TextPrimary.toArgb())
            }

            TimerStatus.PAUSED ->
                staticReadout(views, TimerMath.formatRemaining(remainingMs), TextPrimary.toArgb())

            // No figure at all: a stopped timer has no time to report, and the duration it *would* run for
            // is already one tap away. The play glyph gets the whole tomato to itself.
            TimerStatus.IDLE -> staticReadout(views, "", TextPrimary.toArgb())

            TimerStatus.RINGING -> staticReadout(
                views,
                context.getString(R.string.widget_ringing_glyph),
                if (ringingHighlight) DoradoBright.toArgb() else TextPrimary.toArgb(),
            )
        }
    }

    private fun staticReadout(views: RemoteViews, text: String, color: Int) {
        views.setViewVisibility(R.id.widget_chronometer, View.GONE)
        views.setViewVisibility(R.id.widget_static_text, View.VISIBLE)
        views.setTextViewText(R.id.widget_static_text, text)
        views.setTextColor(R.id.widget_static_text, color)
    }

    private fun tapIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_TAP,
        Intent(context, WidgetTapReceiver::class.java).setAction(WidgetTapReceiver.ACTION_TAP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_TAP = 3001
    }
}
