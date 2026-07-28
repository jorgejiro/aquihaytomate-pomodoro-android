package com.jjrapps.aquihaytomate.timer

import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    @Test
    fun theRunningNotificationIsOngoingAndSilent() {
        val notification = factory.ongoingRunning(running)

        assertTrue(notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
        assertNull("Both channels are mute by design; AlertPlayer does the sound", notification.sound)
    }
}
