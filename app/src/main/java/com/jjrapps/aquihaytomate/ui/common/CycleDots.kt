package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.NumberSmall
import com.jjrapps.aquihaytomate.ui.theme.TextGhost
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TomateBright

private val DOT_SIZE = 7.dp
private val DOT_GAP = 10.dp
private val COUNTER_GAP = 12.dp
private val CURRENT_RING_WIDTH = 1.5.dp
private val PENDING_RING_WIDTH = 1.dp
private const val CURRENT_FILL_ALPHA = 0.25f

/**
 * Where we are in the cycle: filled dots for pomodoros already done, a ring for the current one, empty
 * rings for the ones to come, and `2/4` alongside.
 *
 * Announced as a whole ("pomodoro 2 of 4") rather than dot by dot, which is the only way this is
 * usable with a screen reader.
 */
@Composable
fun CycleDots(
    completed: Int,
    current: Int,
    total: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val safeTotal = total.coerceAtLeast(1)
    val description = stringResource(R.string.a11y_cycle_position, current.coerceAtLeast(1), safeTotal)

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(safeTotal) { index ->
            if (index > 0) Spacer(Modifier.width(DOT_GAP))
            Dot(
                state = when {
                    index < completed -> DotState.DONE
                    index == completed -> DotState.CURRENT
                    else -> DotState.PENDING
                },
                accent = accent,
            )
        }
        Spacer(Modifier.width(COUNTER_GAP))
        Text(
            text = stringResource(R.string.cycle_progress, current.coerceAtLeast(1), safeTotal),
            style = NumberSmall,
            color = TextMuted,
        )
    }
}

private enum class DotState { DONE, CURRENT, PENDING }

@Composable
private fun Dot(state: DotState, accent: Color) {
    Spacer(
        Modifier.size(DOT_SIZE).drawBehind {
            val radius = size.minDimension / 2f
            when (state) {
                DotState.DONE -> drawCircle(accent, radius)

                DotState.CURRENT -> {
                    val stroke = CURRENT_RING_WIDTH.toPx()
                    drawCircle(accent.copy(alpha = CURRENT_FILL_ALPHA), radius - stroke)
                    drawCircle(accent, radius - stroke / 2f, style = Stroke(stroke))
                }

                DotState.PENDING -> {
                    val stroke = PENDING_RING_WIDTH.toPx()
                    drawCircle(TextGhost, radius - stroke / 2f, style = Stroke(stroke))
                }
            }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CycleDotsPreview() {
    AquiHayTomateTheme {
        CycleDots(completed = 2, current = 3, total = 4, accent = TomateBright)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CycleDotsFirstPreview() {
    AquiHayTomateTheme {
        CycleDots(completed = 0, current = 1, total = 4, accent = TomateBright)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CycleDotsLongCyclePreview() {
    AquiHayTomateTheme {
        CycleDots(completed = 5, current = 6, total = 8, accent = TomateBright)
    }
}
