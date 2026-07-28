package com.jjrapps.aquihaytomate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jjrapps.aquihaytomate.R

/**
 * Inter for text, Space Grotesk for figures. Both SIL OFL 1.1, bundled in `res/font/`, with the licence
 * in `res/raw/licenses_ofl.txt`.
 *
 * **Variable fonts, one file per family.** A single `Inter[opsz,wght].ttf` covers every weight the app
 * uses, where the static cut would have been three files for Inter alone; `FontVariation` picks the
 * weight at runtime. About 1 MB for the two, and supported from API 26 — comfortably under `minSdk` 31.
 */
private val InterWeights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold)
private val GroteskWeights = listOf(FontWeight.Medium, FontWeight.Bold)

@OptIn(ExperimentalTextApi::class)
val InterFontFamily: FontFamily = FontFamily(
    InterWeights.map { weight ->
        Font(
            resId = R.font.inter_variable,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

@OptIn(ExperimentalTextApi::class)
val GroteskFontFamily: FontFamily = FontFamily(
    GroteskWeights.map { weight ->
        Font(
            resId = R.font.space_grotesk_variable,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)

/**
 * Tabular figures. Without this the digits have different widths and the countdown jitters
 * horizontally every second, which is exactly what makes a timer look cheap.
 */
private const val TABULAR = "tnum"

/** The countdown itself. */
val DisplayTimer = TextStyle(
    fontFamily = GroteskFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 76.sp,
    letterSpacing = (-2.0).sp,
    fontFeatureSettings = TABULAR,
)

/** "Today" figures and the streak counter. */
val DisplayStat = TextStyle(
    fontFamily = GroteskFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    letterSpacing = (-0.8).sp,
    fontFeatureSettings = TABULAR,
)

/** FOCUS / BREAK / LONG BREAK. */
val PhaseLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    letterSpacing = 3.2.sp,
)

/** Secondary controls: RESET, the onboarding pager button. */
val ControlLabel = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    letterSpacing = 1.4.sp,
)

/**
 * PAUSE / START / RESUME, the primary control of the timer.
 *
 * One step above [ControlLabel] because it is the only thing on that screen the user ever taps, and at
 * 15 sp it read as a caption next to a 268 dp tomato.
 */
val ControlLabelLarge = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 19.sp,
    letterSpacing = 1.8.sp,
)

val TabLabel = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 1.2.sp,
)

val SectionLabelStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    letterSpacing = 1.6.sp,
)

/** The label of a settings row. */
val RowLabel = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
)

/** Titles of onboarding pages and of the changelog. */
val TitleScreen = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    letterSpacing = (-0.4).sp,
)

/** Running text. */
val BodyDefault = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
)

/** Labels under figures, the secondary control, chart detail lines. */
val Caption = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    letterSpacing = 0.4.sp,
)

/** Settings values, chart axis labels. */
val NumberSmall = TextStyle(
    fontFamily = GroteskFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    fontFeatureSettings = TABULAR,
)

val AquiHayTomateTypography = Typography(
    displayLarge = DisplayTimer,
    headlineMedium = TitleScreen,
    titleMedium = ControlLabel,
    bodyMedium = BodyDefault,
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    labelLarge = ControlLabel,
    labelMedium = TabLabel,
    labelSmall = Caption,
)
