package com.jjrapps.aquihaytomate.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jjrapps.aquihaytomate.domain.usecase.ReconcileTimerUseCase
import com.jjrapps.aquihaytomate.domain.usecase.SyncTimerRuntimeUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Repairs the timer after a reboot or an app update.
 *
 * **The service is not relaunched here on purpose.** The state is reconciled and the backup alarm is
 * rearmed; the service comes back when the user opens the app or taps the notification or the widget.
 * `specialUse` would still be allowed to start from `BOOT_COMPLETED` — unlike `dataSync` and friends on
 * Android 15 — but starting it just in case burns battery for nothing. See ADR 002, point 8.
 *
 * `LOCKED_BOOT_COMPLETED` is handled as well so that the alarm is rearmed before the user unlocks. The
 * DataStore files live in credential-encrypted storage, so the read may fail until unlock; that is what
 * the plain `BOOT_COMPLETED` pass is for.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reconcileTimer: ReconcileTimerUseCase

    @Inject lateinit var syncTimerRuntime: SyncTimerRuntimeUseCase

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> Unit

            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                reconcileTimer()
                // Rearms the alarm against the post-reboot uptime, which restarted from zero.
                syncTimerRuntime()
            } catch (e: Exception) {
                Timber.e(e, "Could not reconcile the timer after boot")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
