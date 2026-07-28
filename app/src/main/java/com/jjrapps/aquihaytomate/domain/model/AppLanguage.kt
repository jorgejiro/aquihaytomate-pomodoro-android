package com.jjrapps.aquihaytomate.domain.model

import androidx.annotation.StringRes
import com.jjrapps.aquihaytomate.R

/**
 * UI language override. [AUTO] means "follow the system", which is the only value that leaves the
 * per-app locale list empty; the others map onto a BCP-47 tag handed to
 * `AppCompatDelegate.setApplicationLocales`.
 */
enum class AppLanguage(val id: String, val tag: String?, @param:StringRes val labelRes: Int) {
    AUTO("auto", null, R.string.language_auto),
    SPANISH("es", "es", R.string.language_spanish),
    ENGLISH("en", "en", R.string.language_english),
    ;

    companion object {
        val DEFAULT = AUTO

        fun fromId(id: String?): AppLanguage = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
