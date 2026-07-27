package com.jjrapps.aquihaytomate.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.jjrapps.aquihaytomate.di.ApplicationScope
import com.jjrapps.aquihaytomate.domain.usecase.ReconcileTimerUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The 1×1 widget: a classic [AppWidgetProvider] with `RemoteViews`, not Glance.
 *
 * The deciding factor is the `Chronometer`, which only `RemoteViews` can reach: it ticks inside the
 * launcher's process, so the countdown costs zero wake-ups of ours. Glance would mean `updateAll()` once a
 * minute — around 25 process wake-ups per pomodoro — plus recomposition, serialisation and IPC each time.
 * See docs/decisions/001-widget-con-remoteviews-y-chronometer.md.
 *
 * This class stays thin on purpose: the painting lives in [WidgetUpdater], which the state collector also
 * calls, so a widget added to the home screen and a widget refreshed by a transition cannot look different.
 */
@AndroidEntryPoint
class PomodoroWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var widgetUpdater: WidgetUpdater

    @Inject lateinit var reconcileTimer: ReconcileTimerUseCase

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // The application scope, not a receiver-local one: the update reads DataStore, and the receiver
        // returns long before that finishes.
        scope.launch {
            try {
                // A widget appearing is a good moment to check the state is not stale, which it will be if
                // the process was killed while a slot was running.
                reconcileTimer()
                widgetUpdater.updateAll()
            } catch (e: Exception) {
                Timber.e(e, "Could not update the widget")
            }
        }
    }

    /**
     * Also fires when the launcher is restarted or the widget is resized, which is exactly when a widget
     * that draws itself from a bitmap would otherwise come back blank.
     */
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        scope.launch {
            runCatching { widgetUpdater.updateAll() }
                .onFailure { Timber.w(it, "Could not repaint the widget after an options change") }
        }
    }
}
