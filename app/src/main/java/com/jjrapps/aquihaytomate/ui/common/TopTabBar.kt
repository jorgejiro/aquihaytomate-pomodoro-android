package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.ui.navigation.TopTab
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.BackgroundVoid
import com.jjrapps.aquihaytomate.ui.theme.TabLabel
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.TomateBright

private val BAR_HEIGHT = 48.dp
private val INDICATOR_WIDTH = 16.dp
private val INDICATOR_HEIGHT = 2.dp

/**
 * Three text tabs with an animated underline. Deliberately not a Material `TabRow`: that brings
 * its own paddings, ripple and indicator, all of which fight docs/design-spec.md §5.0.
 *
 * Holds the status bar inset itself, so the black plate reaches behind the clock while the labels sit
 * below it. `Scaffold` does not insert it: only Material's own app bars consume that inset, and this bar
 * is deliberately not one of them.
 *
 * @param accent colour of the underline; follows the current phase.
 */
@Composable
fun TopTabBar(
    selected: TopTab,
    onSelect: (TopTab) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = TomateBright,
) {
    val tabs = TopTab.entries
    var barWidthPx by remember { mutableIntStateOf(0) }

    // With N equally weighted slots, the centre of slot i sits at (2i + 1) / 2N of the width.
    val centreFraction = (tabs.indexOf(selected) * 2 + 1) / (tabs.size * 2f)
    val targetX = with(LocalDensity.current) {
        (barWidthPx * centreFraction).toDp() - INDICATOR_WIDTH / 2
    }
    val indicatorX by animateDpAsState(
        targetValue = targetX,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "tab_indicator",
    )

    Box(
        modifier
            .fillMaxWidth()
            .background(BackgroundVoid)
            .statusBarsPadding()
            .height(BAR_HEIGHT)
            .onSizeChanged { barWidthPx = it.width },
    ) {
        Row(Modifier.fillMaxWidth().height(BAR_HEIGHT)) {
            tabs.forEach { tab ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(BAR_HEIGHT)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(tab.labelRes).uppercase(),
                        style = TabLabel,
                        color = if (tab == selected) TextPrimary else TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                // Lambda overload: the animated value is read in the layout phase, so the indicator
                // slides without recomposing the bar on every frame.
                .offset { IntOffset(indicatorX.roundToPx(), 0) }
                .size(width = INDICATOR_WIDTH, height = INDICATOR_HEIGHT)
                .background(accent, RoundedCornerShape(1.dp)),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360)
@Composable
private fun TopTabBarPreview() {
    AquiHayTomateTheme {
        TopTabBar(selected = TopTab.TIMER, onSelect = {})
    }
}
