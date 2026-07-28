package com.jjrapps.aquihaytomate.ui.changelog

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every catalog entry must resolve to a non-empty highlights array in both shipped locales.
 * Catches the classic mistake of bumping the version and translating only one of them.
 */
@RunWith(AndroidJUnit4::class)
class ChangelogResourcesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun everyReleaseHasHighlightsInEveryLocale() {
        listOf("en", "es").forEach { language ->
            val localized = context.localizedFor(language)
            ChangelogCatalog.releases.forEach { release ->
                val items = localized.resources.getStringArray(release.highlightsRes)
                assertTrue(
                    "Empty changelog array for ${release.versionName} in '$language'",
                    items.isNotEmpty(),
                )
                assertTrue(
                    "Blank changelog item for ${release.versionName} in '$language'",
                    items.none { it.isBlank() },
                )
            }
        }
    }

    private fun Context.localizedFor(language: String): Context {
        val config = Configuration(resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        return createConfigurationContext(config)
    }
}
