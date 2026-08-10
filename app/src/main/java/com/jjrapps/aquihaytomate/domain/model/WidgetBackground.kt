package com.jjrapps.aquihaytomate.domain.model

import androidx.annotation.StringRes
import com.jjrapps.aquihaytomate.R

/** Whether the 1×1 widget paints its black plate or sits straight on the wallpaper. */
enum class WidgetBackground(val id: String, @param:StringRes val labelRes: Int) {
    SOLID("solid", R.string.widget_background_solid),
    TRANSPARENT("transparent", R.string.widget_background_transparent),
    ;

    companion object {
        /**
         * No plate.
         *
         * Solid was the default until 1.2.0, and it looked right only because the author's home screen
         * wallpaper is black: on anything lighter the widget reads as a black card with a tomato inside
         * it, which is the one thing §1 of CLAUDE.md rules out — no cards, no boxes. The tomato is an
         * opaque filled circle, so it cuts its own silhouette over any wallpaper without help.
         */
        val DEFAULT = TRANSPARENT

        fun fromId(id: String?): WidgetBackground = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
