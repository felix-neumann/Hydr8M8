package de.felixneumann.hydr8m8.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.BuiltInPresets
import de.felixneumann.hydr8m8.domain.DrinkKind
import de.felixneumann.hydr8m8.domain.Preset

/** A fresh, unsaved preset to start the "new amount" dialog with. */
fun newPreset(kind: DrinkKind = DrinkKind.WATER): Preset {
    val icon = BuiltInPresets.defaultIcon(kind)
    val template = BuiltInPresets.templateFor(icon)!!
    return Preset(0, kind, template.volumeMl, template.abvPercent, icon, null, false, 0)
}

/**
 * Creates or edits a library preset with the shared [DrinkForm]. Only new presets
 * ([Preset.id] == 0) may switch between water and alcohol.
 */
@Composable
fun PresetEditDialog(
    preset: Preset,
    onSave: (Preset) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val isNew = preset.id == 0L
    val drink = rememberDrinkDraft(preset.kind, preset.icon, preset.volumeMl, preset.abvPercent)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.new_preset else R.string.edit_preset)) },
        text = { DrinkForm(drink, showKindChips = isNew, amountSize = 28.sp) },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    preset.copy(
                        kind = drink.kind,
                        icon = drink.icon,
                        volumeMl = drink.volumeMl,
                        abvPercent = drink.storedAbv,
                    ),
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
