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
         * The bell: brighter than the bowl — a 1112 Hz centroid against 506, and 11 dB more energy above
         * 2 kHz — and it rings for 2.6 s. This one has to cut through whatever you drifted into and get
         * you back to work.
         *
         * `digital` measures louder on paper (−7.2 dB RMS against −15.3, and the most total energy of the
         * four) and was the first choice for that reason, but it lasts 0.72 s: a blip that is over before
         * it registers if you are not looking at the phone. Chosen by ear on the author's own device, on
         * the grounds that standing out is a matter of insisting, not of peak level. See
         * docs/decisions/012-*.
         */
        val DEFAULT_BREAK = BELL

        fun fromId(id: String?, fallback: AlertSound = DEFAULT_FOCUS): AlertSound =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}
