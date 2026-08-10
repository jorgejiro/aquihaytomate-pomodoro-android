package com.jjrapps.aquihaytomate.ui.settings

import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings

/** Which picker sheet is open, if any. */
sealed interface SettingsSheet {
    data class Duration(val slotType: SlotType) : SettingsSheet
    data object PomodorosPerCycle : SettingsSheet
    data object DailyGoal : SettingsSheet
    data object FocusAlertSound : SettingsSheet
    data object BreakAlertSound : SettingsSheet
    data object FocusAlertRepeats : SettingsSheet
    data object BreakAlertRepeats : SettingsSheet
    data object VibrationSeconds : SettingsSheet
    data object KeepScreenOn : SettingsSheet
    data object WidgetBackground : SettingsSheet
    data object Language : SettingsSheet
}

sealed interface SettingsUiState {

    data object Loading : SettingsUiState

    /**
     * @param notificationsGranted refreshed in `onResume`, because the user may have changed it in the
     *   system settings while the app was in the background.
     * @param exactAlarmsGranted same. Denied is not an error — the timer works without it — but Settings
     *   says so, because it does cost reliability with the screen off.
     */
    data class Success(
        val settings: TimerSettings,
        val notificationsGranted: Boolean,
        val exactAlarmsGranted: Boolean,
        val versionName: String,
        val versionCode: Int,
        val openSheet: SettingsSheet? = null,
    ) : SettingsUiState
}
