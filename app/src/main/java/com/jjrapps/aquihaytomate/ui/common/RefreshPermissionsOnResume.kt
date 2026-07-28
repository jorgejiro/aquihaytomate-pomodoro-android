package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Re-reads the permission state every time the screen comes back to the foreground.
 *
 * Both permissions live in the system settings, which the user reaches from the screens that show them,
 * so the state is guaranteed to be stale exactly when they return. Polling on resume is the whole
 * mechanism: neither permission has a change broadcast to observe.
 *
 * Shared by Settings and by the last onboarding page, which is the point — two copies of this would drift
 * and one of the two screens would end up showing a stale "pending".
 *
 * @param onResume receives whether notifications are enabled; the exact alarm permission is read by the
 *   ViewModel through `TimerAlarmScheduler`, which does not need a Context here.
 */
@Composable
fun RefreshPermissionsOnResume(onResume: (notificationsGranted: Boolean) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume(NotificationManagerCompat.from(context).areNotificationsEnabled())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
