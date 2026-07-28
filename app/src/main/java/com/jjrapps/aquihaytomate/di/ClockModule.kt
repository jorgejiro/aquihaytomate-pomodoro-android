package com.jjrapps.aquihaytomate.di

import android.os.SystemClock
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

/**
 * The two clocks the engine reads. Everything time-dependent takes them injected rather than calling
 * `System.currentTimeMillis()` or `SystemClock.elapsedRealtime()` directly, so tests can pin them.
 */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {

    /** System default zone, deliberately: a pomodoro belongs to the day the user is living. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.system(ZoneId.systemDefault())

    @Provides
    @Singleton
    fun provideElapsedRealtimeSource() = ElapsedRealtimeSource { SystemClock.elapsedRealtime() }
}
