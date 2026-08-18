package com.jjrapps.aquihaytomate.timer

import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerState
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import java.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The shape of the ongoing notification.
 *
 * Its layout is not something a unit test can see and not something the compiler checks, and it carries two
 * requirements that came straight from using the app: the countdown has to be the biggest thing in it — so
 * it lives in a body view of ours rather than in the system's timestamp slot — and the three actions have to
 * be the same three in the same order whether the timer runs or is paused, so the button under the finger
 * does not move. See docs/decisions/009-*.
 */
@RunWith(AndroidJUnit4::class)
class TimerNotificationFactoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val factory = TimerNotificationFactory(
        context = context,
        clock = Clock.systemUTC(),
        elapsedRealtime = { SystemClock.elapsedRealtime() },
    )

    private val running = TimerState(
        status = TimerStatus.RUNNING,
        slotType = SlotType.FOCUS,
        completedFocusInCycle = 1,
        slotDurationMs = 25 * 60_000L,
        endAtElapsedRealtimeMs = SystemClock.elapsedRealtime() + 20 * 60_000L,
        endAtEpochMs = System.currentTimeMillis() + 20 * 60_000L,
    )

    @Test
    fun theRunningNotificationCarriesItsOwnBody() {
        val notification = factory.ongoingRunning(running)

        assertNotNull(
            "Without a custom body the figure goes back to the 11 sp timestamp slot",
            notification.contentView,
        )
        assertNotNull("The expanded form needs it too, or it falls back", notification.bigContentView)
    }

    /**
     * The collapsed form gets the layout with the icon controls, the expanded one the layout without them.
     *
     * Not interchangeable: the collapsed body is capped at 48 dp and gets no system action row, so it has
     * to carry its own controls; the expanded body would then show them twice. Getting these two the wrong
     * way round is invisible until the shade is open.
     */
    @Test
    fun eachFormGetsItsOwnLayout() {
        val notification = factory.ongoingRunning(running)

        assertEquals(
            R.layout.notification_timer_collapsed,
            notification.contentView.layoutId,
        )
        assertEquals(R.layout.notification_timer, notification.bigContentView.layoutId)
    }

    /** The line that used to say «A continuación: Descanso · 5 min» was dropped on purpose. */
    @Test
    fun theRunningNotificationHasNoUpNextLine() {
        val notification = factory.ongoingRunning(running)

        assertNull(
            notification.extras.getCharSequence(NotificationCompat.EXTRA_TEXT),
        )
    }

    /** Nothing shows it, but it is the fallback for surfaces that refuse custom views, and TalkBack reads it. */
    @Test
    fun theTitleStaysSetAsAFallback() {
        val title = factory.ongoingRunning(running)
            .extras
            .getCharSequence(NotificationCompat.EXTRA_TITLE)
            ?.toString()

        assertNotNull(title)
        assertTrue("Expected the phase and the cycle position, got: $title", title!!.contains("2/4"))
    }

    @Test
    fun bothStatesOfferTheSameThreeActions() {
        val whileRunning = factory.ongoingRunning(running)
        val whilePaused = factory.ongoingPaused(
            running.copy(status = TimerStatus.PAUSED, remainingAtPauseMs = 12 * 60_000L),
            remainingMs = 12 * 60_000L,
        )

        assertEquals(3, whileRunning.actions.size)
        assertEquals(3, whilePaused.actions.size)

        val pauseLabel = whileRunning.actions[0].title.toString()
        val resumeLabel = whilePaused.actions[0].title.toString()
        assertTrue("First action should toggle the clock", pauseLabel != resumeLabel)
        // The other two must not move between states.
        assertEquals(whileRunning.actions[1].title, whilePaused.actions[1].title)
        assertEquals(whileRunning.actions[2].title, whilePaused.actions[2].title)
    }

    /**
     * Both ongoing forms carry a delete intent, which is what brings them back after a swipe.
     *
     * `setOngoing(true)` stopped being enough in Android 13: the user can dismiss the notification of a
     * foreground service and the service carries on regardless.
     */
    @Test
    fun bothOngoingFormsComeBackIfDismissed() {
        assertNotNull(factory.ongoingRunning(running).deleteIntent)
        assertNotNull(
            factory.ongoingPaused(running.copy(status = TimerStatus.PAUSED), 60_000L).deleteIntent,
        )
    }

    /**
     * The alert is meant to be dismissable — swiping it away is how the user acknowledges it — and doing so
     * has to stop the alarm, which can be vibrating for up to 30 seconds. Its delete intent points at
     * `ACTION_DISMISS`, which stops the sound; it does **not** republish, and `RestoreOngoingNotificationUseCase`
     * refuses to restore anything while ringing.
     */
    @Test
    fun dismissingTheAlertStopsTheAlarm() {
        val alert = factory.slotFinished(running.copy(slotType = SlotType.SHORT_BREAK))

        assertNotNull("Dismissing from a watch would leave the phone buzzing", alert.deleteIntent)
        assertTrue(alert.flags and android.app.Notification.FLAG_AUTO_CANCEL != 0)
    }

    /**
     * Only the alert is handed to a paired watch.
     *
     * The countdown is republished on every transition and a permanent entry in a watch's notification list
     * is noise, so both ongoing forms are `localOnly`. The alert is not, and it is also the only one that
     * stays "alerting" — `setSilent(true)` would drop it out of what a watch is handed. The channel does the
     * muting, so this costs no sound. See ADR 011.
     */
    @Test
    fun onlyTheAlertReachesAPairedWatch() {
        val alert = factory.slotFinished(running.copy(slotType = SlotType.SHORT_BREAK))
        val whileRunning = factory.ongoingRunning(running)
        val whilePaused = factory.ongoingPaused(running.copy(status = TimerStatus.PAUSED), 60_000L)

        assertEquals(
            "The alert is the whole point of pairing a watch with this app",
            0,
            alert.flags and NotificationCompat.FLAG_LOCAL_ONLY,
        )
        assertNull("The channel does the muting; the builder must not", alert.sound)

        assertTrue(
            "A permanent countdown on the wrist is noise",
            whileRunning.flags and NotificationCompat.FLAG_LOCAL_ONLY != 0,
        )
        assertTrue(whilePaused.flags and NotificationCompat.FLAG_LOCAL_ONLY != 0)
    }

    /**
     * The phase travels in the header's `subText`, not in the collapsed body.
     *
     * In the collapsed body there is no width for it: on One UI it was cut to «En…», and that form does not
     * even draw the header. The figure's colour carries the phase there, and the text is still in the
     * subText and the title, which is what a screen reader reads.
     */
    @Test
    fun thePhaseTravelsInTheSubText() {
        val subText = factory.ongoingRunning(running)
            .extras
            .getCharSequence(NotificationCompat.EXTRA_SUB_TEXT)
            ?.toString()

        assertNotNull("Without this the phase is nowhere to be read", subText)
        assertTrue("Expected the phase and the cycle, got: $subText", subText!!.contains("2/4"))
    }

    /**
     * Every view in the bodies has to be one RemoteViews is allowed to inflate.
     *
     * A `Space` used as spacer got as far as running: `RemoteViews` refuses it with «Class not allowed to be
     * inflated», the foreground service notification then fails to inflate, and the system kills the app
     * with `BadForegroundServiceNotificationException`. Inflating both bodies here is the cheapest way to
     * catch that before a device does.
     */
    @Test
    fun bothBodiesInflate() {
        val notification = factory.ongoingRunning(running)

        assertNotNull(notification.contentView.apply { apply(context, null) })
        assertNotNull(notification.bigContentView.apply { apply(context, null) })
    }

    /** Two actions on the wrist: start the next slot, or dismiss. */
    @Test
    fun theAlertOffersStartNextAndDismiss() {
        val alert = factory.slotFinished(running.copy(slotType = SlotType.SHORT_BREAK))

        assertEquals(2, alert.actions.size)
        assertEquals(
            context.getString(R.string.control_start_break),
            alert.actions[0].title.toString(),
        )
        assertEquals(
            context.getString(R.string.notification_action_dismiss),
            alert.actions[1].title.toString(),
        )
    }

    /**
     * The copy of the alert, which is the only thing a paired watch shows: no app name, no icon of ours.
     *
     * It used to read «¡Tiempo!» over «Se acabó el descanso», which on a wrist says neither what ended nor
     * what is waiting. The title now names what was completed — with the cycle position, the one thing the
     * state cannot imply once it has moved on — and the body names the next slot and its length.
     */
    @Test
    fun theAlertSaysWhatEndedAndWhatIsNext() {
        // A break carries the position of the pomodoro just finished, which here is the first one.
        val afterFocus = factory.slotFinished(running.copy(slotType = SlotType.SHORT_BREAK))
        val minutes = 25

        assertEquals(
            context.getString(R.string.notification_focus_done_title, 1, 4),
            afterFocus.extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString(),
        )
        assertEquals(
            context.getString(
                R.string.notification_next_waiting,
                context.getString(
                    R.string.notification_slot_of,
                    context.getString(R.string.phase_short_break),
                    context.resources.getQuantityString(R.plurals.settings_minutes, minutes, minutes),
                ),
            ),
            afterFocus.extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString(),
        )
    }

    /**
     * A slot that started by itself still gets an alert, and says so.
     *
     * Auto-starting means not having to tap, not being kept in the dark: with the phone in another room, this
     * notification is the only way the wrist hears that a pomodoro ended. It offers skipping rather than
     * starting, because there is nothing left to start, and expires on its own.
     */
    @Test
    fun theChainedAlertOffersSkipAndExpires() {
        val chained = factory.slotFinished(running.copy(slotType = SlotType.SHORT_BREAK), chained = true)

        assertEquals(2, chained.actions.size)
        assertEquals(
            context.getString(R.string.notification_action_skip),
            chained.actions[0].title.toString(),
        )
        assertTrue("It must not pile up on the wrist", chained.timeoutAfter > 0)
    }

    @Test
    fun theWaitingAlertOffersStartingTheNextSlot() {
        val waiting = factory.slotFinished(running.copy(slotType = SlotType.SHORT_BREAK))

        assertEquals(
            context.getString(R.string.control_start_break),
            waiting.actions[0].title.toString(),
        )
        assertEquals("Nothing to expire: it is waiting for the user", 0L, waiting.timeoutAfter)
    }

    // ─── The stopped slot ───────────────────────────────────────────────────

    private val idleFocus = running.copy(
        status = TimerStatus.IDLE,
        sessionId = 1_800_000_000_000L,
        endAtElapsedRealtimeMs = 0L,
        endAtEpochMs = 0L,
    )

    /**
     * Resetting from the shade left the shade empty: the timer was still there, back at its start, with no
     * way to run it again short of opening the app. It now keeps a notification with the whole slot and a ▸.
     */
    @Test
    fun theIdleNotificationCarriesItsOwnBodyAndBothLayouts() {
        val notification = factory.ongoingIdle(idleFocus, durationMs = 25 * 60_000L)

        assertEquals(R.layout.notification_timer_collapsed, notification.contentView.layoutId)
        assertEquals(R.layout.notification_timer, notification.bigContentView.layoutId)
        assertNotNull(notification.contentView.apply { apply(context, null) })
        assertNotNull(notification.bigContentView.apply { apply(context, null) })
    }

    /**
     * Dismissable, and it stays dismissed.
     *
     * With the clock stopped there is no countdown running unseen, so a swipe is a legitimate "not now" —
     * the opposite of the running form, which comes back precisely because something is still ticking.
     */
    @Test
    fun theIdleNotificationIsDismissableAndDoesNotComeBack() {
        val notification = factory.ongoingIdle(idleFocus, durationMs = 25 * 60_000L)

        assertEquals(0, notification.flags and android.app.Notification.FLAG_ONGOING_EVENT)
        assertNull("Restoring an idle timer would be the notification that will not die", notification.deleteIntent)
    }

    /** A pomodoro waiting has one thing to offer; a break waiting can also be skipped, as on the screen. */
    @Test
    fun theIdleNotificationOffersStartingAndSkipsOnlyBreaks() {
        val focus = factory.ongoingIdle(idleFocus, durationMs = 25 * 60_000L)
        val breakWaiting = factory.ongoingIdle(
            idleFocus.copy(slotType = SlotType.SHORT_BREAK),
            durationMs = 5 * 60_000L,
        )

        assertEquals(1, focus.actions.size)
        assertEquals(context.getString(R.string.control_start), focus.actions[0].title.toString())

        assertEquals(2, breakWaiting.actions.size)
        assertEquals(
            context.getString(R.string.notification_action_skip),
            breakWaiting.actions[1].title.toString(),
        )
    }

    @Test
    fun theIdleNotificationIsSilentAndPhoneOnly() {
        val notification = factory.ongoingIdle(idleFocus, durationMs = 25 * 60_000L)

        assertNull(notification.sound)
        assertTrue(notification.flags and NotificationCompat.FLAG_LOCAL_ONLY != 0)
    }

    @Test
    fun theRunningNotificationIsOngoingAndSilent() {
        val notification = factory.ongoingRunning(running)

        assertTrue(notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
        assertNull("Both channels are mute by design; AlertPlayer does the sound", notification.sound)
    }
}
