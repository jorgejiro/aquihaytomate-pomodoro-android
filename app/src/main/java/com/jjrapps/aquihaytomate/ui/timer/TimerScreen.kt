package com.jjrapps.aquihaytomate.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.domain.usecase.TimerMath
import com.jjrapps.aquihaytomate.ui.common.ControlGlyph
import com.jjrapps.aquihaytomate.ui.common.CycleDots
import com.jjrapps.aquihaytomate.ui.common.LiquidCountdown
import com.jjrapps.aquihaytomate.ui.common.PhaseLabel
import com.jjrapps.aquihaytomate.ui.common.TextControl
import com.jjrapps.aquihaytomate.ui.common.phaseLabelRes
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.Caption
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.phaseColorsOf

private val TOMATO_DIAMETER = 268.dp
private val TOMATO_DIAMETER_COMPACT = 224.dp
private val COMPACT_HEIGHT_THRESHOLD = 600.dp

private val TOMATO_TO_PHASE = 20.dp
private val PHASE_TO_CONTROL = 8.dp
private val CONTROL_TO_RESET = 4.dp
private val RESET_TO_DOTS = 28.dp
private val SCREEN_PADDING = 20.dp
private val RESET_HEIGHT = 32.dp

private const val RESET_FADE_MS = 150

@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TimerContent(
        state = state,
        onPrimaryClick = viewModel::onPrimaryControlClick,
        onResetClick = viewModel::onResetClick,
        modifier = modifier,
    )
}

@Composable
private fun TimerContent(
    state: TimerUiState,
    onPrimaryClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().padding(horizontal = SCREEN_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        // 224 dp on a short screen, so the whole block still fits without clipping. See §5.1.
        val diameter = if (maxHeight < COMPACT_HEIGHT_THRESHOLD) {
            TOMATO_DIAMETER_COMPACT
        } else {
            TOMATO_DIAMETER
        }

        when (state) {
            TimerUiState.Loading -> Unit
            is TimerUiState.Success -> TimerBlock(
                state = state,
                diameter = diameter,
                onPrimaryClick = onPrimaryClick,
                onResetClick = onResetClick,
            )
        }
    }
}

@Composable
private fun TimerBlock(
    state: TimerUiState.Success,
    diameter: Dp,
    onPrimaryClick: () -> Unit,
    onResetClick: () -> Unit,
) {
    KeepScreenOn(state.keepScreenOn)

    val colors = phaseColorsOf(state.slotType)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LiquidCountdown(
            timeText = state.timeText,
            fillFraction = state.fillFraction,
            colors = colors,
            showCalyx = state.showCalyx,
            contentDescription = tomatoContentDescription(state),
            modifier = Modifier.size(diameter),
        )

        Spacer(Modifier.height(TOMATO_TO_PHASE))
        PhaseLabel(slotType = state.slotType, status = state.status, accent = colors.bright)

        Spacer(Modifier.height(PHASE_TO_CONTROL))
        TextControl(
            label = stringResource(state.primaryControl.labelRes),
            onClick = onPrimaryClick,
            glyph = state.primaryControl.glyph,
        )

        Spacer(Modifier.height(CONTROL_TO_RESET))
        AnimatedVisibility(
            visible = state.showReset,
            enter = fadeIn(tween(RESET_FADE_MS)),
            exit = fadeOut(tween(RESET_FADE_MS)),
        ) {
            TextControl(
                label = stringResource(R.string.control_reset),
                onClick = onResetClick,
                color = TextMuted,
                style = Caption,
                uppercase = false,
                height = RESET_HEIGHT,
            )
        }

        Spacer(Modifier.height(RESET_TO_DOTS))
        CycleDots(
            completed = state.completedInCycle,
            current = state.cyclePosition,
            total = state.pomodorosPerCycle,
            accent = colors.bright,
        )
    }
}

/** The label of the primary control, from docs/design-spec.md §5.1. */
private val PrimaryControl.labelRes: Int
    get() = when (this) {
        PrimaryControl.START -> R.string.control_start
        PrimaryControl.PAUSE -> R.string.control_pause
        PrimaryControl.RESUME -> R.string.control_resume
        PrimaryControl.START_BREAK -> R.string.control_start_break
        PrimaryControl.BACK_TO_WORK -> R.string.control_back_to_work
    }

private val PrimaryControl.glyph: ControlGlyph
    get() = if (this == PrimaryControl.PAUSE) ControlGlyph.PAUSE else ControlGlyph.PLAY

/**
 * What a screen reader announces for the tomato: the phase, how far along the slot is, and the time
 * left spelled out — never the raw `18:42`, which is read as a ratio. See docs/design-spec.md §12.
 */
@Composable
private fun tomatoContentDescription(state: TimerUiState.Success): String {
    val phase = stringResource(phaseLabelRes(state.slotType, state.status))
    val elapsedPercent = ((1f - state.fillFraction) * 100).toInt().coerceIn(0, 100)
    val progress = stringResource(R.string.a11y_tomato_progress, phase, elapsedPercent)

    return "$progress. ${remainingSpokenText(state)}"
}

@Composable
private fun remainingSpokenText(state: TimerUiState.Success): String {
    val totalSeconds = TimerMath.displaySeconds(state.remainingMs)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val secondsText = pluralStringResource(R.plurals.a11y_seconds, seconds, seconds)

    return if (minutes > 0) {
        val minutesText = pluralStringResource(R.plurals.a11y_minutes, minutes, minutes)
        stringResource(R.string.a11y_remaining_with_minutes, minutesText, secondsText)
    } else {
        stringResource(R.string.a11y_remaining_seconds_only, secondsText)
    }
}

/**
 * Honours the "keep the screen on" setting.
 *
 * Set on the window through the view rather than with a wakelock: the flag is dropped automatically
 * when the window goes away, which a wakelock is not.
 */
@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

private fun previewState(
    status: TimerStatus = TimerStatus.RUNNING,
    slotType: SlotType = SlotType.FOCUS,
    timeText: String = "18:42",
    fillFraction: Float = 0.74f,
    primaryControl: PrimaryControl = PrimaryControl.PAUSE,
) = TimerUiState.Success(
    status = status,
    slotType = slotType,
    timeText = timeText,
    remainingMs = 18 * 60_000L + 42_000L,
    fillFraction = fillFraction,
    primaryControl = primaryControl,
    completedInCycle = 1,
    cyclePosition = 2,
    pomodorosPerCycle = 4,
    keepScreenOn = false,
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 720)
@Composable
private fun TimerScreenRunningPreview() {
    AquiHayTomateTheme {
        TimerContent(previewState(), onPrimaryClick = {}, onResetClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 720)
@Composable
private fun TimerScreenIdlePreview() {
    AquiHayTomateTheme {
        TimerContent(
            previewState(
                status = TimerStatus.IDLE,
                timeText = "25:00",
                fillFraction = 1f,
                primaryControl = PrimaryControl.START,
            ),
            onPrimaryClick = {},
            onResetClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 720)
@Composable
private fun TimerScreenBreakPreview() {
    AquiHayTomateTheme {
        TimerContent(
            previewState(
                slotType = SlotType.SHORT_BREAK,
                timeText = "04:07",
                fillFraction = 0.82f,
            ),
            onPrimaryClick = {},
            onResetClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 720)
@Composable
private fun TimerScreenRingingPreview() {
    AquiHayTomateTheme {
        TimerContent(
            previewState(
                status = TimerStatus.RINGING,
                slotType = SlotType.SHORT_BREAK,
                timeText = "00:00",
                fillFraction = 0f,
                primaryControl = PrimaryControl.START_BREAK,
            ),
            onPrimaryClick = {},
            onResetClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 560)
@Composable
private fun TimerScreenCompactPreview() {
    AquiHayTomateTheme {
        TimerContent(previewState(), onPrimaryClick = {}, onResetClick = {})
    }
}
