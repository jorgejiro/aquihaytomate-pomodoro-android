package com.jjrapps.aquihaytomate.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Locale

/**
 * The locale the statistics roll up against.
 *
 * Injected for the same reason `Clock` is: `WeekFields.of(locale)` decides whether a week starts on
 * Monday or Sunday, and a test that cannot pin that cannot check the weekly aggregation at all.
 *
 * Deliberately **not** cached in a `@Singleton`: `Locale.getDefault()` changes when the user switches
 * language from Settings, and a captured value would keep rolling weeks up the old way until the process
 * restarted.
 */
@Module
@InstallIn(SingletonComponent::class)
object LocaleModule {

    @Provides
    fun provideLocale(): Locale = Locale.getDefault()
}
