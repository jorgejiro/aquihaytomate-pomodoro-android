package com.jjrapps.aquihaytomate.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.ui.common.PlaceholderScreen

// TODO(F7): replace with the real settings UI — SettingsRow groups, PhaseToggle, DurationSheet,
//  permission state. See docs/design-spec.md §5.3.
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(stringResource(R.string.tab_settings), modifier)
}
