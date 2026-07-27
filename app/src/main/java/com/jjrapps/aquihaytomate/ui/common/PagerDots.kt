package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.TextGhost
import com.jjrapps.aquihaytomate.ui.theme.TomateFill

private val DOT_SIZE = 6.dp
private val DOT_GAP = 8.dp

/** The onboarding page indicator. Announced as one phrase, never dot by dot. */
@Composable
fun PagerDots(
    pageCount: Int,
    currentPage: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.clearAndSetSemantics { this.contentDescription = contentDescription }) {
        repeat(pageCount) { page ->
            if (page > 0) Spacer(Modifier.width(DOT_GAP))
            Spacer(
                Modifier
                    .size(DOT_SIZE)
                    .background(if (page == currentPage) TomateFill else TextGhost, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PagerDotsPreview() {
    AquiHayTomateTheme {
        PagerDots(pageCount = 3, currentPage = 1, contentDescription = "Página 2 de 3")
    }
}
