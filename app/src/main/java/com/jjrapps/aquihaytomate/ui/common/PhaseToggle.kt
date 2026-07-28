package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.BorderStrong
import com.jjrapps.aquihaytomate.ui.theme.SurfacePressed
import com.jjrapps.aquihaytomate.ui.theme.TextGhost
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.TomateFill

private val TRACK_WIDTH = 40.dp
private val TRACK_HEIGHT = 22.dp
private val THUMB_SIZE = 16.dp
private val THUMB_INSET = 3.dp

/**
 * The project's own switch, 40 × 22 dp.
 *
 * Not a Material `Switch`: that one is 52 × 32 dp with its own ripple and elevation, and it dominates a
 * 52 dp settings row. Purely visual — the row around it owns the click and the semantics, so this never
 * becomes a second tap target for the same setting.
 */
@Composable
fun PhaseToggle(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TRACK_WIDTH - THUMB_SIZE - THUMB_INSET else THUMB_INSET,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "toggle_thumb",
    )

    Box(
        modifier = modifier
            .size(width = TRACK_WIDTH, height = TRACK_HEIGHT)
            .clip(RoundedCornerShape(TRACK_HEIGHT / 2))
            .background(if (checked) TomateFill else SurfacePressed)
            .then(
                if (checked) {
                    Modifier
                } else {
                    Modifier.border(1.dp, BorderStrong, RoundedCornerShape(TRACK_HEIGHT / 2))
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                // Lambda overload: the spring is read in the layout phase, so the thumb slides without
                // recomposing the row it sits in.
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .size(THUMB_SIZE)
                .background(if (checked) TextPrimary else TextGhost, CircleShape),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PhaseTogglePreview() {
    AquiHayTomateTheme {
        Box(Modifier.size(80.dp, 40.dp), contentAlignment = Alignment.Center) {
            PhaseToggle(checked = true)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PhaseToggleOffPreview() {
    AquiHayTomateTheme {
        Box(Modifier.size(80.dp, 40.dp), contentAlignment = Alignment.Center) {
            PhaseToggle(checked = false)
        }
    }
}
