package com.jjrapps.aquihaytomate.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Two separate DataStore files, with distinct qualifiers.
 *
 * They are split because their write patterns have nothing in common: settings change a handful of
 * times in the life of the install, while the timer state changes on every transition and is read by
 * the widget and the service. One file would mean every settings collector waking up on every timer
 * transition.
 *
 * Both use a corruption handler that falls back to empty preferences. A corrupted file would
 * otherwise throw on first read and crash the app on launch, and the contents are reconstructible:
 * settings go back to defaults and the timer reconciles to idle.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @SettingsPreferences
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = create(context, SETTINGS_FILE_NAME, dispatcher)

    @Provides
    @Singleton
    @TimerStatePreferences
    fun provideTimerStateDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher dispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = create(context, TIMER_STATE_FILE_NAME, dispatcher)

    private fun create(
        context: Context,
        name: String,
        dispatcher: CoroutineDispatcher,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(dispatcher + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(name) },
    )

    /** File names are a storage contract: renaming one wipes that store on the next update. */
    const val SETTINGS_FILE_NAME = "settings"
    const val TIMER_STATE_FILE_NAME = "timer_state"
}
