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
        val views = buildViews(state, settings.widgetBackground)

        runCatching { manager.updateAppWidget(ids, views) }
            .onFailure { Timber.w(it, "Could not update the widget") }
    }

    private fun buildViews(state: TimerState, background: WidgetBackground): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_tomate)
        val remainingMs = TimerMath.remainingMs(state, clock.millis(), elapsedRealtime.millis())
        // What each state shows is decided by a pure function, so it can be tested without a launcher.
        val readout = readoutFor(state, remainingMs)

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            when {
                background == WidgetBackground.TRANSPARENT -> R.drawable.widget_plate_transparent
                else -> R.drawable.widget_plate
            },
        )

        views.setImageViewBitmap(
            R.id.widget_tomato,
            renderer.render(
                slotType = state.slotType,
                fillFraction = readout.fillFraction,
                dimmed = readout.dimmed,
                glyph = readout.glyph,
                glyphLarge = readout.glyphLarge,
            ),
        )

        applyFigure(views, readout)
        views.setOnClickPendingIntent(R.id.widget_root, tapIntent())
        return views
    }

    /**
     * Puts the figure on screen: the self-ticking chronometer while the slot runs, a frozen `TextView`
     * otherwise, and neither when there is nothing to report.
     *
     * A `Chronometer` cannot be stopped at an arbitrary value, which is the whole reason there are two views
     * stacked in the layout.
     */
    private fun applyFigure(views: RemoteViews, readout: WidgetReadout) {
        val ticking = readout.tickingRemainingMs
        if (ticking != null) {
            views.setViewVisibility(R.id.widget_chronometer, View.VISIBLE)
            views.setViewVisibility(R.id.widget_static_text, View.GONE)
            // The base is a point in the future on the monotonic clock; SystemUI counts down to it on its
            // own, without ever waking this process.
            views.setChronometer(
                R.id.widget_chronometer,
                SystemClock.elapsedRealtime() + ticking,
                null,
                true,
            )
            views.setChronometerCountDown(R.id.widget_chronometer, true)
            views.setTextColor(R.id.widget_chronometer, TextPrimary.toArgb())
            return
        }

        staticReadout(
            views,
            readout.figureMs?.let { TimerMath.formatRemaining(it) } ?: "",
            TextPrimary.toArgb(),
        )
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
