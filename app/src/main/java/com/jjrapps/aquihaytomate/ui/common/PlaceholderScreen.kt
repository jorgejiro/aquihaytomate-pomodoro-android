package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.ui.theme.SectionLabelStyle
import com.jjrapps.aquihaytomate.ui.theme.TextMuted

/**
 * Scaffolding only. Every screen that still has no content shows this so the navigation graph
 * can be wired and exercised before the features land. Delete each usage as its phase completes.
 */
@Composable
fun PlaceholderScreen(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = SectionLabelStyle,
            color = TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}
