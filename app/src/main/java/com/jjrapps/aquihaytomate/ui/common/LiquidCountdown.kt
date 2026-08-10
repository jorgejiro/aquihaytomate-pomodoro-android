package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.domain.model.SlotType
import com.jjrapps.aquihaytomate.domain.render.TomatoGeometry
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.DisplayTimer
import com.jjrapps.aquihaytomate.ui.theme.PhaseColors
import com.jjrapps.aquihaytomate.ui.theme.TextOnLiquid
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.phaseColorsOf

/** Compensates the empty descender of the digits so the figure looks optically centred. */
private val DIGITS_OPTICAL_OFFSET = (-2).dp

/**
 * The tomato with the countdown knocked out of it: the figure reads light over the hollow and dark over
 * the liquid, with the boundary following the wave.
 *
 * No text `Path` extraction involved. Two `Text` layers with the same style inside two containers of
 * the same size and alignment therefore share a layout, which is what makes the register exact; the
 * lower one is clipped to the liquid. See docs/design-spec.md §6.3.
 *
 * @param contentDescription read instead of the raw `18:42`, which a screen reader would spell out as
 *   a ratio.
 * @param onClick invoked when the tomato itself is tapped; it is the same action as the primary control,
 *   so the whole 268 dp circle is a tap target rather than only the label under it.
 * @param onClickLabel what a screen reader announces the tap does, e.g. "pause".
 */
@Composable
fun LiquidCountdown(
    timeText: String,
    fillFraction: Float,
    colors: PhaseColors,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
) {
    val phases = rememberLiquidPhases()
    val clipPathHolder = remember { Path() }
    val interactionSource = remember { MutableInteractionSource() }
    // Held in a local so the semantics block below can call it without shadowing `onClick` there.
    val clickAction = onClick

    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (clickAction == null) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = clickAction,
                    )
                },
            )
            // The action is declared here as well as on `clickable`: this block replaces the semantics of
            // the whole subtree, so a bare `clickable` above it would not survive it.
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                if (clickAction != null) {
                    this.role = Role.Button
                    onClick(label = onClickLabel) {
                        clickAction()
                        true
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        LiquidTomato(
            fillFraction = fillFraction,
            colors = colors,
            phases = phases,
            modifier = Modifier.fillMaxSize(),
        )

        Digits(timeText, TextPrimary)

        // Same box, same size, same alignment as the layer above, so the glyphs land pixel for pixel.
        // The phase is read inside the draw lambda, so this node repaints per frame without
        // recomposing — the same rule as LiquidTomato.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val path = buildLiquidPath(
                        path = clipPathHolder,
                        fillFraction = fillFraction,
                        phaseRad = phases.front.value,
                        periods = TomatoGeometry.PERIODS_FRONT,
                    )
                    if (path.isEmpty) return@drawWithContent
                    clipPath(path) { this@drawWithContent.drawContent() }
                },
            contentAlignment = Alignment.Center,
        ) {
            Digits(timeText, TextOnLiquid)
        }
    }
}

@Composable
private fun Digits(timeText: String, color: Color) {
    Text(
        text = timeText,
        style = DisplayTimer,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.offset(y = DIGITS_OPTICAL_OFFSET),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LiquidCountdownPreview() {
    AquiHayTomateTheme {
        LiquidCountdown(
            timeText = "18:42",
            fillFraction = 0.74f,
            colors = phaseColorsOf(SlotType.FOCUS),
            contentDescription = "18 minutes and 42 seconds remaining",
            modifier = Modifier.size(268.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun LiquidCountdownBreakPreview() {
    AquiHayTomateTheme {
        LiquidCountdown(
            timeText = "04:07",
            fillFraction = 0.82f,
            colors = phaseColorsOf(SlotType.SHORT_BREAK),
            contentDescription = "4 minutes and 7 seconds remaining",
            modifier = Modifier.size(268.dp),
        )
    }
}
