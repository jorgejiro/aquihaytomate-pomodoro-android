package com.jjrapps.aquihaytomate.domain.repository

import com.jjrapps.aquihaytomate.domain.model.TimerState

/**
 * Publishes and clears the timer notifications.
 *
 * Behind an interface so that the use case which decides *when* there should be a notification can be
 * tested without a `NotificationManager`. The service publishes its own foreground notification
 * directly, since the framework demands it synchronously in `onStartCommand`; this covers the states
 * where no service is alive — paused and ringing.
 */
interface TimerNotifier {

    /**
      * Publishes the ongoing notification for a running slot.
      *
      * Normally the service does this itself through `startForeground`; this exists for the case where the
      * user swiped the notification away and it has to come back without disturbing the service.
      */
     fun showRunning(state: TimerState)

    /** Publishes the ongoing notification for a paused slot, with a Resume action. */
    fun showPaused(state: TimerState, remainingMs: Long)

    /** Publishes the end-of-slot alert. [state] already describes the slot coming up. */
    fun showSlotFinished(state: TimerState)

    /** Clears the ongoing notification. The alert is dismissed by the user or auto-cancelled. */
    fun clearOngoing()

    fun clearAlert()
}
