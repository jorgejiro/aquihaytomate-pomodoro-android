package com.jjrapps.aquihaytomate.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jjrapps.aquihaytomate.ui.common.TopTabBar
import com.jjrapps.aquihaytomate.ui.settings.SettingsScreen
import com.jjrapps.aquihaytomate.ui.stats.StatsScreen
import com.jjrapps.aquihaytomate.ui.theme.BackgroundVoid
import com.jjrapps.aquihaytomate.ui.timer.TimerScreen

@Composable
fun AquiHayTomateNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Changelog is reached from Settings and is not a tab; while it is open the Settings tab
    // stays highlighted.
    val selectedTab = when (currentRoute) {
        Screen.Stats.route -> TopTab.STATS
        Screen.Settings.route, Screen.Changelog.route -> TopTab.SETTINGS
        else -> TopTab.TIMER
    }

    Scaffold(
        containerColor = BackgroundVoid,
        topBar = {
            // TODO(F7): tint the underline with the current phase accent (design-spec §5.0). It needs
            //  the timer state up here, which arrives with MainViewModel.
            TopTabBar(
                selected = selectedTab,
                onSelect = { tab -> navController.navigateToTab(tab) },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Timer.route,
            ) {
                composable(Screen.Timer.route) { TimerScreen() }
                composable(Screen.Stats.route) { StatsScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
            }
        }
    }
}

private fun NavHostController.navigateToTab(tab: TopTab) {
    navigate(tab.screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
