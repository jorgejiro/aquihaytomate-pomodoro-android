package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.Caption
import com.jjrapps.aquihaytomate.ui.theme.NumberSmall
import com.jjrapps.aquihaytomate.ui.theme.RowLabel
import com.jjrapps.aquihaytomate.ui.theme.SurfacePressed
import com.jjrapps.aquihaytomate.ui.theme.SurfaceRaised
import com.jjrapps.aquihaytomate.ui.theme.TextGhost
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.TomateBright

private val ROW_HEIGHT = 52.dp
private val ROW_PADDING = 16.dp
private val GROUP_CORNER = 12.dp
private val DIVIDER_INDENT = 14.dp

/**
 * A group of settings rows: one rounded surface with hairline rules between the rows.
 *
 * The rules are drawn by the group rather than by each row, which is the only way to avoid a stray rule
 * under the last one.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable SettingsGroupScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GROUP_CORNER))
            .background(SurfaceRaised),
    ) {
        val scope = remember { SettingsGroupScope() }
        scope.content()
    }
}

/** Gives the rows of a group access to the group's own inset rule. */
class SettingsGroupScope internal constructor() {

    @Composable
    fun Divider() {
        HairlineDivider(startIndent = DIVIDER_INDENT)
    }
}

/**
 * The standard row: label, optional sublabel, value on the right and a chevron.
 *
 * @param value shown in the phase accent; null for a row that only navigates.
 * @param trailing replaces the value and chevron entirely — used for the toggle rows.
 * @param minHeight taller than the 52 dp of Settings where the rows are the whole screen rather than one
 *   of eight groups — the onboarding, where two permission rows had nothing else to share the page with and
 *   read as cramped.
 */
@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
    value: String? = null,
    valueColor: Color = TomateBright,
    showChevron: Boolean = true,
    minHeight: Dp = ROW_HEIGHT,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(if (pressed && onClick != null) SurfacePressed else Color.Transparent)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = RowLabel, color = TextPrimary)
            if (sublabel != null) {
                Text(text = sublabel, style = Caption, color = TextMuted)
            }
        }

        Spacer(Modifier.width(12.dp))

        if (trailing != null) {
            trailing()
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(text = value, style = NumberSmall, color = valueColor)
                }
                if (showChevron) {
                    Spacer(Modifier.width(8.dp))
                    Text(text = "›", style = NumberSmall, color = TextGhost)
                }
            }
        }
    }
}

/** A row whose value is a [PhaseToggle]. The whole row is the tap target, not just the switch. */
@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sublabel: String? = null,
) {
    SettingsRow(
        label = label,
        sublabel = sublabel,
        modifier = modifier,
        showChevron = false,
        onClick = { onCheckedChange(!checked) },
        trailing = { PhaseToggle(checked = checked) },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 320)
@Composable
private fun SettingsGroupPreview() {
    AquiHayTomateTheme {
        SettingsGroup {
            SettingsRow(label = "Enfoque", value = "25 min", onClick = {})
            Divider()
            SettingsRow(label = "Descanso corto", value = "5 min", onClick = {})
            Divider()
            SettingsToggleRow(
                label = "Auto-iniciar el siguiente",
                checked = true,
                onCheckedChange = {},
            )
            Divider()
            SettingsRow(
                label = "Alarmas exactas",
                sublabel = "Mejora la puntualidad con la pantalla apagada",
                value = "OK",
                onClick = {},
            )
        }
    }
}
