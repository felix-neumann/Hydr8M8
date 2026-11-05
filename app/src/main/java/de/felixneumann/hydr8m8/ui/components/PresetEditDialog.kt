package de.felixneumann.hydr8m8.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.felixneumann.hydr8m8.domain.BuiltInPresets
import de.felixneumann.hydr8m8.domain.DrinkIcon
import de.felixneumann.hydr8m8.domain.DrinkKind
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.ui.Format
import kotlin.math.roundToInt

private const val STEP_ML = 10
private const val MAX_WATER_ML = 2000
private const val MAX_ALCOHOL_ML = 1000
private const val MIN_ABV = 0.5
private const val MAX_ABV = 70.0
private const val DEFAULT_ABV = 5.0

/** A fresh, unsaved preset to start the "new amount" dialog with. */
fun newPreset(kind: DrinkKind = DrinkKind.WATER): Preset {
    val icon = if (kind == DrinkKind.ALCOHOL) DrinkIcon.BEER else DrinkIcon.MUG
    val template = BuiltInPresets.all.first { it.icon == icon }
    return Preset(0, kind, template.volumeMl, template.abvPercent, icon, null, false, 0)
}

/**
 * Creates or edits a library preset. New presets ([Preset.id] == 0) may switch between water and
 * alcohol; picking an icon pre-fills the vessel's typical amount (and ABV).
 */
@Composable
fun PresetEditDialog(
    preset: Preset,
    onSave: (Preset) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val isNew = preset.id == 0L
    var kind by remember { mutableStateOf(preset.kind) }
    var icon by remember { mutableStateOf(preset.icon) }
    var volume by remember { mutableIntStateOf(preset.volumeMl) }
    var abv by remember { mutableDoubleStateOf(preset.abvPercent ?: DEFAULT_ABV) }
    val alcohol = kind == DrinkKind.ALCOHOL
    val max = if (alcohol) MAX_ALCOHOL_ML else MAX_WATER_ML

    fun applyTemplate(option: DrinkIcon) {
        BuiltInPresets.all.firstOrNull { it.icon == option }?.let { t ->
            volume = t.volumeMl
            t.abvPercent?.let { abv = it }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.new_preset else R.string.edit_preset)) },
        text = {
            Column {
                if (isNew) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DrinkKind.entries.forEach { k ->
                            FilterChip(
                                selected = kind == k,
                                onClick = {
                                    if (kind != k) {
                                        val fresh = newPreset(k)
                                        kind = k
                                        icon = fresh.icon
                                        applyTemplate(fresh.icon)
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
                    Spacer(Modifier.height(8.dp))
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DrinkIcon.forKind(kind)) { option ->
                        Surface(
                            onClick = {
                                icon = option
                                applyTemplate(option)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color =
                                if (icon ==
                                    option
                                ) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        ) { DrinkIconView(option, size = 26.dp, modifier = Modifier.padding(6.dp)) }
                    }
                }
                Text(
                    Format.volume(volume),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp),
                )
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { volume = ((it / STEP_ML).roundToInt() * STEP_ML).coerceIn(STEP_ML, max) },
                    valueRange = STEP_ML.toFloat()..max.toFloat(),
                )
                if (alcohol) {
                    Text(stringResource(R.string.abv_label, Format.abv(abv)))
                    Slider(
                        value = abv.toFloat(),
                        onValueChange = { abv = ((it * 2).roundToInt() / 2.0).coerceIn(MIN_ABV, MAX_ABV) },
                        valueRange = MIN_ABV.toFloat()..MAX_ABV.toFloat(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    preset.copy(kind = kind, icon = icon, volumeMl = volume, abvPercent = if (alcohol) abv else null),
                )
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
