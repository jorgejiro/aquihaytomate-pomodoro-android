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
        val DEFAULT = BELL

        fun fromId(id: String?): AlertSound = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
