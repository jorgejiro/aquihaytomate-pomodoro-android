package com.jjrapps.aquihaytomate.ui.changelog

sealed interface ChangelogUiState {

    data object Loading : ChangelogUiState

    data class Success(
        val releases: List<ChangelogRelease>,
        /** The version code the app is running, to mark one entry as current. */
        val installedVersionCode: Int,
    ) : ChangelogUiState
}
