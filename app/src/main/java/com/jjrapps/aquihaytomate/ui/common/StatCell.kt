package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.Caption
import com.jjrapps.aquihaytomate.ui.theme.DisplayStat
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TomateBright

private val VALUE_TO_LABEL = 4.dp

/**
 * One of the three "Today" columns: a big figure with a small label under it.
 *
 * Announced as a single phrase, so a screen reader says "8 pomodoros" instead of reading the number and
 * the word as two unrelated nodes.
 */
@Composable
fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TomateBright,
) {
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$value $label" },
        horizontalAlignment = Alignment.Start,
    ) {
        Text(text = value, style = DisplayStat, color = valueColor)
        Spacer(Modifier.height(VALUE_TO_LABEL))
        Text(text = label, style = Caption, color = TextMuted)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun StatCellPreview() {
    AquiHayTomateTheme {
        StatCell(value = "8", label = "pomodoros")
    }
}
