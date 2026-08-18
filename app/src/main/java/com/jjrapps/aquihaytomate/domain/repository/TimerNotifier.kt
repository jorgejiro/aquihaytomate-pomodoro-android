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

    /**
     * Publishes the notification of a stopped slot waiting to be started, with a Start action.
     *
     * @param durationMs the full length of the pending slot, which is what the figure shows.
     */
    fun showIdle(state: TimerState, durationMs: Long)

    /**
     * Publishes the end-of-slot alert. [state] already describes the slot coming up.
     *
     * @param chained true when that slot started by itself. It still gets an alert: auto-starting means not
     *   having to tap, not being kept in the dark — and on a paired watch this is the only way to know a
     *   pomodoro ended when the phone is in another room.
     */
    fun showSlotFinished(state: TimerState, chained: Boolean = false)

    /** Clears the ongoing notification. The alert is dismissed by the user or auto-cancelled. */
    fun clearOngoing()

    fun clearAlert()
}
