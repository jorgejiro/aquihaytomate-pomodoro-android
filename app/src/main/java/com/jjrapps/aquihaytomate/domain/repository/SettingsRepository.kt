package com.jjrapps.aquihaytomate.domain.repository

import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.AppLanguage
import com.jjrapps.aquihaytomate.domain.model.KeepScreenOnMode
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.WidgetBackground
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val settings: Flow<TimerSettings>

    /** One-shot read, for the entrypoints that cannot collect: receivers, the service, the widget. */
    suspend fun current(): TimerSettings

    suspend fun setDurationMinutes(type: SlotType, minutes: Int)

    suspend fun setPomodorosPerCycle(count: Int)

    suspend fun setAutoStartBreak(enabled: Boolean)

    suspend fun setAutoStartFocus(enabled: Boolean)

    suspend fun setFocusAlertSound(sound: AlertSound)

    suspend fun setBreakAlertSound(sound: AlertSound)

    /** How many times the pomodoro's end sound plays back to back, 1..10. */
    suspend fun setFocusAlertRepeats(count: Int)

    /** The same for the break's end sound, and set independently. */
    suspend fun setBreakAlertRepeats(count: Int)

    suspend fun setVibrationSeconds(seconds: Int)

    suspend fun setKeepScreenOn(mode: KeepScreenOnMode)

    suspend fun setDailyGoal(pomodoros: Int)

    suspend fun setWidgetBackground(background: WidgetBackground)

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setLiquidAnimationEnabled(enabled: Boolean)

    suspend fun setOnboardingDone(done: Boolean)
}
