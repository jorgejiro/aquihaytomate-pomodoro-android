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
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.phaseColorsOf

private val TOMATO_DIAMETER = 268.dp
private val TOMATO_DIAMETER_COMPACT = 224.dp
private val COMPACT_HEIGHT_THRESHOLD = 600.dp

private val TOMATO_TO_PHASE = 28.dp
private val PHASE_TO_CONTROL = 20.dp
private val PRIMARY_HEIGHT = 56.dp

/**
 * Minimum air above and below the control cluster in portrait, for when there is no free height to
 * distribute. It is half of [PHASE_TO_CONTROL] on each side, so on a screen with nothing to spare the
 * spacing ends up exactly as it was before the cluster started floating.
 */
private val CONTROLS_MIN_GAP = 10.dp
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
 * Landscape sizing. The tomato takes this much of the height it is offered — enough to stay the biggest
 * thing on screen, with room left for the phase label to breathe — and never grows past the portrait
 * diameter, so turning the phone does not make it bigger.
 */
private const val LANDSCAPE_TOMATO_FRACTION = 0.84f
private val LANDSCAPE_TOMATO_MIN = 140.dp
private val LANDSCAPE_COLUMN_GAP = 24.dp

/** Replaces the elastic weight of portrait: there is no spare height to distribute. */
private val LANDSCAPE_CONTROLS_TO_NEXT = 20.dp

/**
 * How the free vertical space is spent in portrait.
 *
 * [SPACE_ABOVE_WEIGHT] goes above the tomato, and the rest is split **evenly above and below the control
 * cluster**, which is what centres `PAUSAR` / `REINICIAR` / `SALTAR` in the gap between the fruit and the
 * "up next" line.
 *
 * Until 1.3 the controls hung directly off the phase label and the whole remainder piled up underneath
 * them, so on a tall phone the cluster was crammed against the tomato with a hole below it. The two
 * weights add up to the old single one, so the tomato does not move — only the controls do.
 */
private const val SPACE_ABOVE_WEIGHT = 1f
private const val CONTROLS_SLACK_WEIGHT = 0.675f

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
        val landscape = maxWidth > maxHeight
        val diameter = when {
            // Landscape has width to spare and no height at all, so the tomato is sized off the height
            // it is given rather than off a fixed number, and the block turns into two columns.
            landscape -> (maxHeight * LANDSCAPE_TOMATO_FRACTION)
                .coerceIn(LANDSCAPE_TOMATO_MIN, TOMATO_DIAMETER)
            // 224 dp on a short screen, so the whole block still fits without clipping. See §5.1.
            maxHeight < COMPACT_HEIGHT_THRESHOLD -> TOMATO_DIAMETER_COMPACT
            else -> TOMATO_DIAMETER
        }

        when (state) {
            TimerUiState.Loading -> Unit
            is TimerUiState.Success -> {
                KeepScreenOn(state.keepScreenOn)

                if (landscape) {
                    TimerBlockLandscape(
                        state = state,
                        diameter = diameter,
                        onPrimaryClick = onPrimaryClick,
                        onResetClick = onResetClick,
                        onSkipClick = onSkipClick,
                    )
                } else {
                    TimerBlock(
                        state = state,
                        diameter = diameter,
                        onPrimaryClick = onPrimaryClick,
                        onResetClick = onResetClick,
                        onSkipClick = onSkipClick,
                    )
                }
            }
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
            contentDescription = tomatoContentDescription(state),
            // Tapping the tomato is the primary control: it is by far the biggest target on the screen.
            onClick = onPrimaryClick,
            onClickLabel = primaryLabel,
            modifier = Modifier.size(diameter),
        )

        Spacer(Modifier.height(TOMATO_TO_PHASE))
        PhaseLabel(slotType = state.slotType, status = state.status, accent = colors.bright)

        // Equal slack on both sides: the cluster floats in the middle of what is left between the fruit
        // and the "up next" line instead of hanging off the phase label.
        Spacer(Modifier.weight(CONTROLS_SLACK_WEIGHT))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = CONTROLS_MIN_GAP),
        ) {
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
        }
        Spacer(Modifier.weight(CONTROLS_SLACK_WEIGHT))

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
 * The same block laid out as two columns, for landscape.
 *
 * A phone on its side has around 370 dp of height, and the portrait column needs more than that: the
 * tomato alone eats two thirds of it, and `SALTAR` ended up off the bottom of the screen — the bug this
 * fixes. Two columns spend the width that landscape does have instead, and the tomato stays the biggest
 * thing on screen, which is the whole design.
 *
 * The right column is centred on the tomato rather than pinned to the bottom edge: the cycle dots go
 * with the controls here, because there is no "bottom of the screen" far enough away to make them read
 * as a separate status readout the way they do in portrait.
 */
@Composable
private fun TimerBlockLandscape(
    state: TimerUiState.Success,
    diameter: Dp,
    onPrimaryClick: () -> Unit,
    onResetClick: () -> Unit,
    onSkipClick: () -> Unit,
) {
    val colors = phaseColorsOf(state.slotType)
    val primaryLabel = stringResource(state.primaryControl.labelRes)

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LANDSCAPE_COLUMN_GAP),
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            LiquidCountdown(
                timeText = state.timeText,
                fillFraction = state.fillFraction,
                colors = colors,
                contentDescription = tomatoContentDescription(state),
                onClick = onPrimaryClick,
                onClickLabel = primaryLabel,
                modifier = Modifier.size(diameter),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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

            Spacer(Modifier.height(LANDSCAPE_CONTROLS_TO_NEXT))
            NextUpLine(state.nextSlot)

            Spacer(Modifier.height(NEXT_UP_TO_DOTS))
            CycleDots(
                completed = state.completedInCycle,
                current = state.cyclePosition,
                total = state.pomodorosPerCycle,
                accent = colors.bright,
            )
        }
    }
}

/**
 * `REINICIAR` and `SALTAR`, side by side under the primary control.
 *
 * The two of them plus the primary are the three things there are to do with a running pomodoro, and the
 * screen used to offer only two — reset was there, skip was reachable from the notification alone. They
 * share the row rather than stacking so the whole cluster stays one glance wide; both are `controlLabel`
 * in `TextPrimary`, the same ink as the primary. `TextMuted` read as disabled — a greyed-out label in a
 * screen with no boxes has nothing else to say "you can press me". The step below the primary is carried
 * by size and by the glyph, which is enough.
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
                color = TextPrimary,
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
                color = TextPrimary,
            )
        }
    }
}

/**
 * `SIGUIENTE: DESCANSO · 5 MIN`, in the same `sectionLabel` as the headers of Settings.
 *
 * It answers the question the old screen left hanging — what happens when this runs out — which matters
 * most right at the end of a slot. Fed by the same pure planner as the ongoing notification, so the two
 * cannot disagree about what is coming. The height is reserved so the dots below do not shift when it
 * goes away.
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
 * Honours the "keep the screen on" setting, already resolved against the charger by the ViewModel.
 *
 * Set on the window through the view rather than with a wakelock: the flag is dropped automatically
 * when the window goes away, which a wakelock is not. `onDispose` clearing it is also what confines the
 * whole thing to this screen — leave the Timer tab and the display goes back to its normal timeout.
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

/** A phone on its side, which is where `SALTAR` used to fall off the bottom of the screen. */
@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 900, heightDp = 370)
@Composable
private fun TimerScreenLandscapePreview() {
    AquiHayTomateTheme {
        TimerContent(previewState(), onPrimaryClick = {}, onResetClick = {}, onSkipClick = {})
    }
}
