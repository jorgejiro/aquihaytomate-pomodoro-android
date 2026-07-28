package com.jjrapps.aquihaytomate.data.repository

import com.jjrapps.aquihaytomate.data.local.datastore.SettingsDataSource
import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.AppLanguage
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.WidgetBackground
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataSource: SettingsDataSource,
) : SettingsRepository {

    override val settings: Flow<TimerSettings> = dataSource.settings

    override suspend fun current(): TimerSettings = dataSource.current()

    override suspend fun setDurationMinutes(type: SlotType, minutes: Int) =
        dataSource.setDurationMinutes(type, minutes)

    override suspend fun setPomodorosPerCycle(count: Int) = dataSource.setPomodorosPerCycle(count)

    override suspend fun setAutoStartBreak(enabled: Boolean) =
        dataSource.setAutoStartBreak(enabled)

    override suspend fun setAutoStartFocus(enabled: Boolean) =
        dataSource.setAutoStartFocus(enabled)

    override suspend fun setAlertSound(sound: AlertSound) = dataSource.setAlertSound(sound)

    override suspend fun setVibrationSeconds(seconds: Int) =
        dataSource.setVibrationSeconds(seconds)

    override suspend fun setKeepScreenOn(enabled: Boolean) = dataSource.setKeepScreenOn(enabled)

    override suspend fun setDailyGoal(pomodoros: Int) = dataSource.setDailyGoal(pomodoros)

    override suspend fun setWidgetBackground(background: WidgetBackground) =
        dataSource.setWidgetBackground(background)

    override suspend fun setLanguage(language: AppLanguage) = dataSource.setLanguage(language)

    override suspend fun setLiquidAnimationEnabled(enabled: Boolean) =
        dataSource.setLiquidAnimationEnabled(enabled)

    override suspend fun setOnboardingDone(done: Boolean) = dataSource.setOnboardingDone(done)
}
