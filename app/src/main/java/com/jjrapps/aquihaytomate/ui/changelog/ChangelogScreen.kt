package com.jjrapps.aquihaytomate.ui.changelog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.ui.common.HairlineDivider
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.BodyDefault
import com.jjrapps.aquihaytomate.ui.theme.Caption
import com.jjrapps.aquihaytomate.ui.theme.NumberSmall
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.TextSecondary
import com.jjrapps.aquihaytomate.ui.theme.TitleScreen
import com.jjrapps.aquihaytomate.ui.theme.TomateBright
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val SCREEN_PADDING = 20.dp
private val TITLE_TOP = 20.dp
private val RELEASE_GAP = 24.dp
private val BULLET_GAP = 8.dp

@Composable
fun ChangelogScreen(
    modifier: Modifier = Modifier,
    viewModel: ChangelogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ChangelogContent(state, modifier)
}

@Composable
private fun ChangelogContent(state: ChangelogUiState, modifier: Modifier = Modifier) {
    when (state) {
        ChangelogUiState.Loading -> Box(modifier.fillMaxSize())
        is ChangelogUiState.Success -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SCREEN_PADDING),
        ) {
            Spacer(Modifier.height(TITLE_TOP))
            Text(
                text = stringResource(R.string.changelog_title),
                style = TitleScreen,
                color = TextPrimary,
            )

            state.releases.forEach { release ->
                Spacer(Modifier.height(RELEASE_GAP))
                Release(
                    release = release,
                    isInstalled = release.versionCode == state.installedVersionCode,
                )
            }

            Spacer(Modifier.height(RELEASE_GAP * 2))
        }
    }
}

@Composable
private fun Release(release: ChangelogRelease, isInstalled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = release.versionName, style = NumberSmall, color = TextPrimary)
        Spacer(Modifier.width(8.dp))
        Text(
            text = release.releaseDate.format(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
            ),
            style = Caption,
            color = TextMuted,
        )
        if (isInstalled) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.changelog_current_badge).uppercase(),
                style = Caption,
                color = TomateBright,
            )
        }
    }

    Spacer(Modifier.height(BULLET_GAP))
    HairlineDivider()
    Spacer(Modifier.height(BULLET_GAP))

    stringArrayResource(release.highlightsRes).forEach { highlight ->
        Row(Modifier.fillMaxWidth()) {
            Text(text = "·", style = BodyDefault, color = TomateBright)
            Spacer(Modifier.width(8.dp))
            Text(text = highlight, style = BodyDefault, color = TextSecondary)
        }
        Spacer(Modifier.height(BULLET_GAP))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 360, heightDp = 700)
@Composable
private fun ChangelogScreenPreview() {
    AquiHayTomateTheme {
        ChangelogContent(
            ChangelogUiState.Success(
                releases = ChangelogCatalog.releases,
                installedVersionCode = 1,
            ),
        )
    }
}
