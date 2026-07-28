package com.jjrapps.aquihaytomate.widget

import com.jjrapps.aquihaytomate.di.ApplicationScope
import com.jjrapps.aquihaytomate.domain.repository.SettingsRepository
import com.jjrapps.aquihaytomate.domain.repository.TimerStateRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The single place the widget is refreshed from.
 *
 * `distinctUntilChangedBy` on the three fields that change what the widget looks like, plus a debounce, so
 * a burst of writes during a transition collapses into one update. **Never once a second** — that is what
 * the `Chronometer` is for. See CLAUDE.md §6.
 */
@Singleton
class WidgetStateCollector @Inject constructor(
    private val timerStateRepository: TimerStateRepository,
    private val settingsRepository: SettingsRepository,
    private val widgetUpdater: WidgetUpdater,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            combine(
                timerStateRepository.state,
                // The background is a widget-only setting, so a change to it has to repaint too.
                settingsRepository.settings.map { it.widgetBackground },
            ) { state, background -> state to background }
                .distinctUntilChangedBy { (state, background) ->
                    listOf(state.status, state.slotType, state.endAtEpochMs, background)
                }
                .debounce(DEBOUNCE_MS)
                .collect {
                    try {
                        widgetUpdater.updateAll()
                    } catch (e: Exception) {
                        Timber.e(e, "Could not refresh the widget")
                    }
                }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 250L
    }
}
