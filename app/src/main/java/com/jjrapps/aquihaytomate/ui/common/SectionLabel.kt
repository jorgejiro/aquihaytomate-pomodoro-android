package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.SectionLabelStyle
import com.jjrapps.aquihaytomate.ui.theme.TextMuted

/**
 * A section heading, optionally with controls on the right — the `‹ ›` arrows of the charts.
 *
 * Upper case is applied here, not in `strings.xml`: shouting in the XML breaks translation and screen
 * readers.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = text.uppercase(), style = SectionLabelStyle, color = TextMuted)
        trailing?.invoke()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun SectionLabelPreview() {
    AquiHayTomateTheme {
        SectionLabel("Esta semana")
    }
}
