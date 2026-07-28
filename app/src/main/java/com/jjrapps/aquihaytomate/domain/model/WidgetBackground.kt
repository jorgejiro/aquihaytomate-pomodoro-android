package com.jjrapps.aquihaytomate.domain.model

import androidx.annotation.StringRes
import com.jjrapps.aquihaytomate.R

/** Whether the 1×1 widget paints its black plate or sits straight on the wallpaper. */
enum class WidgetBackground(val id: String, @param:StringRes val labelRes: Int) {
    SOLID("solid", R.string.widget_background_solid),
    TRANSPARENT("transparent", R.string.widget_background_transparent),
    ;

    companion object {
        val DEFAULT = SOLID

        fun fromId(id: String?): WidgetBackground = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
