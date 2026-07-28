package com.jjrapps.aquihaytomate.ui.navigation

import androidx.annotation.StringRes
import com.jjrapps.aquihaytomate.R

sealed class Screen(val route: String) {
    data object Timer : Screen("timer")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")

    /** Reached from Settings → About; not a tab. */
    data object Changelog : Screen("changelog")

    /** The OFL notice for the bundled fonts. Also reached from Settings → About. */
    data object Licenses : Screen("licenses")
}

/** The three top-level tabs, in display order. */
enum class TopTab(val screen: Screen, @param:StringRes val labelRes: Int) {
    TIMER(Screen.Timer, R.string.tab_timer),
    STATS(Screen.Stats, R.string.tab_stats),
    SETTINGS(Screen.Settings, R.string.tab_settings),
}
