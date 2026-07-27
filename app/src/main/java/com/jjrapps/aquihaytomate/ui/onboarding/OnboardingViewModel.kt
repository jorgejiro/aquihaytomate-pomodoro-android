package com.jjrapps.aquihaytomate.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Only the two durations page 2 offers; the rest of Settings is not part of onboarding. */
    val settings: StateFlow<TimerSettings> = observeSettings()
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = TimerSettings(),
        )

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

    /** Marks onboarding done. `MainViewModel` reads this to decide the start destination. */
    fun onFinished() {
        viewModelScope.launch { settingsRepository.setOnboardingDone(true) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
