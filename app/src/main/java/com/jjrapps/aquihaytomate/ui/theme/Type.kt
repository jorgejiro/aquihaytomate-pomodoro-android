package com.jjrapps.aquihaytomate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO(F7): bundle the Inter and Space Grotesk TTFs in res/font/ and point these at them.
//  See docs/design-spec.md §3. Until then the system families keep the metrics roughly right.
val InterFontFamily: FontFamily = FontFamily.SansSerif
val GroteskFontFamily: FontFamily = FontFamily.Default

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

/** PAUSE / START / RESUME. */
val ControlLabel = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
    letterSpacing = 1.4.sp,
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
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleMedium = ControlLabel,
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    labelLarge = ControlLabel,
    labelMedium = TabLabel,
    labelSmall = Caption,
)
