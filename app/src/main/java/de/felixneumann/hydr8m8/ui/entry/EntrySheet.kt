package de.felixneumann.hydr8m8.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.BuiltInPresets
import de.felixneumann.hydr8m8.domain.DrinkIcon
import de.felixneumann.hydr8m8.domain.DrinkKind
import de.felixneumann.hydr8m8.domain.Entry
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.components.DatePickerDialogFor
import de.felixneumann.hydr8m8.ui.components.DrinkForm
import de.felixneumann.hydr8m8.ui.components.TimePickerDialog
import de.felixneumann.hydr8m8.ui.components.rememberDrinkDraft
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Result of the entry sheet. */
data class EntryDraft(
    val kind: DrinkKind,
    val volumeMl: Int,
    val abvPercent: Double?,
    val icon: DrinkIcon,
    val at: Instant,
    val saveAsFavorite: Boolean,
)

/**
 * Bottom sheet to add a custom amount, back-date an entry ([initialDate]) or edit [editing].
 * The drink itself is edited with the shared [DrinkForm].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    initialDate: LocalDate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (EntryDraft) -> Unit,
    modifier: Modifier = Modifier,
    editing: Entry? = null,
    onDelete: (() -> Unit)? = null,
) {
    val zone = ZoneId.systemDefault()
    val defaultIcon = BuiltInPresets.defaultIcon(DrinkKind.WATER)
    val drink =
        rememberDrinkDraft(
            kind = editing?.kind ?: DrinkKind.WATER,
            icon = editing?.icon ?: defaultIcon,
            volumeMl = editing?.volumeMl ?: BuiltInPresets.templateFor(defaultIcon)!!.volumeMl,
            abvPercent = editing?.abvPercent,
        )
    var date by remember { mutableStateOf(editing?.at?.atZone(zone)?.toLocalDate() ?: initialDate) }
    var time by remember {
        mutableStateOf(
            editing?.at?.atZone(zone)?.toLocalTime()?.withSecond(0)?.withNano(0)
                ?: if (initialDate == today) LocalTime.now().withSecond(0).withNano(0) else LocalTime.NOON,
        )
    }
    var favorite by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            DrinkForm(drink, showStepButtons = true)

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showDate = true }) {
                    Text("📅 " + if (date == today) stringResource(R.string.today) else Format.date(date))
                }
                TextButton(onClick = { showTime = true }) { Text("🕘 " + Format.time(time)) }
            }
            if (editing == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = favorite, onCheckedChange = { favorite = it })
                    Text(stringResource(R.string.save_as_favorite))
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onSave(
                        EntryDraft(
                            kind = drink.kind,
                            volumeMl = drink.volumeMl,
                            abvPercent = drink.storedAbv,
                            icon = drink.icon,
                            at = date.atTime(time).atZone(zone).toInstant(),
                            saveAsFavorite = favorite,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                val label =
                    if (editing == null) {
                        stringResource(R.string.add_volume, Format.volume(drink.volumeMl))
                    } else {
                        stringResource(R.string.save)
                    }
                Text(label, fontWeight = FontWeight.Bold)
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete_entry), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDate) {
        DatePickerDialogFor(
            initial = date,
            maxDate = today,
            onConfirm = {
                date = it
                showDate = false
            },
            onDismiss = { showDate = false },
        )
    }
    if (showTime) {
        TimePickerDialog(
            initial = time,
            onConfirm = {
                time = it
                showTime = false
            },
            onDismiss = { showTime = false },
        )
    }
}
