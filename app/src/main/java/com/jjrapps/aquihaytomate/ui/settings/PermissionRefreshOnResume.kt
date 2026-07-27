package com.jjrapps.aquihaytomate.ui.settings

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
 * Both permissions live in the system settings, which the user reaches from this very screen, so the
 * state is guaranteed to be stale exactly when they return. Polling on resume is the whole mechanism:
 * neither permission has a change broadcast to observe.
 */
@Composable
internal fun PermissionRefreshOnResume(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onResume(
                    notificationsGranted = NotificationManagerCompat.from(context)
                        .areNotificationsEnabled(),
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
