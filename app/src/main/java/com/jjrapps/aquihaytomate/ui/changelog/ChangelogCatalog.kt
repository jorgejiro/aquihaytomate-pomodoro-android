package com.jjrapps.aquihaytomate.ui.changelog

import androidx.annotation.ArrayRes
import com.jjrapps.aquihaytomate.R
import java.time.LocalDate

/**
 * A released version of the app. The highlights live in a localized string array so the
 * changelog is translated like any other UI text; the screen resolves [highlightsRes].
 */
data class ChangelogRelease(
    val versionName: String,
    val versionCode: Int,
    val releaseDate: LocalDate,
    @param:ArrayRes val highlightsRes: Int,
)

/**
 * Static catalog of releases, newest first.
 *
 * When bumping the version in `build.gradle.kts`, add an entry here, the matching `string-array`
 * in `values/strings.xml` and `values-es/strings.xml`, and a section in `CHANGELOG.md`.
 * `ChangelogCatalogTest` fails if this catalog and the compiled version drift apart.
 */
object ChangelogCatalog {

    val releases: List<ChangelogRelease> = listOf(
        ChangelogRelease(
            versionName = "1.4.0",
            versionCode = 8,
            releaseDate = LocalDate.of(2026, 8, 24),
            highlightsRes = R.array.changelog_1_4_0,
        ),
        ChangelogRelease(
            versionName = "1.3.1",
            versionCode = 7,
            releaseDate = LocalDate.of(2026, 8, 18),
            highlightsRes = R.array.changelog_1_3_1,
        ),
        ChangelogRelease(
            versionName = "1.3.0",
            versionCode = 6,
            releaseDate = LocalDate.of(2026, 8, 10),
            highlightsRes = R.array.changelog_1_3_0,
        ),
        ChangelogRelease(
            versionName = "1.2.0",
            versionCode = 5,
            releaseDate = LocalDate.of(2026, 8, 10),
            highlightsRes = R.array.changelog_1_2_0,
        ),
        ChangelogRelease(
            versionName = "1.1.0",
            versionCode = 4,
            releaseDate = LocalDate.of(2026, 7, 29),
            highlightsRes = R.array.changelog_1_1_0,
        ),
        ChangelogRelease(
            versionName = "1.0.1",
            versionCode = 3,
            releaseDate = LocalDate.of(2026, 7, 29),
            highlightsRes = R.array.changelog_1_0_1,
        ),
        ChangelogRelease(
            versionName = "1.0.0",
            versionCode = 2,
            releaseDate = LocalDate.of(2026, 7, 28),
            highlightsRes = R.array.changelog_1_0_0,
        ),
    )
}
