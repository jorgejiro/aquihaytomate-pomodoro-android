package com.jjrapps.aquihaytomate.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjrapps.aquihaytomate.domain.model.SlotType
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
class OnboardingViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: TimerAlarmScheduler,
) : ViewModel() {

    private val permissions = MutableStateFlow(
        PermissionState(notificationsGranted = false, exactAlarmsGranted = false),
    )

    /** Only the two durations page 2 offers, plus the permission state page 3 gates on. */
    val uiState: StateFlow<OnboardingUiState> =
        combine(observeSettings(), permissions) { settings, permissionState ->
            OnboardingUiState(
                settings = settings,
                notificationsGranted = permissionState.notificationsGranted,
                exactAlarmsGranted = permissionState.exactAlarmsGranted,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = OnboardingUiState(),
        )

    /**
     * Re-read on every resume, and also right after the notification dialog answers: the system permission
     * dialog does not reliably take the Activity through `ON_PAUSE`, so resume alone would leave the row
     * saying "pending" over a permission the user just granted.
     */
    fun onResume(notificationsGranted: Boolean) {
        permissions.value = PermissionState(
            notificationsGranted = notificationsGranted,
            exactAlarmsGranted = alarmScheduler.canScheduleExactAlarms(),
        )
    }

    fun onFocusMinutesSelected(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDurationMinutes(SlotType.FOCUS, minutes)
        }
    }

    fun onBreakMinutesSelected(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDurationMinutes(SlotType.SHORT_BREAK, minutes)
        }
    }

    fun onLongBreakMinutesSelected(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDurationMinutes(SlotType.LONG_BREAK, minutes)
        }
    }

    fun onPomodorosPerCycleSelected(count: Int) {
        viewModelScope.launch { settingsRepository.setPomodorosPerCycle(count) }
    }

    fun onAutoStartBreakChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoStartBreak(enabled) }
    }

    fun onAutoStartFocusChanged(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoStartFocus(enabled) }
    }

    /** Marks onboarding done. `MainViewModel` reads this to decide the start destination. */
    fun onFinished() {
        viewModelScope.launch { settingsRepository.setOnboardingDone(true) }
    }

    private data class PermissionState(
        val notificationsGranted: Boolean,
        val exactAlarmsGranted: Boolean,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
