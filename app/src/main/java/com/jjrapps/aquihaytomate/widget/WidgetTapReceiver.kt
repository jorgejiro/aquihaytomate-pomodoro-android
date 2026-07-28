package com.jjrapps.aquihaytomate.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jjrapps.aquihaytomate.domain.time.ElapsedRealtimeSource
import com.jjrapps.aquihaytomate.domain.usecase.ReconcileTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ResetTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.ToggleTimerUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The widget's only input: one tap toggles, two taps reset.
 *
 * A widget tap is one of the legitimate exemptions for starting a foreground service, so the toggle path
 * can bring the service up from here.
 *
 * **No long press.** In every launcher — Nova, Pixel Launcher, One UI — a long press on a widget is
 * swallowed by the launcher itself to drag it or open its menu, and the event never reaches the app. It is
 * a platform limitation, not a design preference. See ADR 001.
 */
@AndroidEntryPoint
class WidgetTapReceiver : BroadcastReceiver() {

    @Inject lateinit var tapGate: TapGate

    @Inject lateinit var toggleTimer: ToggleTimerUseCase

    @Inject lateinit var resetTimer: ResetTimerUseCase

    @Inject lateinit var reconcileTimer: ReconcileTimerUseCase

    @Inject lateinit var elapsedRealtime: ElapsedRealtimeSource

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TAP) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // A tap is also a chance to repair the state: the widget may well be the first thing
                // touched after the process was killed.
                reconcileTimer()

                when (tapGate.register(elapsedRealtime.millis())) {
                    // Applied at once, with no waiting on the double-tap window.
                    TapKind.SINGLE -> toggleTimer()

                    // Undo the toggle the first tap already applied, then reset.
                    TapKind.DOUBLE -> {
                        toggleTimer()
                        resetTimer()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Could not handle the widget tap")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAP = "com.jjrapps.aquihaytomate.WIDGET_TAP"
    }
}
