package com.jjrapps.aquihaytomate.alert

import androidx.annotation.RawRes
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.domain.model.AlertSound

/**
 * The one place that maps an [AlertSound] onto a raw resource.
 *
 * Keeping it here rather than on the enum is what lets the domain stay free of `R`, and it means adding
 * a sound touches exactly two files: the enum and this map.
 *
 * The clips are Opus in an Ogg container, mono at 48 kHz, and were synthesised for this app — so there
 * is nothing to attribute. Opus rather than the Vorbis the spec named: it is the same `.ogg` container,
 * has been supported since API 21, and encodes mono better. About 80 KB for the four.
 */
object SoundCatalog {

    @RawRes
    fun rawResFor(sound: AlertSound): Int? = when (sound) {
        AlertSound.SILENT -> null
        AlertSound.BELL -> R.raw.bell
        AlertSound.BOWL -> R.raw.bowl
        AlertSound.DIGITAL -> R.raw.digital
        AlertSound.SOFT -> R.raw.soft
    }
}
