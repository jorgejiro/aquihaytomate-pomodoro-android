package com.jjrapps.aquihaytomate

import android.content.Context
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjrapps.aquihaytomate.domain.model.AppLanguage
import com.jjrapps.aquihaytomate.ui.main.MainUiState
import com.jjrapps.aquihaytomate.ui.main.MainViewModel
import com.jjrapps.aquihaytomate.ui.navigation.AquiHayTomateNavGraph
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.BackgroundVoid
import dagger.hilt.android.AndroidEntryPoint

/**
 * Extends [AppCompatActivity] rather than ComponentActivity because runtime locale switching goes
 * through `AppCompatDelegate.setApplicationLocales`.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val ready = state as? MainUiState.Ready

            ApplyLanguage(ready?.language)

            AquiHayTomateTheme(
                reducedMotion = rememberReducedMotion(
                    liquidAnimationEnabled = ready?.liquidAnimationEnabled ?: true,
                ),
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = BackgroundVoid) {
                    AquiHayTomateNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Applies the language the user picked in Settings.
 *
 * Set only when it actually differs from what is already in force: `setApplicationLocales` recreates the
 * Activity, so writing it unconditionally on every recomposition would loop forever.
 */
@Composable
private fun ApplyLanguage(language: AppLanguage?) {
    if (language == null) return

    DisposableEffect(language) {
        val desired = language.tag?.let { LocaleListCompat.forLanguageTags(it) }
            ?: LocaleListCompat.getEmptyLocaleList()
        if (AppCompatDelegate.getApplicationLocales() != desired) {
            AppCompatDelegate.setApplicationLocales(desired)
        }
        onDispose { }
    }
}

/**
 * True when the liquid animation must not run.
 *
 * Three sources, because no single one is enough: the system animator scale, battery saver — which some
 * OEMs do not reflect in the animator scale at all — and the user's own toggle.
 * `rememberInfiniteTransition` honours none of them by itself; see docs/design-spec.md §6.5.
 */
@Composable
private fun rememberReducedMotion(liquidAnimationEnabled: Boolean): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    var systemReduced by remember { mutableStateOf(animatorScaleIsZero(context)) }
    var powerSaving by remember { mutableStateOf(isPowerSaveMode(context)) }

    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                systemReduced = animatorScaleIsZero(context)
                powerSaving = isPowerSaveMode(context)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }

    return systemReduced || powerSaving || !liquidAnimationEnabled
}

private fun animatorScaleIsZero(context: Context): Boolean =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f

private fun isPowerSaveMode(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
