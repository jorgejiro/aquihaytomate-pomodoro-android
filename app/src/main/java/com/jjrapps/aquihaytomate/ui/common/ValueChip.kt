package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.BorderStrong
import com.jjrapps.aquihaytomate.ui.theme.NumberSmall
import com.jjrapps.aquihaytomate.ui.theme.SurfacePressed
import com.jjrapps.aquihaytomate.ui.theme.TextOnLiquid
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.TomateFill

private val CHIP_HEIGHT = 40.dp
private val CHIP_CORNER = 8.dp

/** One option of a picker: the value to store and the text to show. */
data class PickerOption<T>(val value: T, val label: String)

/**
 * A selectable chip. Shared by the picker sheets in Settings and by the duration choices in onboarding,
 * so a value looks the same wherever the user meets it.
 */
@Composable
fun ValueChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = CHIP_HEIGHT)
            .clip(RoundedCornerShape(CHIP_CORNER))
            .background(if (selected) TomateFill else SurfacePressed)
            .then(
                if (selected) {
                    Modifier
                } else {
                    Modifier.border(1.dp, BorderStrong, RoundedCornerShape(CHIP_CORNER))
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = NumberSmall,
            color = if (selected) TextOnLiquid else TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ValueChipPreview() {
    AquiHayTomateTheme {
        ValueChip(label = "25 min", selected = true, onClick = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ValueChipUnselectedPreview() {
    AquiHayTomateTheme {
        ValueChip(label = "45 min", selected = false, onClick = {})
    }
}
