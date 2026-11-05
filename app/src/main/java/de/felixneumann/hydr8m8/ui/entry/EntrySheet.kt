package de.felixneumann.hydr8m8.ui.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.AlcoholMath
import de.felixneumann.hydr8m8.domain.DrinkIcon
import de.felixneumann.hydr8m8.domain.DrinkKind
import de.felixneumann.hydr8m8.domain.Entry
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.components.DatePickerDialogFor
import de.felixneumann.hydr8m8.ui.components.DrinkIconView
import de.felixneumann.hydr8m8.ui.components.TimePickerDialog
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/** Result of the entry sheet. */
data class EntryDraft(
    val kind: DrinkKind,
    val volumeMl: Int,
    val abvPercent: Double?,
    val icon: DrinkIcon,
    val at: Instant,
    val saveAsFavorite: Boolean,
)

private const val STEP_ML = 10
private const val MIN_ML = 10
private const val MAX_WATER_ML = 2000
private const val MAX_ALCOHOL_ML = 1000
private const val ABV_STEP = 0.5
private const val MIN_ABV = 0.5
private const val MAX_ABV = 70.0
private const val DEFAULT_WATER_ML = 250
private const val DEFAULT_ALCOHOL_ML = 330
private const val DEFAULT_ABV = 5.0

/**
 * Bottom sheet to add a custom amount, back-date an entry ([initialDate]) or edit [editing].
 * Amount slider moves in 10 ml steps.
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kind by remember { mutableStateOf(editing?.kind ?: DrinkKind.WATER) }
    var icon by remember { mutableStateOf(editing?.icon ?: DrinkIcon.GLASS) }
    var volume by remember { mutableIntStateOf(editing?.volumeMl ?: DEFAULT_WATER_ML) }
    var abv by remember { mutableDoubleStateOf(editing?.abvPercent ?: DEFAULT_ABV) }
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
    val colors = HydrTheme.colors
    val maxMl = if (kind == DrinkKind.ALCOHOL) MAX_ALCOHOL_ML else MAX_WATER_ML

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DrinkKind.entries.forEach { k ->
                    FilterChip(
                        selected = kind == k,
                        onClick = {
                            if (kind != k) {
                                kind = k
                                icon = DrinkIcon.forKind(k).first()
                                volume = if (k == DrinkKind.ALCOHOL) DEFAULT_ALCOHOL_ML else DEFAULT_WATER_ML
                            }
                        },
                        label = {
                            Text(
                                stringResource(
                                    if (k ==
                                        DrinkKind.WATER
                                    ) {
                                        R.string.kind_water
                                    } else {
                                        R.string.kind_alcohol
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DrinkIcon.forKind(kind)) { option ->
                    Surface(
                        onClick = { icon = option },
                        shape = RoundedCornerShape(12.dp),
                        color = if (icon == option) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    ) {
                        DrinkIconView(option, size = 30.dp, modifier = Modifier.padding(8.dp))
                    }
                }
            }

            Text(
                Format.volume(volume),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 18.dp),
            )
            Slider(
                value = volume.toFloat(),
                onValueChange = { volume = ((it / STEP_ML).roundToInt() * STEP_ML).coerceIn(MIN_ML, maxMl) },
                valueRange = MIN_ML.toFloat()..maxMl.toFloat(),
                steps = (maxMl - MIN_ML) / STEP_ML - 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(-STEP_ML * 5, -STEP_ML, STEP_ML, STEP_ML * 5).forEach { delta ->
                    OutlinedButton(
                        onClick = { volume = (volume + delta).coerceIn(MIN_ML, maxMl) },
                        modifier = Modifier.weight(1f),
                    ) { Text((if (delta > 0) "+" else "−") + kotlin.math.abs(delta)) }
                }
            }

            if (kind == DrinkKind.ALCOHOL) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.abv_label, Format.abv(abv)) + " · " +
                        stringResource(R.string.goal_bonus, Format.volume(AlcoholMath.goalBonusMl(volume, abv))),
                    color = colors.alcohol,
                )
                Slider(
                    value = abv.toFloat(),
                    onValueChange = { abv = ((it / ABV_STEP).roundToInt() * ABV_STEP).coerceIn(MIN_ABV, MAX_ABV) },
                    valueRange = MIN_ABV.toFloat()..MAX_ABV.toFloat(),
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showDate = true }) {
                    Text(
                        "📅 " +
                            if (date ==
                                today
                            ) {
                                stringResource(R.string.today)
                            } else {
                                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                            },
                    )
                }
                TextButton(onClick = { showTime = true }) {
                    Text("🕘 " + time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
                }
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
                            kind = kind,
                            volumeMl = volume,
                            abvPercent = if (kind == DrinkKind.ALCOHOL) abv else null,
                            icon = icon,
                            at = date.atTime(time).atZone(zone).toInstant(),
                            saveAsFavorite = favorite,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    if (editing ==
                        null
                    ) {
                        stringResource(R.string.add_volume, Format.volume(volume))
                    } else {
                        stringResource(R.string.save)
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete_entry), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.size(16.dp))
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
