package com.jjrapps.aquihaytomate.timer

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import com.jjrapps.aquihaytomate.domain.repository.TimerServiceController
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class TimerServiceControllerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TimerServiceController {

    /**
     * **Every `startForegroundService` goes through this try/catch.** Android 12+ forbids starting a
     * foreground service from the background unless an exemption applies, and the legitimate ones here
     * are: from the Activity, from a notification action, from a widget tap and from an exact alarm.
     * When there is no exemption we degrade to "persisted state plus backup alarm", which is level 3 of
     * the four in ADR 002 and still delivers the slot.
     */
    override fun start(): Boolean = try {
        context.startForegroundService(PomodoroTimerService.intent(context))
        true
    } catch (e: ForegroundServiceStartNotAllowedException) {
        Timber.w(e, "Foreground start refused; falling back to state plus alarm")
        false
    } catch (e: IllegalStateException) {
        // Some OEM builds throw the plain superclass instead of the documented subclass.
        Timber.w(e, "Foreground start refused; falling back to state plus alarm")
        false
    }

    override fun stop() {
        runCatching { context.stopService(PomodoroTimerService.intent(context)) }
            .onFailure { Timber.w(it, "Could not stop the timer service") }
    }
}
