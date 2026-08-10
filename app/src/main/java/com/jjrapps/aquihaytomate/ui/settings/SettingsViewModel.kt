package com.jjrapps.aquihaytomate.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjrapps.aquihaytomate.BuildConfig
import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.AppLanguage
import com.jjrapps.aquihaytomate.domain.model.KeepScreenOnMode
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.WidgetBackground
import com.jjrapps.aquihaytomate.domain.repository.AlertPlayer
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
    private val alertPlayer: AlertPlayer,
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

    fun onDurationSelected(slotType: SlotType, minutes: Int) = update {
        settingsRepository.setDurationMinutes(slotType, minutes)
    }

    fun onPomodorosPerCycleSelected(count: Int) = update {
        settingsRepository.setPomodorosPerCycle(count)
    }

    fun onDailyGoalSelected(pomodoros: Int) = update {
        settingsRepository.setDailyGoal(pomodoros)
    }

    fun onFocusAlertSoundSelected(sound: AlertSound) = update {
        settingsRepository.setFocusAlertSound(sound)
        playPreview(sound)
    }

    fun onBreakAlertSoundSelected(sound: AlertSound) = update {
        settingsRepository.setBreakAlertSound(sound)
        playPreview(sound)
    }

    /**
     * Lo hace sonar al elegirlo. Antes se podía cambiar el sonido pero no oírlo, así que la lista de
     * nombres no servía de nada: nadie sabe qué es «Suave» hasta que lo escucha.
     *
     * Sin vibración —cero segundos—, porque lo que se está eligiendo es el sonido, y suena por el mismo
     * camino que la alerta de verdad, de modo que respeta el silencio y el volumen de alarma igual que
     * ella. Si el móvil está en silencio no se oye nada, que es exactamente lo que pasará al terminar el
     * pomodoro.
     */
    private suspend fun playPreview(sound: AlertSound) {
        alertPlayer.stop()
        if (sound.isAudible) alertPlayer.play(sound, vibrationSeconds = 0)
    }

    /** Corta la escucha al cerrar el selector, para que no siga sonando por encima de la app. */
    fun onSheetDismissed() {
        alertPlayer.stop()
        openSheet.value = null
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

    fun onAutoStartBreakChanged(enabled: Boolean) = update {
        settingsRepository.setAutoStartBreak(enabled)
    }

    fun onAutoStartFocusChanged(enabled: Boolean) = update {
        settingsRepository.setAutoStartFocus(enabled)
    }

    fun onKeepScreenOnSelected(mode: KeepScreenOnMode) = update {
        settingsRepository.setKeepScreenOn(mode)
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
