package com.jjrapps.aquihaytomate.domain.repository

import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.AppLanguage
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

    suspend fun setAutoStartNext(enabled: Boolean)

    suspend fun setAlertSound(sound: AlertSound)

    suspend fun setVibrationSeconds(seconds: Int)

    suspend fun setKeepScreenOn(enabled: Boolean)

    suspend fun setDailyGoal(pomodoros: Int)

    suspend fun setWidgetBackground(background: WidgetBackground)

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setLiquidAnimationEnabled(enabled: Boolean)

    suspend fun setOnboardingDone(done: Boolean)
}
