package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertPolicyTest {

    private fun decide(
        sound: AlertSound = AlertSound.BELL,
        vibrationSeconds: Int = 5,
        ringerMode: RingerMode = RingerMode.NORMAL,
        interruptionFilter: InterruptionFilter = InterruptionFilter.ALL,
        dndAllowsAlarms: Boolean = true,
        alarmVolumeLevel: Int = 7,
    ) = AlertPolicy.decide(
        sound = sound,
        vibrationSeconds = vibrationSeconds,
        ringerMode = ringerMode,
        interruptionFilter = interruptionFilter,
        dndAllowsAlarms = dndAllowsAlarms,
        alarmVolumeLevel = alarmVolumeLevel,
    )

    @Test
    fun `normal mode plays both by default`() {
        val decision = decide()

        assertTrue(decision.playSound)
        assertTrue(decision.vibrate)
    }

    @Test
    fun `the silent sound choice still vibrates`() {
        val decision = decide(sound = AlertSound.SILENT)

        assertFalse(decision.playSound)
        assertTrue(decision.vibrate)
    }

    @Test
    fun `zero vibration seconds means no vibration`() {
        val decision = decide(vibrationSeconds = 0)

        assertTrue(decision.playSound)
        assertFalse(decision.vibrate)
    }

    // Silent mode means silent, buzzing included.
    @Test
    fun `silent ringer suppresses sound and vibration`() {
        assertEquals(AlertDecision.NOTHING, decide(ringerMode = RingerMode.SILENT))
    }

    @Test
    fun `vibrate ringer drops the sound and keeps the buzz`() {
        val decision = decide(ringerMode = RingerMode.VIBRATE)

        assertFalse(decision.playSound)
        assertTrue(decision.vibrate)
    }

    @Test
    fun `vibrate ringer still honours vibration being turned off`() {
        val decision = decide(ringerMode = RingerMode.VIBRATE, vibrationSeconds = 0)

        assertTrue(decision.isSilent)
    }

    // Total DND means total.
    @Test
    fun `total do not disturb wins over everything`() {
        RingerMode.entries.forEach { mode ->
            assertEquals(
                "ringer $mode should be silenced by total DND",
                AlertDecision.NOTHING,
                decide(ringerMode = mode, interruptionFilter = InterruptionFilter.NONE),
            )
        }
    }

    @Test
    fun `priority do not disturb that blocks alarms silences the alert`() {
        assertEquals(
            AlertDecision.NOTHING,
            decide(
                interruptionFilter = InterruptionFilter.PRIORITY,
                dndAllowsAlarms = false,
            ),
        )
    }

    @Test
    fun `priority do not disturb that allows alarms lets us through`() {
        val decision = decide(
            interruptionFilter = InterruptionFilter.PRIORITY,
            dndAllowsAlarms = true,
        )

        assertTrue(decision.playSound)
        assertTrue(decision.vibrate)
    }

    // The alarms-only mode exists precisely to let timers like this one through.
    @Test
    fun `the alarms only filter lets us through`() {
        val decision = decide(interruptionFilter = InterruptionFilter.ALARMS)

        assertTrue(decision.playSound)
        assertTrue(decision.vibrate)
    }

    // We play through USAGE_ALARM, so at zero there is nothing to hear.
    @Test
    fun `alarm volume at zero leaves only the vibration`() {
        val decision = decide(alarmVolumeLevel = 0)

        assertFalse(decision.playSound)
        assertTrue(decision.vibrate)
    }

    @Test
    fun `alarm volume at zero with vibration off is fully silent`() {
        assertTrue(decide(alarmVolumeLevel = 0, vibrationSeconds = 0).isSilent)
    }

    @Test
    fun `a settings snapshot decides the same way as the raw values`() {
        val settings = TimerSettings(alertSound = AlertSound.BOWL, vibrationSeconds = 12)

        assertEquals(
            decide(sound = AlertSound.BOWL, vibrationSeconds = 12),
            AlertPolicy.decide(
                settings = settings,
                ringerMode = RingerMode.NORMAL,
                alarmVolumeLevel = 7,
            ),
        )
    }

    @Test
    fun `an out of range vibration value cannot enable vibration`() {
        assertFalse(decide(vibrationSeconds = -4).vibrate)
    }
}
