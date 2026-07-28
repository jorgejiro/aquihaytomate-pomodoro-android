package com.jjrapps.aquihaytomate.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jjrapps.aquihaytomate.domain.usecase.CompleteSlotUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Layer 3 of the engine: the safety net fires here.
 *
 * Reached when the alarm goes off — which normally means the foreground service was killed by the OEM
 * or the process died. It closes the slot straight from the receiver instead of trying to start the
 * service: when the alarm was inexact there is no foreground-start exemption anyway, and
 * `CompleteSlotUseCase` needs nothing but the repositories.
 */
@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var completeSlot: CompleteSlotUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SLOT_DEADLINE) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                completeSlot()
            } catch (e: Exception) {
                Timber.e(e, "Could not close the slot from the backup alarm")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SLOT_DEADLINE = "com.jjrapps.aquihaytomate.SLOT_DEADLINE"
    }
}
