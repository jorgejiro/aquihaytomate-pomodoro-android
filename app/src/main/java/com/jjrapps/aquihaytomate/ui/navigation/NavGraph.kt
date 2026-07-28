package com.jjrapps.aquihaytomate.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jjrapps.aquihaytomate.ui.changelog.ChangelogScreen
import com.jjrapps.aquihaytomate.ui.common.TopTabBar
import com.jjrapps.aquihaytomate.ui.licenses.LicensesScreen
import com.jjrapps.aquihaytomate.ui.main.MainUiState
import com.jjrapps.aquihaytomate.ui.main.MainViewModel
import com.jjrapps.aquihaytomate.ui.onboarding.OnboardingScreen
import com.jjrapps.aquihaytomate.ui.settings.SettingsScreen
import com.jjrapps.aquihaytomate.ui.stats.StatsScreen
import com.jjrapps.aquihaytomate.ui.theme.BackgroundVoid
import com.jjrapps.aquihaytomate.ui.theme.phaseColorsOf
import com.jjrapps.aquihaytomate.ui.timer.TimerScreen

@Composable
fun AquiHayTomateNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val current = state) {
        // Drawing the tabs before the settings have been read would flash the timer at a first-run user
        // for a frame before the onboarding replaced it.
        MainUiState.Loading -> Box(Modifier.fillMaxSize())

        is MainUiState.Ready -> if (current.onboardingDone) {
            MainTabs(navController = navController, state = current)
        } else {
            OnboardingScreen(
                // No navigation needed: flipping onboarding_done re-emits the state and swaps the tree.
                onFinished = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun MainTabs(navController: NavHostController, state: MainUiState.Ready) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Changelog is reached from Settings and is not a tab; while it is open the Settings tab
    // stays highlighted.
    val selectedTab = when (currentRoute) {
        Screen.Stats.route -> TopTab.STATS
        Screen.Settings.route, Screen.Changelog.route, Screen.Licenses.route -> TopTab.SETTINGS
        else -> TopTab.TIMER
    }

    Scaffold(
        containerColor = BackgroundVoid,
        topBar = {
            TopTabBar(
                selected = selectedTab,
                onSelect = { tab -> navController.navigateToTab(tab) },
                accent = phaseColorsOf(state.phase).bright,
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
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onOpenChangelog = { navController.navigate(Screen.Changelog.route) },
                        onOpenLicenses = { navController.navigate(Screen.Licenses.route) },
                    )
                }
                composable(Screen.Changelog.route) { ChangelogScreen() }
                composable(Screen.Licenses.route) { LicensesScreen() }
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
