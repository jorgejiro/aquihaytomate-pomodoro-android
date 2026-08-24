package com.jjrapps.aquihaytomate.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerSettings
import com.jjrapps.aquihaytomate.ui.common.LiquidTomato
import com.jjrapps.aquihaytomate.ui.common.PagerDots
import com.jjrapps.aquihaytomate.ui.common.PickerOption
import com.jjrapps.aquihaytomate.ui.common.RefreshPermissionsOnResume
import com.jjrapps.aquihaytomate.ui.common.SectionLabel
import com.jjrapps.aquihaytomate.ui.common.SettingsGroup
import com.jjrapps.aquihaytomate.ui.common.SettingsRow
import com.jjrapps.aquihaytomate.ui.common.SettingsToggleRow
import com.jjrapps.aquihaytomate.ui.common.TextControl
import com.jjrapps.aquihaytomate.ui.common.ValueChip
import com.jjrapps.aquihaytomate.ui.theme.AlertAmber
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.BodyDefault
import com.jjrapps.aquihaytomate.ui.theme.Caption
import com.jjrapps.aquihaytomate.ui.theme.LocalReducedMotion
import com.jjrapps.aquihaytomate.ui.theme.TextGhost
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.TextSecondary
import com.jjrapps.aquihaytomate.ui.theme.TitleScreen
import com.jjrapps.aquihaytomate.ui.theme.TomateBright
import com.jjrapps.aquihaytomate.ui.theme.phaseColorsOf
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 5

/**
 * The page the two permissions live on, and the only one the forward control is gated on.
 *
 * It is no longer the last page — the alert repeats page is — so the gate cannot ride on `EMPEZAR` any
 * more: greying out the finish control on a page that says nothing about permissions would read as broken.
 * The pressure is the same, applied where it is legible. See docs/decisions/008-*.
 */
private const val PERMISSIONS_PAGE = 3

private const val DRAIN_LOOP_MS = 6000
private const val REDUCED_MOTION_FILL = 0.55f

private val SCREEN_PADDING = 32.dp
private val TOMATO_SIZE = 120.dp
private val WIDGET_TOMATO_SIZE = 48.dp
private val BODY_MAX_WIDTH = 260.dp
private val FOOTER_PADDING = 24.dp

/** Reserved on every page so the pager does not shift when the escape hatch appears on the permissions page. */
private val ESCAPE_SLOT_HEIGHT = 40.dp

/** The permission rows own their page, so they get more room than the 52 dp of a Settings row. */
private val PERMISSION_ROW_HEIGHT = 64.dp
private val PERMISSIONS_LABEL_GAP = 14.dp

private val FOCUS_CHOICES = listOf(20, 25, 30, 45)
private val BREAK_CHOICES = listOf(3, 5, 10, 15)
private val CYCLE_CHOICES = listOf(2, 3, 4, 6)
private val LONG_BREAK_CHOICES = listOf(10, 15, 20, 30)

/** A subset of the 1..10 Settings offers: enough to make the idea land, few enough to fit one row. */
private val REPEAT_CHOICES = listOf(1, 2, 3, 4)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    RefreshPermissionsOnResume(viewModel::onResume)

    // The runtime dialog is shown at most once per install: after a denial Android drops it silently, so a
    // second tap has to go to the system settings or the row would look broken.
    var notificationDialogShown by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onResume(notificationsGranted = granted) }

    val finish = {
        viewModel.onFinished()
        onFinished()
    }

    OnboardingContent(
        state = state,
        onFocusMinutesSelected = viewModel::onFocusMinutesSelected,
        onBreakMinutesSelected = viewModel::onBreakMinutesSelected,
        onPomodorosPerCycleSelected = viewModel::onPomodorosPerCycleSelected,
        onLongBreakMinutesSelected = viewModel::onLongBreakMinutesSelected,
        onAutoStartBreakChanged = viewModel::onAutoStartBreakChanged,
        onAutoStartFocusChanged = viewModel::onAutoStartFocusChanged,
        onFocusAlertRepeatsSelected = viewModel::onFocusAlertRepeatsSelected,
        onBreakAlertRepeatsSelected = viewModel::onBreakAlertRepeatsSelected,
        onRequestNotifications = {
            val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !notificationDialogShown
            if (canAsk) {
                notificationDialogShown = true
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(notificationSettingsIntent(context))
            }
        },
        onRequestExactAlarms = { context.startActivity(exactAlarmSettingsIntent(context)) },
        onFinished = finish,
        modifier = modifier,
    )
}

@Composable
private fun OnboardingContent(
    state: OnboardingUiState,
    onFocusMinutesSelected: (Int) -> Unit,
    onBreakMinutesSelected: (Int) -> Unit,
    onPomodorosPerCycleSelected: (Int) -> Unit,
    onLongBreakMinutesSelected: (Int) -> Unit,
    onAutoStartBreakChanged: (Boolean) -> Unit,
    onAutoStartFocusChanged: (Boolean) -> Unit,
    onFocusAlertRepeatsSelected: (Int) -> Unit,
    onBreakAlertRepeatsSelected: (Int) -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize().safeDrawingPadding()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            // The gate would be decorative without this: greying out the forward control means nothing if
            // the page after it is one swipe away. The gesture comes back the moment both permissions are
            // in, and `CONTINUAR SIN ELLOS` is there in the meantime — that escape hatch is what makes
            // holding the pager here acceptable rather than a trap. See docs/decisions/008-*.
            userScrollEnabled = !isGated(pagerState.currentPage, state.permissionsGranted),
        ) { page ->
            when (page) {
                0 -> WhatItIsPage()
                1 -> DurationsPage(state.settings, onFocusMinutesSelected, onBreakMinutesSelected)
                2 -> CyclePage(
                    settings = state.settings,
                    onPomodorosPerCycleSelected = onPomodorosPerCycleSelected,
                    onLongBreakMinutesSelected = onLongBreakMinutesSelected,
                    onAutoStartBreakChanged = onAutoStartBreakChanged,
                    onAutoStartFocusChanged = onAutoStartFocusChanged,
                )
                PERMISSIONS_PAGE -> WidgetAndPermissionsPage(
                    state = state,
                    onRequestNotifications = onRequestNotifications,
                    onRequestExactAlarms = onRequestExactAlarms,
                )
                else -> AlertRepeatsPage(
                    settings = state.settings,
                    onFocusAlertRepeatsSelected = onFocusAlertRepeatsSelected,
                    onBreakAlertRepeatsSelected = onBreakAlertRepeatsSelected,
                )
            }
        }

        Footer(
            currentPage = pagerState.currentPage,
            permissionsGranted = state.permissionsGranted,
            onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            onFinished = onFinished,
        )
    }
}

/**
 * Whether the pager is being held on this page: only ever the permissions page, and only until both are
 * granted. Shared by the footer and the pager itself so the control and the gesture cannot disagree.
 */
private fun isGated(page: Int, permissionsGranted: Boolean): Boolean =
    page == PERMISSIONS_PAGE && !permissionsGranted

/**
 * Page dots, the forward control, and — only while a permission is still pending — the way out.
 *
 * The forward control is greyed out on the permissions page until both are in place: they are what makes
 * the timer ring on time, so onboarding presses for them rather than mentioning them. The escape hatch is
 * not optional politeness: Android lets the user deny either permission for good, and a first-run screen
 * that could trap them there would be a bug. It moves the pager on rather than finishing, because there is
 * a page after this one now. See docs/decisions/008-*.
 */
@Composable
private fun Footer(
    currentPage: Int,
    permissionsGranted: Boolean,
    onNext: () -> Unit,
    onFinished: () -> Unit,
) {
    val isLastPage = currentPage == PAGE_COUNT - 1
    val gated = isGated(currentPage, permissionsGranted)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FOOTER_PADDING)
            .padding(bottom = FOOTER_PADDING),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PagerDots(
                pageCount = PAGE_COUNT,
                currentPage = currentPage,
                contentDescription = stringResource(
                    R.string.a11y_cycle_position,
                    currentPage + 1,
                    PAGE_COUNT,
                ),
            )

            TextControl(
                label = stringResource(
                    if (isLastPage) R.string.onboarding_start else R.string.onboarding_next,
                ),
                onClick = { if (isLastPage) onFinished() else onNext() },
                color = if (gated) TextGhost else TextPrimary,
                enabled = !gated,
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(ESCAPE_SLOT_HEIGHT),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (gated) {
                TextControl(
                    label = stringResource(R.string.onboarding_continue_without),
                    onClick = onNext,
                    color = TextMuted,
                    style = Caption,
                    height = ESCAPE_SLOT_HEIGHT,
                )
            }
        }
    }
}

/**
 * Page 1, with the tomato draining from full to empty on a six second loop. It is the demonstration of
 * the whole concept, so it is the one animation worth running here.
 */
@Composable
private fun WhatItIsPage() {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "drain")
    val animatedFill by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(DRAIN_LOOP_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "drainFill",
    )

    Page {
        LiquidTomato(
            // With reduced motion the loop is replaced by a still tomato, not by an empty gap.
            fillFraction = if (reduced) REDUCED_MOTION_FILL else animatedFill,
            colors = phaseColorsOf(SlotType.FOCUS),
            modifier = Modifier.size(TOMATO_SIZE),
        )
        Spacer(Modifier.height(32.dp))
        Title(stringResource(R.string.onboarding_page1_title))
        Spacer(Modifier.height(12.dp))
        Body(stringResource(R.string.onboarding_page1_body))
    }
}

@Composable
private fun DurationsPage(
    settings: TimerSettings,
    onFocusMinutesSelected: (Int) -> Unit,
    onBreakMinutesSelected: (Int) -> Unit,
) {
    Page {
        Title(stringResource(R.string.onboarding_page2_title))
        Spacer(Modifier.height(32.dp))

        SectionLabel(stringResource(R.string.onboarding_page2_focus))
        Spacer(Modifier.height(8.dp))
        ChoiceRow(
            options = FOCUS_CHOICES.map { PickerOption(it, minutesLabel(it)) },
            selected = settings.focusMinutes,
            onSelect = onFocusMinutesSelected,
        )

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.onboarding_page2_break))
        Spacer(Modifier.height(8.dp))
        ChoiceRow(
            options = BREAK_CHOICES.map { PickerOption(it, minutesLabel(it)) },
            selected = settings.shortBreakMinutes,
            onSelect = onBreakMinutesSelected,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_page2_hint),
            style = Caption,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Page 3: the shape of a cycle, and whether the timer chains on its own.
 *
 * These four are the settings that decide how the app *feels* over a whole morning, and they are the ones
 * worth asking about before the first pomodoro rather than leaving buried in Settings. The two switches are
 * the same rows as Settings, defaults included: the break starts by itself, the next pomodoro does not.
 */
@Composable
private fun CyclePage(
    settings: TimerSettings,
    onPomodorosPerCycleSelected: (Int) -> Unit,
    onLongBreakMinutesSelected: (Int) -> Unit,
    onAutoStartBreakChanged: (Boolean) -> Unit,
    onAutoStartFocusChanged: (Boolean) -> Unit,
) {
    Page {
        Title(stringResource(R.string.onboarding_cycle_title))
        Spacer(Modifier.height(28.dp))

        SectionLabel(stringResource(R.string.onboarding_cycle_pomodoros))
        Spacer(Modifier.height(8.dp))
        ChoiceRow(
            options = CYCLE_CHOICES.map { PickerOption(it, it.toString()) },
            selected = settings.pomodorosPerCycle,
            onSelect = onPomodorosPerCycleSelected,
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.onboarding_cycle_long_break))
        Spacer(Modifier.height(8.dp))
        ChoiceRow(
            options = LONG_BREAK_CHOICES.map { PickerOption(it, minutesLabel(it)) },
            selected = settings.longBreakMinutes,
            onSelect = onLongBreakMinutesSelected,
        )

        Spacer(Modifier.height(28.dp))
        SectionLabel(stringResource(R.string.onboarding_cycle_chaining))
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
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
        }
    }
}

@Composable
private fun WidgetAndPermissionsPage(
    state: OnboardingUiState,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
) {
    Page {
        LiquidTomato(
            fillFraction = 0.6f,
            colors = phaseColorsOf(SlotType.FOCUS),
            modifier = Modifier.size(WIDGET_TOMATO_SIZE),
        )
        Spacer(Modifier.height(24.dp))
        Title(stringResource(R.string.onboarding_permissions_title))
        Spacer(Modifier.height(12.dp))
        Body(stringResource(R.string.onboarding_permissions_body))

        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.onboarding_permissions_label))
        Spacer(Modifier.height(PERMISSIONS_LABEL_GAP))
        SettingsGroup {
            PermissionRow(
                label = stringResource(R.string.settings_notifications),
                granted = state.notificationsGranted,
                onClick = onRequestNotifications,
            )
            Divider()
            PermissionRow(
                label = stringResource(R.string.settings_exact_alarms),
                granted = state.exactAlarmsGranted,
                onClick = onRequestExactAlarms,
            )
        }

        // Only while something is still pending: once both are granted the warning has nothing to warn
        // about, and leaving it there reads as if the rows above had not worked.
        if (!state.permissionsGranted) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_permissions_hint),
                style = Caption,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The last page: how many times each end of a slot rings.
 *
 * It is here and not buried in Settings because missing the end of a slot is the failure nobody attributes
 * to a setting — you assume the timer did not ring, not that it rang once while you were in the kitchen.
 * Both ends default to two plays, chained without a gap so they read as one longer alert, and both are
 * asked separately for the same reason the sounds are: getting up from the desk and coming back to it are
 * not equally easy to sleep through.
 */
@Composable
private fun AlertRepeatsPage(
    settings: TimerSettings,
    onFocusAlertRepeatsSelected: (Int) -> Unit,
    onBreakAlertRepeatsSelected: (Int) -> Unit,
) {
    Page {
        Title(stringResource(R.string.onboarding_alerts_title))
        Spacer(Modifier.height(12.dp))
        Body(stringResource(R.string.onboarding_alerts_body))

        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.onboarding_alerts_focus))
        Spacer(Modifier.height(8.dp))
        ChoiceRow(
            options = REPEAT_CHOICES.map { PickerOption(it, repeatsLabel(it)) },
            selected = settings.focusAlertRepeats,
            onSelect = onFocusAlertRepeatsSelected,
        )

        Spacer(Modifier.height(24.dp))
        SectionLabel(stringResource(R.string.onboarding_alerts_break))
        Spacer(Modifier.height(8.dp))
        ChoiceRow(
            options = REPEAT_CHOICES.map { PickerOption(it, repeatsLabel(it)) },
            selected = settings.breakAlertRepeats,
            onSelect = onBreakAlertRepeatsSelected,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_page2_hint),
            style = Caption,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * One permission, its state spelled out rather than implied.
 *
 * A row that says "pending" in amber with "required" underneath is what the old plain "Allow
 * notifications" control failed to do: nothing about it said whether it had been dealt with. Granted rows
 * lose their chevron and their tap target — there is nothing left to do on them.
 */
@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    SettingsRow(
        label = label,
        // Taller than in Settings: here the two rows are the whole content of the page and at 52 dp they
        // sat on top of each other.
        minHeight = PERMISSION_ROW_HEIGHT,
        sublabel = if (granted) null else stringResource(R.string.onboarding_permissions_required),
        value = stringResource(
            if (granted) R.string.onboarding_permissions_ready else R.string.onboarding_permissions_pending,
        ),
        valueColor = if (granted) TomateBright else AlertAmber,
        showChevron = !granted,
        onClick = if (granted) null else onClick,
    )
}

/**
 * The frame every onboarding page shares: centred when it fits, scrollable when it does not.
 *
 * `heightIn(min = maxHeight)` is what keeps both: the column is at least as tall as the viewport, so
 * `Arrangement.Center` still centres a short page, and it grows past it when the content is taller —
 * a phone on its side, or a large font scale, where the third page has four settings and two permission
 * rows to fit. Without the scroll the bottom of the page, `EMPEZAR` included, simply is not reachable.
 */
@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .fillMaxWidth()
                .padding(horizontal = SCREEN_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

@Composable
private fun Title(text: String) {
    Text(text = text, style = TitleScreen, color = TextPrimary, textAlign = TextAlign.Center)
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = BodyDefault,
        color = TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = BODY_MAX_WIDTH),
    )
}

@Composable
private fun <T> ChoiceRow(
    options: List<PickerOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            Box(Modifier.weight(1f)) {
                ValueChip(
                    label = option.label,
                    selected = option.value == selected,
                    onClick = { onSelect(option.value) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

/** ACTION_REQUEST_SCHEDULE_EXACT_ALARM, never USE_EXACT_ALARM in the manifest. See CLAUDE.md §3. */
private fun exactAlarmSettingsIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        "package:${context.packageName}".toUri(),
    )

@Composable
private fun minutesLabel(minutes: Int): String =
    pluralStringResource(R.plurals.settings_minutes, minutes, minutes)

@Composable
private fun repeatsLabel(times: Int): String =
    pluralStringResource(R.plurals.settings_alert_repeats_value, times, times)

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingPage1Preview() {
    AquiHayTomateTheme {
        OnboardingContent(
            state = OnboardingUiState(),
            onFocusMinutesSelected = {},
            onBreakMinutesSelected = {},
            onPomodorosPerCycleSelected = {},
            onLongBreakMinutesSelected = {},
            onAutoStartBreakChanged = {},
            onAutoStartFocusChanged = {},
            onFocusAlertRepeatsSelected = {},
            onBreakAlertRepeatsSelected = {},
            onRequestNotifications = {},
            onRequestExactAlarms = {},
            onFinished = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingCyclePreview() {
    AquiHayTomateTheme {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                CyclePage(
                    settings = TimerSettings(),
                    onPomodorosPerCycleSelected = {},
                    onLongBreakMinutesSelected = {},
                    onAutoStartBreakChanged = {},
                    onAutoStartFocusChanged = {},
                )
            }
            Footer(currentPage = 2, permissionsGranted = false, onNext = {}, onFinished = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingPermissionsPendingPreview() {
    AquiHayTomateTheme {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                WidgetAndPermissionsPage(
                    state = OnboardingUiState(),
                    onRequestNotifications = {},
                    onRequestExactAlarms = {},
                )
            }
            Footer(
                currentPage = PERMISSIONS_PAGE,
                permissionsGranted = false,
                onNext = {},
                onFinished = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingPermissionsGrantedPreview() {
    AquiHayTomateTheme {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                WidgetAndPermissionsPage(
                    state = OnboardingUiState(
                        notificationsGranted = true,
                        exactAlarmsGranted = true,
                    ),
                    onRequestNotifications = {},
                    onRequestExactAlarms = {},
                )
            }
            Footer(
                currentPage = PERMISSIONS_PAGE,
                permissionsGranted = true,
                onNext = {},
                onFinished = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingAlertRepeatsPreview() {
    AquiHayTomateTheme {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                AlertRepeatsPage(
                    settings = TimerSettings(),
                    onFocusAlertRepeatsSelected = {},
                    onBreakAlertRepeatsSelected = {},
                )
            }
            Footer(
                currentPage = PAGE_COUNT - 1,
                permissionsGranted = true,
                onNext = {},
                onFinished = {},
            )
        }
    }
}
