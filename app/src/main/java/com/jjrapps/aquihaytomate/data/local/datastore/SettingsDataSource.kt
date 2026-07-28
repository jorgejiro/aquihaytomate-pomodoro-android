package com.jjrapps.aquihaytomate.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.AppLanguage
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.WidgetBackground
import com.jjrapps.aquihaytomate.di.SettingsPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * User settings, in their own DataStore file. Kept separate from the timer state because the two have
 * completely different write rates and lifetimes: settings change a handful of times ever, the timer
 * state changes on every transition.
 *
 * Every setter coerces into the range declared in [TimerSettings]. Values are clamped on the way in
 * *and* on the way out, so a hand-edited file or a value written by an older build can never produce a
 * zero-length pomodoro.
 */
@Singleton
class SettingsDataSource @Inject constructor(
    @param:SettingsPreferences private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<TimerSettings> = dataStore.data.map { it.toSettings() }

    suspend fun current(): TimerSettings = settings.first()

    suspend fun setDurationMinutes(type: SlotType, minutes: Int) {
        val key = when (type) {
            SlotType.FOCUS -> Keys.FOCUS_MINUTES
            SlotType.SHORT_BREAK -> Keys.SHORT_BREAK_MINUTES
            SlotType.LONG_BREAK -> Keys.LONG_BREAK_MINUTES
        }
        val range = TimerSettings.minutesRangeFor(type)
        dataStore.edit { it[key] = minutes.coerceIn(range) }
    }

    suspend fun setPomodorosPerCycle(count: Int) = edit(Keys.POMODOROS_PER_CYCLE) {
        count.coerceIn(TimerSettings.POMODOROS_PER_CYCLE_RANGE)
    }

    suspend fun setVibrationSeconds(seconds: Int) = edit(Keys.VIBRATION_SECONDS) {
        seconds.coerceIn(TimerSettings.VIBRATION_SECONDS_RANGE)
    }

    suspend fun setDailyGoal(pomodoros: Int) = edit(Keys.DAILY_GOAL) {
        pomodoros.coerceIn(TimerSettings.DAILY_GOAL_RANGE)
    }

    suspend fun setAutoStartBreak(enabled: Boolean) = edit(Keys.AUTO_START_BREAK) { enabled }

    suspend fun setAutoStartFocus(enabled: Boolean) = edit(Keys.AUTO_START_FOCUS) { enabled }

    suspend fun setKeepScreenOn(enabled: Boolean) = edit(Keys.KEEP_SCREEN_ON) { enabled }

    suspend fun setLiquidAnimationEnabled(enabled: Boolean) =
        edit(Keys.LIQUID_ANIMATION) { enabled }

    suspend fun setOnboardingDone(done: Boolean) = edit(Keys.ONBOARDING_DONE) { done }

    suspend fun setAlertSound(sound: AlertSound) = edit(Keys.ALERT_SOUND) { sound.id }

    suspend fun setWidgetBackground(background: WidgetBackground) =
        edit(Keys.WIDGET_BACKGROUND) { background.id }

    suspend fun setLanguage(language: AppLanguage) = edit(Keys.LANGUAGE) { language.id }

    private suspend fun <T> edit(key: Preferences.Key<T>, value: () -> T) {
        dataStore.edit { it[key] = value() }
    }

    private fun Preferences.toSettings(): TimerSettings {
        val defaults = TimerSettings()
        return TimerSettings(
            focusMinutes = minutes(Keys.FOCUS_MINUTES, SlotType.FOCUS, defaults.focusMinutes),
            shortBreakMinutes = minutes(
                Keys.SHORT_BREAK_MINUTES,
                SlotType.SHORT_BREAK,
                defaults.shortBreakMinutes,
            ),
            longBreakMinutes = minutes(
                Keys.LONG_BREAK_MINUTES,
                SlotType.LONG_BREAK,
                defaults.longBreakMinutes,
            ),
            pomodorosPerCycle = (this[Keys.POMODOROS_PER_CYCLE] ?: defaults.pomodorosPerCycle)
                .coerceIn(TimerSettings.POMODOROS_PER_CYCLE_RANGE),
            // Falls back to the single auto_start_next flag these two replaced, so anyone who had it on
            // keeps chaining both ways until they touch the new switches. Reading a retired key is
            // cheaper than a migration and it costs one line.
            autoStartBreak = this[Keys.AUTO_START_BREAK]
                ?: this[Keys.RETIRED_AUTO_START_NEXT]
                ?: defaults.autoStartBreak,
            autoStartFocus = this[Keys.AUTO_START_FOCUS]
                ?: this[Keys.RETIRED_AUTO_START_NEXT]
                ?: defaults.autoStartFocus,
            alertSound = AlertSound.fromId(this[Keys.ALERT_SOUND]),
            vibrationSeconds = (this[Keys.VIBRATION_SECONDS] ?: defaults.vibrationSeconds)
                .coerceIn(TimerSettings.VIBRATION_SECONDS_RANGE),
            keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
            dailyGoal = (this[Keys.DAILY_GOAL] ?: defaults.dailyGoal)
                .coerceIn(TimerSettings.DAILY_GOAL_RANGE),
            widgetBackground = WidgetBackground.fromId(this[Keys.WIDGET_BACKGROUND]),
            language = AppLanguage.fromId(this[Keys.LANGUAGE]),
            liquidAnimationEnabled = this[Keys.LIQUID_ANIMATION] ?: defaults.liquidAnimationEnabled,
            onboardingDone = this[Keys.ONBOARDING_DONE] ?: defaults.onboardingDone,
        )
    }

    private fun Preferences.minutes(
        key: Preferences.Key<Int>,
        type: SlotType,
        default: Int,
    ): Int = (this[key] ?: default).coerceIn(TimerSettings.minutesRangeFor(type))

    /** Preference keys are a storage contract; renaming one silently resets that setting. */
    private object Keys {
        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val POMODOROS_PER_CYCLE = intPreferencesKey("pomodoros_per_cycle")
        val AUTO_START_BREAK = booleanPreferencesKey("auto_start_break")
        val AUTO_START_FOCUS = booleanPreferencesKey("auto_start_focus")

        /**
         * The single switch that became the two above. Never written any more, only read as their
         * fallback; deleting it would silently turn the setting off for whoever had it on.
         */
        val RETIRED_AUTO_START_NEXT = booleanPreferencesKey("auto_start_next")
        val ALERT_SOUND = stringPreferencesKey("alert_sound")
        val VIBRATION_SECONDS = intPreferencesKey("vibration_seconds")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val WIDGET_BACKGROUND = stringPreferencesKey("widget_background")
        val LANGUAGE = stringPreferencesKey("language")
        val LIQUID_ANIMATION = booleanPreferencesKey("liquid_animation")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }
}
