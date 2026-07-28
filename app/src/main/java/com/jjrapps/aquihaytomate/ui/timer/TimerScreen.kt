package com.jjrapps.aquihaytomate.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
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
import com.jjrapps.aquihaytomate.ui.common.phaseNameRes
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.ControlLabelLarge
import com.jjrapps.aquihaytomate.ui.theme.SectionLabelStyle
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.phaseColorsOf

private val TOMATO_DIAMETER = 268.dp
private val TOMATO_DIAMETER_COMPACT = 224.dp
private val COMPACT_HEIGHT_THRESHOLD = 600.dp

private val TOMATO_TO_PHASE = 28.dp
private val PHASE_TO_CONTROL = 20.dp
private val PRIMARY_HEIGHT = 56.dp
private val SCREEN_PADDING = 20.dp
private val DOTS_BOTTOM_MARGIN = 32.dp
private val NEXT_UP_TO_DOTS = 16.dp

/** Reserved whether or not the secondary controls are showing, so the tomato does not jump. */
private val SECONDARY_ROW_HEIGHT = 48.dp

/** Reserved the same way for the "up next" line, which is absent while ringing. */
private val NEXT_UP_HEIGHT = 20.dp

/** Wide enough that RESET and SKIP read as two controls rather than as one long label. */
private val SECONDARY_GAP = 24.dp

/**
 * The free vertical space is split above and below the tomato block. Below weighs more so the block sits
 * a little above the optical centre and the cycle dots settle near the bottom edge, where they belong:
 * they are a status readout, not part of the control cluster.
 */
private const val SPACE_ABOVE_WEIGHT = 1f
private const val SPACE_BELOW_WEIGHT = 1.35f

private const val CONTROL_FADE_MS = 150

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
        onSkipClick = viewModel::onSkipClick,
        modifier = modifier,
    )
}

@Composable
private fun TimerContent(
    state: TimerUiState,
    onPrimaryClick: () -> Unit,
    onResetClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().padding(horizontal = SCREEN_PADDING),
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
                onSkipClick = onSkipClick,
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
    onSkipClick: () -> Unit,
) {
    KeepScreenOn(state.keepScreenOn)

    val colors = phaseColorsOf(state.slotType)
    val primaryLabel = stringResource(state.primaryControl.labelRes)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(SPACE_ABOVE_WEIGHT))

        LiquidCountdown(
            timeText = state.timeText,
            fillFraction = state.fillFraction,
            colors = colors,
            showCalyx = state.showCalyx,
            contentDescription = tomatoContentDescription(state),
            // Tapping the tomato is the primary control: it is by far the biggest target on the screen.
            onClick = onPrimaryClick,
            onClickLabel = primaryLabel,
            modifier = Modifier.size(diameter),
        )

        Spacer(Modifier.height(TOMATO_TO_PHASE))
        PhaseLabel(slotType = state.slotType, status = state.status, accent = colors.bright)

        Spacer(Modifier.height(PHASE_TO_CONTROL))
        TextControl(
            label = primaryLabel,
            onClick = onPrimaryClick,
            glyph = state.primaryControl.glyph,
            style = ControlLabelLarge,
            height = PRIMARY_HEIGHT,
        )

        SecondaryControls(
            showReset = state.showReset,
            showSkip = state.showSkip,
            onResetClick = onResetClick,
            onSkipClick = onSkipClick,
        )

        Spacer(Modifier.weight(SPACE_BELOW_WEIGHT))
        NextUpLine(state.nextSlot)

        Spacer(Modifier.height(NEXT_UP_TO_DOTS))
        CycleDots(
            completed = state.completedInCycle,
            current = state.cyclePosition,
            total = state.pomodorosPerCycle,
            accent = colors.bright,
        )
        Spacer(Modifier.height(DOTS_BOTTOM_MARGIN))
    }
}

/**
 * `REINICIAR` and `SALTAR`, side by side under the primary control.
 *
 * The two of them plus the primary are the three things there are to do with a running pomodoro, and the
 * screen used to offer only two — reset was there, skip was reachable from the notification alone. They
 * share the row rather than stacking so the whole cluster stays one glance wide; both are `controlLabel`
 * in `TextMuted`, a clear step below the primary, because neither is the ordinary thing to do.
 *
 * The row keeps its height even when both are hidden, so the tomato above it never moves.
 */
@Composable
private fun SecondaryControls(
    showReset: Boolean,
    showSkip: Boolean,
    onResetClick: () -> Unit,
    onSkipClick: () -> Unit,
) {
    Row(
        modifier = Modifier.height(SECONDARY_ROW_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(SECONDARY_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = showReset,
            enter = fadeIn(tween(CONTROL_FADE_MS)),
            exit = fadeOut(tween(CONTROL_FADE_MS)),
        ) {
            TextControl(
                label = stringResource(R.string.control_reset),
                onClick = onResetClick,
                color = TextMuted,
            )
        }

        AnimatedVisibility(
            visible = showSkip,
            enter = fadeIn(tween(CONTROL_FADE_MS)),
            exit = fadeOut(tween(CONTROL_FADE_MS)),
        ) {
            TextControl(
                label = stringResource(R.string.control_skip),
                onClick = onSkipClick,
                color = TextMuted,
            )
        }
    }
}

/**
 * `A CONTINUACIÓN: DESCANSO · 5 MIN`, in the same `sectionLabel` as the headers of Settings.
 *
 * It answers the question the old screen left hanging — what happens when this runs out — which matters
 * most right at the end of a slot. Same string and same pure planner as the ongoing notification, so the
 * two cannot disagree. The height is reserved so the dots below do not shift when it goes away.
 */
@Composable
private fun NextUpLine(nextSlot: NextSlot?) {
    Box(
        modifier = Modifier.height(NEXT_UP_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        if (nextSlot != null) {
            val text = stringResource(
                R.string.next_up,
                stringResource(phaseNameRes(nextSlot.type)),
                pluralStringResource(R.plurals.settings_minutes, nextSlot.minutes, nextSlot.minutes),
            )
            // A plain Text rather than the SectionLabel component: that one fills the width for the
            // trailing arrows of the charts, and this line has to sit centred under the tomato.
            Text(text = text.uppercase(), style = SectionLabelStyle, color = TextMuted)
        }
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
    nextSlot: NextSlot? = NextSlot(SlotType.SHORT_BREAK, 5),
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
    nextSlot = nextSlot,
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 720)
@Composable
private fun TimerScreenRunningPreview() {
    AquiHayTomateTheme {
        TimerContent(previewState(), onPrimaryClick = {}, onResetClick = {}, onSkipClick = {})
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
            onSkipClick = {},
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
            onSkipClick = {},
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
            onSkipClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 560)
@Composable
private fun TimerScreenCompactPreview() {
    AquiHayTomateTheme {
        TimerContent(previewState(), onPrimaryClick = {}, onResetClick = {}, onSkipClick = {})
    }
}
