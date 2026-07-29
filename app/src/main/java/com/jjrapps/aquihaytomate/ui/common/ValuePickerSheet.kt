package com.jjrapps.aquihaytomate.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jjrapps.aquihaytomate.ui.theme.AquiHayTomateTheme
import com.jjrapps.aquihaytomate.ui.theme.Caption
import com.jjrapps.aquihaytomate.ui.theme.SurfaceRaised
import com.jjrapps.aquihaytomate.ui.theme.TextMuted
import com.jjrapps.aquihaytomate.ui.theme.TextPrimary
import com.jjrapps.aquihaytomate.ui.theme.TitleScreen

private val SHEET_CORNER = 18.dp
private val SHEET_PADDING = 20.dp

/**
 * The bottom sheet behind every "pick one of these" setting: durations, cycle length, sound, language.
 *
 * A grid of chips rather than a slider or a dialog with a numeric field. Chips are a single tap for the
 * values anybody actually picks, and no keyboard is involved.
 *
 * **[stayOpen] is for the sound picker.** Everywhere else, tapping a chip picks the value and the sheet
 * gets out of the way — there is nothing to compare, 25 minutes is 25 minutes. A sound is the opposite:
 * the names mean nothing until you hear them, so its chips play as they are picked and the sheet has to
 * stay put for the next tap, with an explicit way out at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ValuePickerSheet(
    title: String,
    options: List<PickerOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    stayOpen: Boolean = false,
    hint: String? = null,
    doneLabel: String? = null,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceRaised,
        shape = RoundedCornerShape(topStart = SHEET_CORNER, topEnd = SHEET_CORNER),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SHEET_PADDING)
                .padding(bottom = SHEET_PADDING * 2),
        ) {
            Text(text = title, style = TitleScreen, color = TextPrimary)
            if (hint != null) {
                Spacer(Modifier.height(6.dp))
                Text(text = hint, style = Caption, color = TextMuted)
            }
            Spacer(Modifier.height(SHEET_PADDING))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    ValueChip(
                        label = option.label,
                        selected = option.value == selected,
                        onClick = {
                            onSelect(option.value)
                            if (!stayOpen) onDismiss()
                        },
                    )
                }
            }

            if (stayOpen && doneLabel != null) {
                Spacer(Modifier.height(SHEET_PADDING))
                TextControl(
                    label = doneLabel,
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0B0A, widthDp = 360)
@Composable
private fun PickerChipsPreview() {
    AquiHayTomateTheme {
        Column(Modifier.padding(SHEET_PADDING)) {
            Text(text = "Enfoque", style = TitleScreen, color = TextPrimary)
            Spacer(Modifier.height(SHEET_PADDING))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1, 5, 10, 15, 20, 25, 30, 45, 50, 60).forEach { minutes ->
                    ValueChip(label = "$minutes min", selected = minutes == 25, onClick = {})
                }
            }
        }
    }
}
