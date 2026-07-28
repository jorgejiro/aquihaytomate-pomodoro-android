package com.jjrapps.aquihaytomate.ui.onboarding

import com.jjrapps.aquihaytomate.domain.model.TimerSettings

/**
 * Onboarding is the one screen with no `Loading` case, on purpose: the defaults of [TimerSettings] are a
 * perfectly good first frame, and a blank screen in front of a first-run user — even for one frame — is
 * worse than showing 25/5 straight away and correcting it when DataStore answers.
 *
 * The permissions start out as pending: `RefreshPermissionsOnResume` corrects them on the first
 * `ON_RESUME`, which happens before the user can ever swipe to page 3.
 */
data class OnboardingUiState(
    val settings: TimerSettings = TimerSettings(),
    val notificationsGranted: Boolean = false,
    val exactAlarmsGranted: Boolean = false,
) {
    val permissionsGranted: Boolean get() = notificationsGranted && exactAlarmsGranted
}
