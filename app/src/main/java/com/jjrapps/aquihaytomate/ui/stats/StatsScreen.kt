package com.jjrapps.aquihaytomate.ui.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jjrapps.aquihaytomate.R
import com.jjrapps.aquihaytomate.ui.common.PlaceholderScreen

// TODO(F6): replace with the real stats UI — StatCell row, WeekBarChart, MonthHeatmap,
//  StreakStrip, Sparkline. See docs/design-spec.md §5.2.
@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(stringResource(R.string.tab_stats), modifier)
}
