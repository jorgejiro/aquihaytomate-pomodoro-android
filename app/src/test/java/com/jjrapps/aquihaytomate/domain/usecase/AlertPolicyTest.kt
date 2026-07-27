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
        dndSuppressesAlarms: Boolean = false,
    ) = AlertPolicy.decide(sound, vibrationSeconds, ringerMode, dndSuppressesAlarms)

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

    @Test
    fun `do not disturb blocking alarms wins over everything`() {
        RingerMode.entries.forEach { mode ->
            assertEquals(
                "ringer $mode should be silenced by DND",
                AlertDecision.NOTHING,
                decide(ringerMode = mode, dndSuppressesAlarms = true),
            )
        }
    }

    @Test
    fun `a settings snapshot decides the same way as the raw values`() {
        val settings = TimerSettings(alertSound = AlertSound.BOWL, vibrationSeconds = 12)

        assertEquals(
            decide(sound = AlertSound.BOWL, vibrationSeconds = 12),
            AlertPolicy.decide(settings, RingerMode.NORMAL, dndSuppressesAlarms = false),
        )
    }

    @Test
    fun `an out of range vibration value cannot enable vibration`() {
        assertFalse(decide(vibrationSeconds = -4).vibrate)
    }
}
