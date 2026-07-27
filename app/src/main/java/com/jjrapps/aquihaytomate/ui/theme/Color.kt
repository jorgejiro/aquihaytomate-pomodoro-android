package com.jjrapps.aquihaytomate.ui.theme

import androidx.compose.ui.graphics.Color
import com.jjrapps.aquihaytomate.domain.model.SlotType

// ─── Backgrounds ────────────────────────────────────────────────────────
val BackgroundVoid      = Color(0xFF000000)   // app canvas, pure black
val SurfaceRaised       = Color(0xFF0D0B0A)   // settings groups, bottom sheets
val SurfacePressed      = Color(0xFF1A1614)   // pressed state of rows and chips
val SurfaceSunken       = Color(0xFF120F0E)   // track behind chart bars

// ─── Focus phase · tomato red ───────────────────────────────────────────
val TomateBright        = Color(0xFFFF4433)   // live accent: labels, today's bar
val TomateFill          = Color(0xFFE23B2B)   // liquid fill
val TomateDeep          = Color(0xFF7A2018)   // outline of the empty circle
val TomateGhost         = Color(0xFF1C0D0B)   // hollow of the tomato above the liquid

// ─── Short break phase · amber ──────────────────────────────────────────
val AmbarBright         = Color(0xFFF2A03D)
val AmbarFill           = Color(0xFFDB8C2C)
val AmbarDeep           = Color(0xFF6B4715)
val AmbarGhost          = Color(0xFF1A1208)

// ─── Long break phase · gold ────────────────────────────────────────────
val DoradoBright        = Color(0xFFFFD166)
val DoradoFill          = Color(0xFFE8B849)
val DoradoDeep          = Color(0xFF6E5720)
val DoradoGhost         = Color(0xFF191509)

// ─── Text ───────────────────────────────────────────────────────────────
val TextPrimary         = Color(0xFFF5F2EF)   // bone, not pure white
val TextSecondary       = Color(0xFFA8A09B)
val TextMuted           = Color(0xFF6E6763)
val TextGhost           = Color(0xFF3A3532)   // pending cycle dots, chevrons
val TextOnLiquid        = Color(0xFF000000)   // knocked-out digits over the liquid

// ─── Borders ────────────────────────────────────────────────────────────
val BorderHair          = Color(0xFF1E1A18)   // 1 dp dividers
val BorderStrong        = Color(0xFF332D2A)   // inactive toggles and chips

// ─── Semantic ───────────────────────────────────────────────────────────
val AlertAmber          = Color(0xFFF2A03D)   // permission and battery warnings
val SurfaceHighlight    = Color(0x99FFFFFF)   // highlight line on the liquid surface
val WidgetPlateStroke   = Color(0x24FFFFFF)   // widget edge over light wallpapers

/**
 * The four shades of the current phase, resolved once and passed down, rather than each component
 * doing its own `when (slotType)`. See docs/design-spec.md §2.2.
 */
data class PhaseColors(
    val bright: Color,
    val fill: Color,
    val deep: Color,
    val ghost: Color,
)

fun phaseColorsOf(type: SlotType): PhaseColors = when (type) {
    SlotType.FOCUS -> PhaseColors(TomateBright, TomateFill, TomateDeep, TomateGhost)
    SlotType.SHORT_BREAK -> PhaseColors(AmbarBright, AmbarFill, AmbarDeep, AmbarGhost)
    SlotType.LONG_BREAK -> PhaseColors(DoradoBright, DoradoFill, DoradoDeep, DoradoGhost)
}
