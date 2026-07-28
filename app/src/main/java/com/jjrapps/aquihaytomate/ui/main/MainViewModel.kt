package com.jjrapps.aquihaytomate.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjrapps.aquihaytomate.domain.model.AppLanguage
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.usecase.ObserveSettingsUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ObserveTimerStateUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ReconcileTimerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the shell around the tabs needs to know before it can draw anything. */
sealed interface MainUiState {

    /** The persisted settings have not been read yet, so the start destination is still unknown. */
    data object Loading : MainUiState

    data class Ready(
        val onboardingDone: Boolean,
        val language: AppLanguage,
        /** Drives the tab underline, which follows the current phase. */
        val phase: SlotType,
        val liquidAnimationEnabled: Boolean,
    ) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    observeTimerState: ObserveTimerStateUseCase,
    private val reconcileTimer: ReconcileTimerUseCase,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> =
        combine(observeSettings(), observeTimerState().map { it.slotType }) { settings, phase ->
            MainUiState.Ready(
                onboardingDone = settings.onboardingDone,
                language = settings.language,
                phase = phase,
                liquidAnimationEnabled = settings.liquidAnimationEnabled,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = MainUiState.Loading,
        )

    init {
        // Opening the app is one of the moments the engine repairs itself: the process may have been
        // killed, or a slot may have run out while nobody was watching.
        viewModelScope.launch { reconcileTimer() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
