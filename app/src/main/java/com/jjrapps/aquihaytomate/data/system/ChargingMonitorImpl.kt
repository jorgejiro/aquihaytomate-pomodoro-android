package com.jjrapps.aquihaytomate.data.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.jjrapps.aquihaytomate.domain.repository.ChargingMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/**
 * Plugged-in state, read from the battery broadcasts.
 *
 * `ACTION_BATTERY_CHANGED` is read once, as a sticky broadcast, purely for the starting value, and then
 * only `ACTION_POWER_CONNECTED` / `DISCONNECTED` stay registered. Keeping the battery broadcast itself
 * subscribed would hand us a broadcast every time the level or the temperature moves, several times a
 * minute, to answer a question that changes twice a day.
 *
 * The reading is `EXTRA_PLUGGED`, not [BatteryManager.isCharging]. On One UI — the author's phone —
 * battery protection stops the charge at 80 %, and from then on the phone reports "not charging" while
 * sitting on the charger. What the screen rule cares about is whether the display is costing the user
 * any battery, and plugged in it is not, full or not.
 */
@Singleton
class ChargingMonitorImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ChargingMonitor {

    override val isCharging: Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(intent.action == Intent.ACTION_POWER_CONNECTED)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        trySend(pluggedInNow())
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
                .onFailure { Timber.w(it, "Charging receiver was already gone") }
        }
    }.distinctUntilChanged()

    /**
     * The sticky `ACTION_BATTERY_CHANGED`, which a null receiver returns straight away without
     * subscribing to anything. A missing intent means the system has not published one yet; assuming
     * "not plugged in" only costs a screen that dims until the next connect.
     */
    private fun pluggedInNow(): Boolean {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return plugged != 0
    }
}
