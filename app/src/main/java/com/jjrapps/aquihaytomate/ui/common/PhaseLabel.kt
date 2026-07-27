package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.model.TimerStatus
import com.jjrapps.aquihaytomate.ui.theme.AlertAmber
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.LocalReducedMotion
import com.jjrapps.aquihaytomate.ui.theme.PhaseLabelStyle
import com.jjrapps.aquihaytomate.ui.theme.TomateBright

private const val BLINK_PERIOD_MS = 900
private const val BLINK_MIN_ALPHA = 0.45f

/**
 * `FOCUS` / `BREAK` / `LONG BREAK`, replaced by `TIME'S UP!` while ringing.
 *
 * The label is always present, which is what keeps the phase from being signalled by colour alone —
 * see docs/design-spec.md §2.4. The strings live in their natural form in `strings.xml` and the caps
 * are applied here: shouting in the XML would break translation and screen readers.
 */
@Composable
fun PhaseLabel(
    slotType: SlotType,
    status: TimerStatus,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val ringing = status == TimerStatus.RINGING
    val labelRes = phaseLabelRes(slotType, status)

    Text(
        text = stringResource(labelRes).uppercase(),
        style = PhaseLabelStyle,
        color = if (ringing) AlertAmber else accent,
        modifier = modifier.alpha(if (ringing) blinkAlpha() else 1f),
    )
}

/**
 * The phase name for a state. Shared with the tomato's `contentDescription`, so the label a sighted
 * user reads and the one a screen reader announces cannot diverge.
 */
@StringRes
fun phaseLabelRes(slotType: SlotType, status: TimerStatus): Int =
    if (status == TimerStatus.RINGING) {
        R.string.phase_ringing
    } else {
        when (slotType) {
            SlotType.FOCUS -> R.string.phase_focus
            SlotType.SHORT_BREAK -> R.string.phase_short_break
            SlotType.LONG_BREAK -> R.string.phase_long_break
        }
    }

/** Fixed alpha with reduced motion: the blink is decoration, the text carries the information. */
@Composable
private fun blinkAlpha(): Float {
    if (LocalReducedMotion.current) return 1f

    val transition = rememberInfiniteTransition(label = "ringing")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = BLINK_MIN_ALPHA,
        animationSpec = infiniteRepeatable(
            animation = tween(BLINK_PERIOD_MS),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringingAlpha",
    )
    return alpha
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PhaseLabelFocusPreview() {
    AquiHayTomateTheme {
        PhaseLabel(SlotType.FOCUS, TimerStatus.RUNNING, TomateBright)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PhaseLabelRingingPreview() {
    AquiHayTomateTheme {
        PhaseLabel(SlotType.SHORT_BREAK, TimerStatus.RINGING, AlertAmber)
    }
}
