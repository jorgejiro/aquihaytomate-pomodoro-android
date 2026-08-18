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
         * **`digital`, chosen by ear on the author's own device in 1.3.1.** It measures as the loudest of
         * the four — −7.2 dB RMS against the bowl's −15.3, and the most total energy — and at 0.72 s it is
         * also the shortest: a short, dry beep rather than a chime that fades.
         *
         * It replaced the singing bowl, which was picked for being the gravest and longest — spectral
         * centroid 506 Hz, 2.9 s — on the theory that ending a pomodoro is good news and should land as a
         * reward. The theory held up worse than the sound did: soft and slow is also easy to miss, which
         * is the one thing an end-of-slot alert cannot be. Both are still in the catalogue, and the bowl
         * is one tap away. See docs/decisions/012-*.
         */
        val DEFAULT_FOCUS = DIGITAL

        /**
         * The sound the end of a break gets by default.
         *
         * The bell: brighter than the bowl — a 1112 Hz centroid against 506, and 11 dB more energy above
         * 2 kHz — and it rings for 2.6 s. This one has to cut through whatever you drifted into and get
         * you back to work.
         *
         * Still the bell and not `digital`, even though `digital` is now what a finished pomodoro plays:
         * the point of this ADR is that the two ends of a slot must not sound alike, and the bell rings
         * for 2.6 s against 0.72 — the longer of the two is the one that has to reach you from another
         * room. See docs/decisions/012-*.
         */
        val DEFAULT_BREAK = BELL

        fun fromId(id: String?, fallback: AlertSound = DEFAULT_FOCUS): AlertSound =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}
