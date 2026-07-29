package com.jjrapps.aquihaytomate.domain.model

import androidx.annotation.StringRes
import com.jjrapps.aquihaytomate.R

/**
 * The catalogue of end-of-slot sounds offered in Settings.
 *
 * [id] is the stable key written to DataStore; it never changes even if the enum is reordered or
 * an entry is renamed. The audio file is deliberately **not** referenced here: `alert/SoundCatalog`
 * is the single place that maps a sound onto `R.raw`, so the domain stays free of raw resources and
 * the enum can exist before the assets land in F5.
 */
enum class AlertSound(val id: String, @param:StringRes val labelRes: Int) {
    SILENT("silent", R.string.sound_silent),
    BELL("bell", R.string.sound_bell),
    BOWL("bowl", R.string.sound_bowl),
    DIGITAL("digital", R.string.sound_digital),
    SOFT("soft", R.string.sound_soft),
    ;

    val isAudible: Boolean get() = this != SILENT

    companion object {
        /**
         * The sound a finished pomodoro gets by default.
         *
         * The singing bowl, measured as the gravest and longest of the four — spectral centroid 506 Hz,
         * 2.9 s — so it lands as a reward rather than as an order. Ending a pomodoro is good news.
         */
        val DEFAULT_FOCUS = BOWL

        /**
         * The sound the end of a break gets by default.
         *
         * `digital`, at a 2170 Hz centroid and 0.72 s: four times brighter than the bowl and dry. This one
         * has to cut through whatever you drifted into and get you back to work, so it is deliberately
         * less pleasant. See docs/decisions/012-*.
         */
        val DEFAULT_BREAK = DIGITAL

        fun fromId(id: String?, fallback: AlertSound = DEFAULT_FOCUS): AlertSound =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}
