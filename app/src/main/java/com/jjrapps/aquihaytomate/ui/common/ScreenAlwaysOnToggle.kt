package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.ui.theme.AmbarBright
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.SectionLabelStyle
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TomateBright

private val TOUCH_HEIGHT = 48.dp
private val MARK_SIZE = 7.dp
private val MARK_RING_WIDTH = 1.dp
private val MARK_GAP = 8.dp
private const val MAX_LABEL_LINES = 2

/**
 * The "screen always on" button of the Timer screen: a mark and a label, no box, like every other control.
 *
 * Off it is a hollow ring and `PANTALLA` in `TextMuted`, the ink of a readout that is just there. On it
 * fills with the phase accent and the label grows into `PANTALLA ENCENDIDA` in the same accent, so the
 * state is told three ways — shape, colour and words — and never by colour alone.
 *
 * The label may wrap to two lines: a twelve-pomodoro cycle leaves little room beside the dots.
 */
@Composable
fun ScreenAlwaysOnToggle(
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = if (enabled) accent else TextMuted
    val label = stringResource(
        if (enabled) R.string.screen_always_on_label_on else R.string.screen_always_on_label_off,
    )
    val description = stringResource(R.string.a11y_screen_always_on)

    Row(
        modifier = modifier
            .heightIn(min = TOUCH_HEIGHT)
            .toggleable(
                value = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Switch,
                onValueChange = { onClick() },
            )
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            Modifier.size(MARK_SIZE).drawBehind {
                if (enabled) {
                    drawCircle(ink)
                } else {
                    val stroke = MARK_RING_WIDTH.toPx()
                    drawCircle(ink, radius = (size.minDimension - stroke) / 2f, style = Stroke(stroke))
                }
            },
        )
        Spacer(Modifier.width(MARK_GAP))
        Text(
            text = label.uppercase(),
            style = SectionLabelStyle,
            color = ink,
            textAlign = TextAlign.End,
            maxLines = MAX_LABEL_LINES,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ScreenAlwaysOnToggleOffPreview() {
    AquiHayTomateTheme {
        ScreenAlwaysOnToggle(enabled = false, accent = TomateBright, onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ScreenAlwaysOnToggleOnPreview() {
    AquiHayTomateTheme {
        ScreenAlwaysOnToggle(enabled = true, accent = TomateBright, onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ScreenAlwaysOnToggleOnBreakPreview() {
    AquiHayTomateTheme {
        ScreenAlwaysOnToggle(enabled = true, accent = AmbarBright, onClick = {})
    }
}
