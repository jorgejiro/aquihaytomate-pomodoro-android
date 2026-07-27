package com.jjrapps.aquihaytomate.ui.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.BodyDefault
import com.jjrapps.aquihaytomate.ui.theme.TextSecondary
import com.jjrapps.aquihaytomate.ui.theme.TitleScreen
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary

private val SCREEN_PADDING = 20.dp

/**
 * The OFL notice for the two bundled font families.
 *
 * Read straight out of `res/raw` rather than pasted into a string resource: it is a licence text that must
 * be reproduced verbatim, and a translator has no business touching it.
 */
@Composable
fun LicensesScreen(modifier: Modifier = Modifier) {
    // LocalResources rather than LocalContext.current.resources: the former recomposes on a
    // configuration change, so switching language would not leave a stale reader behind.
    val resources = LocalResources.current
    val text = remember(resources) {
        resources.openRawResource(R.raw.licenses_ofl).bufferedReader().use { it.readText() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SCREEN_PADDING),
    ) {
        Spacer(Modifier.height(SCREEN_PADDING))
        Text(
            text = stringResource(R.string.settings_licenses),
            style = TitleScreen,
            color = TextPrimary,
        )
        Spacer(Modifier.height(SCREEN_PADDING))
        Text(text = text, style = BodyDefault, color = TextSecondary)
        Spacer(Modifier.height(SCREEN_PADDING * 2))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 700)
@Composable
private fun LicensesScreenPreview() {
    AquiHayTomateTheme {
        LicensesScreen()
    }
}
