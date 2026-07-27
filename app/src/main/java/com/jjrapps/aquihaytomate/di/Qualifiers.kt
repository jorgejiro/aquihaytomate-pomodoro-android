package com.jjrapps.aquihaytomate.di

import javax.inject.Qualifier

/** The settings DataStore. Two stores exist and both are `DataStore<Preferences>`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsPreferences

/** The timer state DataStore, layer 1 of the engine. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TimerStatePreferences

/**
 * A `CoroutineScope` that lives as long as the process, for work that must not be cancelled when a
 * screen goes away: the widget state collector, and closing a slot from a receiver.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
