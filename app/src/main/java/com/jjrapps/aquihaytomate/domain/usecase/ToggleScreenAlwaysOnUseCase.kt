package com.jjrapps.aquihaytomate.domain.usecase

import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * The "screen always on" button of the Timer screen: switches the keep-screen-on setting to `always` and
 * back. See [com.jjrapps.aquihaytomate.domain.model.KeepScreenOnMode.toggleAlways].
 */
class ToggleScreenAlwaysOnUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() = settingsRepository.toggleKeepScreenOnAlways()
}
