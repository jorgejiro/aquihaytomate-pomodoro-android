package com.jjrapps.aquihaytomate.di

import com.jjrapps.aquihaytomate.alert.AlertPlayerImpl
import com.jjrapps.aquihaytomate.data.repository.SettingsRepositoryImpl
import com.jjrapps.aquihaytomate.data.repository.StatsRepositoryImpl
import com.jjrapps.aquihaytomate.data.repository.TimerStateRepositoryImpl
import com.jjrapps.aquihaytomate.data.system.ChargingMonitorImpl
import com.jjrapps.aquihaytomate.domain.repository.AlertPlayer
import com.jjrapps.aquihaytomate.domain.repository.ChargingMonitor
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.StatsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerAlarmScheduler
import com.jjrapps.aquihaytomate.domain.repository.TimerNotifier
import com.jjrapps.aquihaytomate.domain.repository.TimerServiceController
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import com.jjrapps.aquihaytomate.timer.TimerAlarmSchedulerImpl
import com.jjrapps.aquihaytomate.timer.TimerNotifierImpl
import com.jjrapps.aquihaytomate.timer.TimerServiceControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindChargingMonitor(impl: ChargingMonitorImpl): ChargingMonitor

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    @Singleton
    abstract fun bindTimerStateRepository(impl: TimerStateRepositoryImpl): TimerStateRepository

    @Binds
    @Singleton
    abstract fun bindAlertPlayer(impl: AlertPlayerImpl): AlertPlayer

    @Binds
    @Singleton
    abstract fun bindTimerAlarmScheduler(impl: TimerAlarmSchedulerImpl): TimerAlarmScheduler

    /**
     * Swapping this one binding for a no-op is how the app would ship without a foreground service if
     * Play ever rejected the `specialUse` type. See ADR 002.
     */
    @Binds
    @Singleton
    abstract fun bindTimerServiceController(
        impl: TimerServiceControllerImpl,
    ): TimerServiceController

    @Binds
    @Singleton
    abstract fun bindTimerNotifier(impl: TimerNotifierImpl): TimerNotifier
}
