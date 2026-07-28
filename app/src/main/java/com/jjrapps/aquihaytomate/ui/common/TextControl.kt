package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.ControlLabel
import com.jjrapps.aquihaytomate.ui.theme.ControlLabelLarge
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary

private val TOUCH_HEIGHT = 48.dp
private val GLYPH_GAP = 10.dp

/** The glyph is drawn at this fraction of the label size, so it grows with the style rather than apart. */
private const val GLYPH_TO_FONT_RATIO = 0.72f
private val GLYPH_FALLBACK_SIZE = 11.dp

/**
 * The glyph beside a control label.
 *
 * Drawn rather than typed: `▸` and `❚❚` are not in every font that could end up standing in for Inter,
 * and a missing-glyph box in the primary control would be the ugliest bug in the app.
 */
enum class ControlGlyph { NONE, PLAY, PAUSE }

/**
 * A control with no box at all: a glyph, a label, a 48 dp touch target and an unbounded ripple.
 *
 * "If you are unsure whether to put a box around it or not, do not" — docs/design-spec.md §1. A `Text`
 * with a 48 dp touch target is a perfectly good button.
 */
@Composable
fun TextControl(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: ControlGlyph = ControlGlyph.NONE,
    color: Color = TextPrimary,
    style: TextStyle = ControlLabel,
    height: Dp = TOUCH_HEIGHT,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .height(height)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (glyph != ControlGlyph.NONE) {
            Glyph(glyph, color, glyphSizeFor(style))
            Spacer(Modifier.width(GLYPH_GAP))
        }
        Text(
            text = label.uppercase(),
            style = style,
            color = color,
        )
    }
}

/** Keeps the glyph in proportion with whatever style the caller passed. */
@Composable
private fun glyphSizeFor(style: TextStyle): Dp {
    val fontSize = style.fontSize
    if (!fontSize.isSp) return GLYPH_FALLBACK_SIZE
    return with(LocalDensity.current) { fontSize.toDp() * GLYPH_TO_FONT_RATIO }
}

@Composable
private fun Glyph(glyph: ControlGlyph, color: Color, glyphSize: Dp) {
    val path = remember { Path() }
    Spacer(
        Modifier.size(glyphSize).drawBehind {
            when (glyph) {
                ControlGlyph.PLAY -> {
                    path.rewind()
                    path.moveTo(0f, 0f)
                    path.lineTo(size.width, size.height / 2f)
                    path.lineTo(0f, size.height)
                    path.close()
                    drawPath(path, color)
                }

                ControlGlyph.PAUSE -> {
                    val barWidth = size.width * 0.32f
                    drawRect(color, Offset.Zero, Size(barWidth, size.height))
                    drawRect(
                        color,
                        Offset(size.width - barWidth, 0f),
                        Size(barWidth, size.height),
                    )
                }

                ControlGlyph.NONE -> Unit
            }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TextControlStartPreview() {
    AquiHayTomateTheme {
        TextControl(
            label = "Iniciar",
            onClick = {},
            glyph = ControlGlyph.PLAY,
            style = ControlLabelLarge,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TextControlPausePreview() {
    AquiHayTomateTheme {
        TextControl(
            label = "Pausar",
            onClick = {},
            glyph = ControlGlyph.PAUSE,
            style = ControlLabelLarge,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TextControlSecondaryPreview() {
    AquiHayTomateTheme {
        TextControl(label = "Reiniciar", onClick = {}, color = TextMuted)
    }
}
