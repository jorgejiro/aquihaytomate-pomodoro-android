package com.jjrapps.aquihaytomate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True when the liquid animation must not run: system animator scale at zero, battery saver on,
 * or the user's own toggle in Settings.
 *
 * `rememberInfiniteTransition` does NOT honour `Settings.Global.ANIMATOR_DURATION_SCALE` by itself
 * — that only affects `Animator` and View animations — so this has to be plumbed by hand.
 * See docs/design-spec.md §6.5.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Dark only, on purpose. No light scheme, no dynamic colour. See CLAUDE.md §1.
 * Material 3 slots are mapped onto our own semantic palette so that stray Material components
 * (ripples, bottom sheets) do not fall back to purple.
 */
private val AquiHayTomateDarkColorScheme = darkColorScheme(
    primary = TomateBright,
    onPrimary = TextOnLiquid,
    primaryContainer = TomateFill,
    onPrimaryContainer = TextPrimary,
    secondary = AmbarBright,
    onSecondary = TextOnLiquid,
    tertiary = DoradoBright,
    onTertiary = TextOnLiquid,
    background = BackgroundVoid,
    onBackground = TextPrimary,
    surface = SurfaceRaised,
    onSurface = TextPrimary,
    surfaceVariant = SurfacePressed,
    onSurfaceVariant = TextSecondary,
    outline = BorderStrong,
    outlineVariant = BorderHair,
    error = AlertAmber,
    onError = TextOnLiquid,
)

@Composable
fun AquiHayTomateTheme(
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(
            colorScheme = AquiHayTomateDarkColorScheme,
            typography = AquiHayTomateTypography,
            shapes = AquiHayTomateShapes,
            content = content,
        )
    }
}
