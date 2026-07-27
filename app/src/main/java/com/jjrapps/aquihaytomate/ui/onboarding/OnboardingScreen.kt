package com.jjrapps.aquihaytomate.ui.onboarding

import android.Manifest
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.jjrapps.aquihaytomate.ui.common.ControlGlyph
import com.jjrapps.aquihaytomate.ui.common.LiquidTomato
import com.jjrapps.aquihaytomate.ui.common.PagerDots
import com.jjrapps.aquihaytomate.ui.common.PickerOption
import com.jjrapps.aquihaytomate.ui.common.SectionLabel
import com.jjrapps.aquihaytomate.ui.common.TextControl
import com.jjrapps.aquihaytomate.ui.common.ValueChip
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.BodyDefault
import com.jjrapps.aquihaytomate.ui.theme.Caption
import com.jjrapps.aquihaytomate.ui.theme.LocalReducedMotion
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.TextSecondary
import com.jjrapps.aquihaytomate.ui.theme.TitleScreen
import com.jjrapps.aquihaytomate.ui.theme.phaseColorsOf
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3
private const val DRAIN_LOOP_MS = 6000
private const val REDUCED_MOTION_FILL = 0.55f

private val SCREEN_PADDING = 32.dp
private val TOMATO_SIZE = 120.dp
private val BODY_MAX_WIDTH = 260.dp

private val FOCUS_CHOICES = listOf(20, 25, 30, 45)
private val BREAK_CHOICES = listOf(3, 5, 10, 15)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    OnboardingContent(
        settings = settings,
        onFocusMinutesSelected = viewModel::onFocusMinutesSelected,
        onBreakMinutesSelected = viewModel::onBreakMinutesSelected,
        onFinished = {
            viewModel.onFinished()
            onFinished()
        },
        modifier = modifier,
    )
}

@Composable
private fun OnboardingContent(
    settings: TimerSettings,
    onFocusMinutesSelected: (Int) -> Unit,
    onBreakMinutesSelected: (Int) -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> WhatItIsPage()
                1 -> DurationsPage(settings, onFocusMinutesSelected, onBreakMinutesSelected)
                else -> WidgetAndPermissionsPage()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PagerDots(
                pageCount = PAGE_COUNT,
                currentPage = pagerState.currentPage,
                contentDescription = stringResource(
                    R.string.a11y_cycle_position,
                    pagerState.currentPage + 1,
                    PAGE_COUNT,
                ),
            )

            val isLastPage = pagerState.currentPage == PAGE_COUNT - 1
            TextControl(
                label = stringResource(
                    if (isLastPage) R.string.onboarding_start else R.string.onboarding_next,
                ),
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
            )
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
            showCalyx = false,
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

@Composable
private fun WidgetAndPermissionsPage() {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Denied is fine: the timer works without notifications. */ }

    Page {
        LiquidTomato(
            fillFraction = 0.6f,
            colors = phaseColorsOf(SlotType.FOCUS),
            showCalyx = false,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(32.dp))
        Title(stringResource(R.string.onboarding_page3_title))
        Spacer(Modifier.height(12.dp))
        Body(stringResource(R.string.onboarding_page3_body))

        Spacer(Modifier.height(24.dp))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            TextControl(
                label = stringResource(R.string.onboarding_page3_notifications),
                glyph = ControlGlyph.NONE,
                onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )
        }
        TextControl(
            label = stringResource(R.string.onboarding_page3_exact_alarms),
            color = TextSecondary,
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        "package:${context.packageName}".toUri(),
                    ),
                )
            },
        )
        Text(
            text = stringResource(R.string.onboarding_page3_optional),
            style = Caption,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SCREEN_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
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

@Composable
private fun minutesLabel(minutes: Int): String =
    pluralStringResource(R.plurals.settings_minutes, minutes, minutes)

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 720)
@Composable
private fun OnboardingPage1Preview() {
    AquiHayTomateTheme {
        OnboardingContent(
            settings = TimerSettings(),
            onFocusMinutesSelected = {},
            onBreakMinutesSelected = {},
            onFinished = {},
        )
    }
}
