package com.jjrapps.aquihaytomate.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.AlertSound
import com.jjrapps.aquihaytomate.domain.model.AppLanguage
import com.jjrapps.aquihaytomate.domain.model.KeepScreenOnMode
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.domain.model.WidgetBackground
import com.jjrapps.aquihaytomate.ui.common.PickerOption
import com.jjrapps.aquihaytomate.ui.common.RefreshPermissionsOnResume
import com.jjrapps.aquihaytomate.ui.common.SectionLabel
import com.jjrapps.aquihaytomate.ui.common.SettingsGroup
import com.jjrapps.aquihaytomate.ui.common.SettingsRow
import com.jjrapps.aquihaytomate.ui.common.SettingsToggleRow
import com.jjrapps.aquihaytomate.ui.common.ValuePickerSheet
import com.jjrapps.aquihaytomate.ui.theme.AlertAmber
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.TomateBright

private val SCREEN_PADDING = 20.dp
private val SECTION_TOP = 20.dp
private val SECTION_BOTTOM = 8.dp

/** The chip values offered for each setting. Anything outside them is a range the user never asks for. */
private val FOCUS_MINUTES = listOf(1, 5, 10, 15, 20, 25, 30, 45, 50, 60, 90, 120)
private val SHORT_BREAK_MINUTES = listOf(1, 2, 3, 5, 8, 10, 15, 20, 30)
private val LONG_BREAK_MINUTES = listOf(5, 10, 15, 20, 25, 30, 45, 60)
private val CYCLE_LENGTHS = (2..12).toList()
private val DAILY_GOALS = (1..24).toList()
private val VIBRATION_SECONDS = listOf(0, 1, 2, 3, 5, 8, 10, 15, 20, 30)

@Composable
fun SettingsScreen(
    onOpenChangelog: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    RefreshPermissionsOnResume(viewModel::onResume)

    SettingsContent(
        state = state,
        onOpenChangelog = onOpenChangelog,
        onOpenLicenses = onOpenLicenses,
        onSheetRequested = viewModel::onSheetRequested,
        onSheetDismissed = viewModel::onSheetDismissed,
        onDurationSelected = viewModel::onDurationSelected,
        onPomodorosPerCycleSelected = viewModel::onPomodorosPerCycleSelected,
        onDailyGoalSelected = viewModel::onDailyGoalSelected,
        onFocusAlertSoundSelected = viewModel::onFocusAlertSoundSelected,
        onBreakAlertSoundSelected = viewModel::onBreakAlertSoundSelected,
        onVibrationSecondsSelected = viewModel::onVibrationSecondsSelected,
        onWidgetBackgroundSelected = viewModel::onWidgetBackgroundSelected,
        onLanguageSelected = viewModel::onLanguageSelected,
        onAutoStartBreakChanged = viewModel::onAutoStartBreakChanged,
        onAutoStartFocusChanged = viewModel::onAutoStartFocusChanged,
        onKeepScreenOnSelected = viewModel::onKeepScreenOnSelected,
        onLiquidAnimationChanged = viewModel::onLiquidAnimationChanged,
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        },
        onOpenExactAlarmSettings = {
            // ACTION_REQUEST_SCHEDULE_EXACT_ALARM, never USE_EXACT_ALARM in the manifest. See §3.
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    "package:${context.packageName}".toUri(),
                ),
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onOpenChangelog: () -> Unit,
    onOpenLicenses: () -> Unit,
    onSheetRequested: (SettingsSheet) -> Unit,
    onSheetDismissed: () -> Unit,
    onDurationSelected: (SlotType, Int) -> Unit,
    onPomodorosPerCycleSelected: (Int) -> Unit,
    onDailyGoalSelected: (Int) -> Unit,
    onFocusAlertSoundSelected: (AlertSound) -> Unit,
    onBreakAlertSoundSelected: (AlertSound) -> Unit,
    onVibrationSecondsSelected: (Int) -> Unit,
    onWidgetBackgroundSelected: (WidgetBackground) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onAutoStartBreakChanged: (Boolean) -> Unit,
    onAutoStartFocusChanged: (Boolean) -> Unit,
    onKeepScreenOnSelected: (KeepScreenOnMode) -> Unit,
    onLiquidAnimationChanged: (Boolean) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        SettingsUiState.Loading -> Box(modifier.fillMaxSize())
        is SettingsUiState.Success -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SCREEN_PADDING),
            ) {
                DurationsSection(state.settings, onSheetRequested)
                BehaviourSection(
                    state.settings,
                    onSheetRequested,
                    onAutoStartBreakChanged,
                    onAutoStartFocusChanged,
                )
                AlertsSection(state.settings, onSheetRequested)
                WidgetSection(state.settings, onSheetRequested)
                AppearanceSection(state.settings, onLiquidAnimationChanged)
                SystemSection(
                    state,
                    onSheetRequested,
                    onOpenNotificationSettings,
                    onOpenExactAlarmSettings,
                )
                AboutSection(state, onOpenChangelog, onOpenLicenses)
                Spacer(Modifier.height(SECTION_TOP * 2))
            }

            Sheets(
                state = state,
                onDismiss = onSheetDismissed,
                onDurationSelected = onDurationSelected,
                onPomodorosPerCycleSelected = onPomodorosPerCycleSelected,
                onDailyGoalSelected = onDailyGoalSelected,
                onFocusAlertSoundSelected = onFocusAlertSoundSelected,
                onBreakAlertSoundSelected = onBreakAlertSoundSelected,
                onVibrationSecondsSelected = onVibrationSecondsSelected,
                onKeepScreenOnSelected = onKeepScreenOnSelected,
                onWidgetBackgroundSelected = onWidgetBackgroundSelected,
                onLanguageSelected = onLanguageSelected,
            )
        }
    }
}

@Composable
private fun DurationsSection(
    settings: TimerSettings,
    onSheetRequested: (SettingsSheet) -> Unit,
) {
    Section(stringResource(R.string.settings_section_durations)) {
        SettingsRow(
            label = stringResource(R.string.settings_focus),
            value = minutesLabel(settings.focusMinutes),
            onClick = { onSheetRequested(SettingsSheet.Duration(SlotType.FOCUS)) },
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_short_break),
            value = minutesLabel(settings.shortBreakMinutes),
            onClick = { onSheetRequested(SettingsSheet.Duration(SlotType.SHORT_BREAK)) },
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_long_break),
            value = minutesLabel(settings.longBreakMinutes),
            onClick = { onSheetRequested(SettingsSheet.Duration(SlotType.LONG_BREAK)) },
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_pomodoros_per_cycle),
            value = settings.pomodorosPerCycle.toString(),
            onClick = { onSheetRequested(SettingsSheet.PomodorosPerCycle) },
        )
    }
}

@Composable
private fun BehaviourSection(
    settings: TimerSettings,
    onSheetRequested: (SettingsSheet) -> Unit,
    onAutoStartBreakChanged: (Boolean) -> Unit,
    onAutoStartFocusChanged: (Boolean) -> Unit,
) {
    Section(stringResource(R.string.settings_section_behaviour)) {
        // Two switches, one per direction of the chain. See TimerSettings.autoStartsInto for why they are
        // not one: a break that starts by itself is welcome, a pomodoro that does is not.
        SettingsToggleRow(
            label = stringResource(R.string.settings_auto_start_break),
            sublabel = stringResource(R.string.settings_auto_start_break_sublabel),
            checked = settings.autoStartBreak,
            onCheckedChange = onAutoStartBreakChanged,
        )
        Divider()
        SettingsToggleRow(
            label = stringResource(R.string.settings_auto_start_focus),
            sublabel = stringResource(R.string.settings_auto_start_focus_sublabel),
            checked = settings.autoStartFocus,
            onCheckedChange = onAutoStartFocusChanged,
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_keep_screen_on),
            sublabel = stringResource(R.string.settings_keep_screen_on_sublabel),
            value = stringResource(settings.keepScreenOn.labelRes),
            onClick = { onSheetRequested(SettingsSheet.KeepScreenOn) },
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_daily_goal),
            value = settings.dailyGoal.toString(),
            onClick = { onSheetRequested(SettingsSheet.DailyGoal) },
        )
    }
}

@Composable
private fun AlertsSection(
    settings: TimerSettings,
    onSheetRequested: (SettingsSheet) -> Unit,
) {
    Section(stringResource(R.string.settings_section_alerts)) {
        SettingsRow(
            label = stringResource(R.string.settings_alert_sound_focus),
            sublabel = stringResource(R.string.settings_alert_sound_focus_sublabel),
            value = stringResource(settings.focusAlertSound.labelRes),
            onClick = { onSheetRequested(SettingsSheet.FocusAlertSound) },
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_alert_sound_break),
            sublabel = stringResource(R.string.settings_alert_sound_break_sublabel),
            value = stringResource(settings.breakAlertSound.labelRes),
            onClick = { onSheetRequested(SettingsSheet.BreakAlertSound) },
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_vibration),
            value = vibrationLabel(settings.vibrationSeconds),
            onClick = { onSheetRequested(SettingsSheet.VibrationSeconds) },
        )
    }
}

@Composable
private fun WidgetSection(
    settings: TimerSettings,
    onSheetRequested: (SettingsSheet) -> Unit,
) {
    Section(stringResource(R.string.settings_section_widget)) {
        SettingsRow(
            label = stringResource(R.string.settings_widget_background),
            value = stringResource(settings.widgetBackground.labelRes),
            onClick = { onSheetRequested(SettingsSheet.WidgetBackground) },
        )
    }
}

@Composable
private fun AppearanceSection(
    settings: TimerSettings,
    onLiquidAnimationChanged: (Boolean) -> Unit,
) {
    Section(stringResource(R.string.settings_section_appearance)) {
        SettingsToggleRow(
            label = stringResource(R.string.settings_liquid_animation),
            sublabel = stringResource(R.string.settings_liquid_animation_sublabel),
            checked = settings.liquidAnimationEnabled,
            onCheckedChange = onLiquidAnimationChanged,
        )
    }
}

@Composable
private fun SystemSection(
    state: SettingsUiState.Success,
    onSheetRequested: (SettingsSheet) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    Section(stringResource(R.string.settings_section_system)) {
        SettingsRow(
            label = stringResource(R.string.settings_language),
            value = stringResource(state.settings.language.labelRes),
            onClick = { onSheetRequested(SettingsSheet.Language) },
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_notifications),
            value = stringResource(
                if (state.notificationsGranted) R.string.settings_granted else R.string.settings_denied,
            ),
            valueColor = if (state.notificationsGranted) TomateBright else AlertAmber,
            onClick = onOpenNotificationSettings,
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_exact_alarms),
            // Denied is a documented degradation, not a failure: the timer still fires through the
            // foreground service. Saying so is fairer than a bare warning.
            sublabel = if (state.exactAlarmsGranted) {
                null
            } else {
                stringResource(R.string.settings_exact_alarms_denied_sublabel)
            },
            value = stringResource(
                if (state.exactAlarmsGranted) R.string.settings_granted else R.string.settings_denied,
            ),
            valueColor = if (state.exactAlarmsGranted) TomateBright else AlertAmber,
            onClick = onOpenExactAlarmSettings,
        )
    }
}

@Composable
private fun AboutSection(
    state: SettingsUiState.Success,
    onOpenChangelog: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    Section(stringResource(R.string.settings_section_about)) {
        SettingsRow(
            label = stringResource(R.string.changelog_title),
            onClick = onOpenChangelog,
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_licenses),
            onClick = onOpenLicenses,
        )
        Divider()
        SettingsRow(
            label = stringResource(R.string.settings_version),
            value = stringResource(
                R.string.settings_version_value,
                state.versionName,
                state.versionCode,
            ),
            showChevron = false,
        )
    }
}

@Composable
private fun Sheets(
    state: SettingsUiState.Success,
    onDismiss: () -> Unit,
    onDurationSelected: (SlotType, Int) -> Unit,
    onPomodorosPerCycleSelected: (Int) -> Unit,
    onDailyGoalSelected: (Int) -> Unit,
    onFocusAlertSoundSelected: (AlertSound) -> Unit,
    onBreakAlertSoundSelected: (AlertSound) -> Unit,
    onVibrationSecondsSelected: (Int) -> Unit,
    onKeepScreenOnSelected: (KeepScreenOnMode) -> Unit,
    onWidgetBackgroundSelected: (WidgetBackground) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    when (val sheet = state.openSheet) {
        null -> Unit

        is SettingsSheet.Duration -> {
            val values = when (sheet.slotType) {
                SlotType.FOCUS -> FOCUS_MINUTES
                SlotType.SHORT_BREAK -> SHORT_BREAK_MINUTES
                SlotType.LONG_BREAK -> LONG_BREAK_MINUTES
            }
            val title = when (sheet.slotType) {
                SlotType.FOCUS -> R.string.settings_focus
                SlotType.SHORT_BREAK -> R.string.settings_short_break
                SlotType.LONG_BREAK -> R.string.settings_long_break
            }
            ValuePickerSheet(
                title = stringResource(title),
                options = values.map { PickerOption(it, minutesLabel(it)) },
                selected = state.settings.durationMinutesFor(sheet.slotType),
                onSelect = { onDurationSelected(sheet.slotType, it) },
                onDismiss = onDismiss,
            )
        }

        SettingsSheet.PomodorosPerCycle -> ValuePickerSheet(
            title = stringResource(R.string.settings_pomodoros_per_cycle),
            options = CYCLE_LENGTHS.map { PickerOption(it, it.toString()) },
            selected = state.settings.pomodorosPerCycle,
            onSelect = onPomodorosPerCycleSelected,
            onDismiss = onDismiss,
        )

        SettingsSheet.DailyGoal -> ValuePickerSheet(
            title = stringResource(R.string.settings_daily_goal),
            options = DAILY_GOALS.map { PickerOption(it, it.toString()) },
            selected = state.settings.dailyGoal,
            onSelect = onDailyGoalSelected,
            onDismiss = onDismiss,
        )

        // Los dos selectores de sonido se quedan abiertos y suenan al tocarlos, para poder comparar.
        SettingsSheet.FocusAlertSound -> ValuePickerSheet(
            title = stringResource(R.string.settings_alert_sound_focus),
            hint = stringResource(R.string.settings_alert_sound_hint),
            options = AlertSound.entries.map { PickerOption(it, stringResource(it.labelRes)) },
            selected = state.settings.focusAlertSound,
            onSelect = onFocusAlertSoundSelected,
            onDismiss = onDismiss,
            stayOpen = true,
            doneLabel = stringResource(R.string.action_done),
        )

        SettingsSheet.BreakAlertSound -> ValuePickerSheet(
            title = stringResource(R.string.settings_alert_sound_break),
            hint = stringResource(R.string.settings_alert_sound_hint),
            options = AlertSound.entries.map { PickerOption(it, stringResource(it.labelRes)) },
            selected = state.settings.breakAlertSound,
            onSelect = onBreakAlertSoundSelected,
            onDismiss = onDismiss,
            stayOpen = true,
            doneLabel = stringResource(R.string.action_done),
        )

        SettingsSheet.VibrationSeconds -> ValuePickerSheet(
            title = stringResource(R.string.settings_vibration),
            options = VIBRATION_SECONDS.map { PickerOption(it, vibrationLabel(it)) },
            selected = state.settings.vibrationSeconds,
            onSelect = onVibrationSecondsSelected,
            onDismiss = onDismiss,
        )

        SettingsSheet.KeepScreenOn -> ValuePickerSheet(
            title = stringResource(R.string.settings_keep_screen_on),
            hint = stringResource(R.string.settings_keep_screen_on_hint),
            options = KeepScreenOnMode.entries.map {
                PickerOption(it, stringResource(it.labelRes))
            },
            selected = state.settings.keepScreenOn,
            onSelect = onKeepScreenOnSelected,
            onDismiss = onDismiss,
        )

        SettingsSheet.WidgetBackground -> ValuePickerSheet(
            title = stringResource(R.string.settings_widget_background),
            options = WidgetBackground.entries.map { PickerOption(it, stringResource(it.labelRes)) },
            selected = state.settings.widgetBackground,
            onSelect = onWidgetBackgroundSelected,
            onDismiss = onDismiss,
        )

        SettingsSheet.Language -> ValuePickerSheet(
            title = stringResource(R.string.settings_language),
            options = AppLanguage.entries.map { PickerOption(it, stringResource(it.labelRes)) },
            selected = state.settings.language,
            onSelect = onLanguageSelected,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable com.jjrapps.aquihaytomate.ui.common.SettingsGroupScope.() -> Unit,
) {
    Spacer(Modifier.height(SECTION_TOP))
    SectionLabel(title)
    Spacer(Modifier.height(SECTION_BOTTOM))
    SettingsGroup(content = content)
}

@Composable
private fun minutesLabel(minutes: Int): String =
    pluralStringResource(R.plurals.settings_minutes, minutes, minutes)

/** Zero seconds is the off switch for vibration, so it reads as "off" rather than as "0 s". */
@Composable
private fun vibrationLabel(seconds: Int): String = if (seconds <= 0) {
    stringResource(R.string.settings_vibration_off)
} else {
    pluralStringResource(R.plurals.settings_seconds, seconds, seconds)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 1600)
@Composable
private fun SettingsScreenPreview() {
    AquiHayTomateTheme {
        SettingsContent(
            state = SettingsUiState.Success(
                settings = TimerSettings(),
                notificationsGranted = true,
                exactAlarmsGranted = false,
                versionName = "0.9.0",
                versionCode = 1,
            ),
            onOpenChangelog = {},
            onOpenLicenses = {},
            onSheetRequested = {},
            onSheetDismissed = {},
            onDurationSelected = { _, _ -> },
            onPomodorosPerCycleSelected = {},
            onDailyGoalSelected = {},
            onFocusAlertSoundSelected = {},
            onBreakAlertSoundSelected = {},
            onVibrationSecondsSelected = {},
            onWidgetBackgroundSelected = {},
            onLanguageSelected = {},
            onAutoStartBreakChanged = {},
            onAutoStartFocusChanged = {},
            onKeepScreenOnSelected = {},
            onLiquidAnimationChanged = {},
            onOpenNotificationSettings = {},
            onOpenExactAlarmSettings = {},
        )
    }
}
