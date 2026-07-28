package com.jjrapps.aquihaytomate.ui.changelog

import androidx.lifecycle.ViewModel
import com.jjrapps.aquihaytomate.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class ChangelogViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<ChangelogUiState>(
        ChangelogUiState.Success(
            releases = ChangelogCatalog.releases,
            installedVersionCode = BuildConfig.VERSION_CODE,
        ),
    )

    val uiState: StateFlow<ChangelogUiState> = _uiState.asStateFlow()
}
