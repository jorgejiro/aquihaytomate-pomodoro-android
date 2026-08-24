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
         * **The bell, chosen by ear on the author's own device in 1.3.1**, and it rings for 2.6 s. Two
         * defaults have come and gone here: the singing bowl, picked for being the gravest and longest on
         * the theory that finishing a pomodoro is a reward, and then `digital`, picked for measuring the
         * loudest of the four. Neither survived being lived with — the bowl is easy to miss, and
         * `digital` is a 0.72 s blip that is over before you register it.
         *
         * The bell is what is left when the criterion is neither gentleness nor peak level but **being
         * heard**: bright enough to carry —a 1112 Hz centroid against the bowl's 506, and 11 dB more
         * energy above 2 kHz— and long enough to arrive. Both of the sounds it replaced are still in the
         * catalogue, one tap away. See docs/decisions/012-*.
         */
        val DEFAULT_FOCUS = BELL

        /**
         * The sound the end of a break gets by default.
         *
         * The singing bowl: the gravest and the longest of the four — a 506 Hz centroid and 2.9 s, with
         * more total energy than the bell — so it fills the room rather than cutting through it, which is
         * what works when you have drifted into something else and nobody is watching the screen.
         *
         * What matters is not which of the two is harsher but that **the two ends of a slot do not sound
         * alike**, which is the whole point of this pair: the bell says the work is done, the bowl says
         * the break is. Both play twice by default, which is what carries them to another room. See
         * docs/decisions/012-*.
         */
        val DEFAULT_BREAK = BOWL

        fun fromId(id: String?, fallback: AlertSound = DEFAULT_FOCUS): AlertSound =
            entries.firstOrNull { it.id == id } ?: fallback
    }
}
