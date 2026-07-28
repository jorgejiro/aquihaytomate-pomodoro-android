package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.ui.theme.BorderHair

/**
 * A 1 dp rule.
 *
 * Not `HorizontalDivider`: Material's version brings its own colour and insets, and the whole point of
 * this project's look is that the separators are barely there. Inset 14 dp inside settings groups, full
 * width between sections. See docs/design-spec.md §4.
 */
@Composable
fun HairlineDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(1.dp)
            .background(BorderHair),
    )
}
