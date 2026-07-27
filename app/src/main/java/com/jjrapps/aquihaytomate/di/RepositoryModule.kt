package com.jjrapps.aquihaytomate.di

import com.jjrapps.aquihaytomate.alert.NoOpAlertPlayer
import com.jjrapps.aquihaytomate.data.repository.SettingsRepositoryImpl
import com.jjrapps.aquihaytomate.data.repository.StatsRepositoryImpl
import com.jjrapps.aquihaytomate.data.repository.TimerStateRepositoryImpl
import com.jjrapps.aquihaytomate.domain.repository.AlertPlayer
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.StatsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
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
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    @Singleton
    abstract fun bindTimerStateRepository(impl: TimerStateRepositoryImpl): TimerStateRepository

    /** TODO(F5): swap for `AlertPlayerImpl` once the sounds and the vibrator land. */
    @Binds
    @Singleton
    abstract fun bindAlertPlayer(impl: NoOpAlertPlayer): AlertPlayer
}
