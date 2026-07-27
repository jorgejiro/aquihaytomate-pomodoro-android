package com.jjrapps.aquihaytomate.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjrapps.aquihaytomate.BuildConfig
import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.AppLanguage
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.WidgetBackground
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerAlarmScheduler
import com.jjrapps.aquihaytomate.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: TimerAlarmScheduler,
) : ViewModel() {

    private val openSheet = MutableStateFlow<SettingsSheet?>(null)
    private val permissions = MutableStateFlow(
        PermissionState(notificationsGranted = true, exactAlarmsGranted = true),
    )

    val uiState: StateFlow<SettingsUiState> =
        combine(observeSettings(), openSheet, permissions) { settings, sheet, permissionState ->
            SettingsUiState.Success(
                settings = settings,
                notificationsGranted = permissionState.notificationsGranted,
                exactAlarmsGranted = permissionState.exactAlarmsGranted,
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                openSheet = sheet,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SettingsUiState.Loading,
        )

    /**
     * Refreshed from `onResume`: the user can grant or revoke either permission in the system settings
     * while the app is in the background, and a stale "denied" warning is worse than none.
     */
    fun onResume(notificationsGranted: Boolean) {
        permissions.value = PermissionState(
            notificationsGranted = notificationsGranted,
            exactAlarmsGranted = alarmScheduler.canScheduleExactAlarms(),
        )
    }

    fun onSheetRequested(sheet: SettingsSheet) {
        openSheet.value = sheet
    }

    fun onSheetDismissed() {
        openSheet.value = null
    }

    fun onDurationSelected(slotType: SlotType, minutes: Int) = update {
        settingsRepository.setDurationMinutes(slotType, minutes)
    }

    fun onPomodorosPerCycleSelected(count: Int) = update {
        settingsRepository.setPomodorosPerCycle(count)
    }

    fun onDailyGoalSelected(pomodoros: Int) = update {
        settingsRepository.setDailyGoal(pomodoros)
    }

    fun onAlertSoundSelected(sound: AlertSound) = update {
        settingsRepository.setAlertSound(sound)
    }

    fun onVibrationSecondsSelected(seconds: Int) = update {
        settingsRepository.setVibrationSeconds(seconds)
    }

    fun onWidgetBackgroundSelected(background: WidgetBackground) = update {
        settingsRepository.setWidgetBackground(background)
    }

    fun onLanguageSelected(language: AppLanguage) = update {
        settingsRepository.setLanguage(language)
    }

    fun onAutoStartNextChanged(enabled: Boolean) = update {
        settingsRepository.setAutoStartNext(enabled)
    }

    fun onKeepScreenOnChanged(enabled: Boolean) = update {
        settingsRepository.setKeepScreenOn(enabled)
    }

    fun onLiquidAnimationChanged(enabled: Boolean) = update {
        settingsRepository.setLiquidAnimationEnabled(enabled)
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private data class PermissionState(
        val notificationsGranted: Boolean,
        val exactAlarmsGranted: Boolean,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
