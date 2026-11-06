package de.felixneumann.hydr8m8.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.DrinkIcon
import de.felixneumann.hydr8m8.domain.DrinkKind
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.domain.Surfaces
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.components.DrinkIconView
import de.felixneumann.hydr8m8.ui.components.HintText
import de.felixneumann.hydr8m8.ui.components.PresetEditDialog
import de.felixneumann.hydr8m8.ui.components.SectionCard
import de.felixneumann.hydr8m8.ui.components.SurfaceEditor
import de.felixneumann.hydr8m8.ui.components.newPreset
import de.felixneumann.hydr8m8.ui.label
import kotlin.math.roundToInt

@Composable
fun PresetLibraryScreen(
    presets: List<Preset>,
    onSave: (Preset) -> Unit,
    onDelete: (Preset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<Preset?>(null) }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HintText(stringResource(R.string.preset_library_hint))
        DrinkKind.entries.forEach { kind ->
            SectionCard(
                title =
                    stringResource(kind.label()),
            ) {
                presets.filter { it.kind == kind }.forEach { preset ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { editing = preset }
                            .padding(vertical = 4.dp)
                            .alpha(if (preset.hidden) 0.45f else 1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DrinkIconView(preset.icon)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Format.presetDisplay(preset))
                            Text(
                                stringResource(preset.icon.label()),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onSave(preset.copy(hidden = !preset.hidden)) }) {
                            Icon(
                                if (preset.hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription =
                                    stringResource(
                                        if (preset.hidden) R.string.show else R.string.hide,
                                    ),
                            )
                        }
                    }
                }
                TextButton(onClick = {
                    val icon = DrinkIcon.forKind(kind).first()
                    editing = newPreset(kind)
                }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.new_preset))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    editing?.let { preset ->
        PresetEditDialog(
            preset = preset,
            onSave = {
                onSave(it)
                editing = null
            },
            onDelete =
                if (!preset.isBuiltIn && preset.id != 0L) {
                    {
                        onDelete(preset)
                        editing = null
                    }
                } else {
                    null
                },
            onDismiss = { editing = null },
        )
    }
}

@Composable
fun SurfaceEditorScreen(
    surface: String,
    selected: List<Preset>,
    library: List<Preset>,
    onChange: (List<Long>) -> Unit,
    onCreatePreset: (Preset, (Long) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HintText(stringResource(surfaceHint(surface), Surfaces.maxFor(surface)))
        SectionCard {
            SurfaceEditor(selected, library, Surfaces.maxFor(surface), onChange, onCreatePreset = onCreatePreset)
        }
    }
}

fun surfaceTitle(surface: String): Int =
    when (surface) {
        Surfaces.HOME -> R.string.surface_home
        Surfaces.SHORTCUTS -> R.string.surface_shortcuts
        Surfaces.NOTIFICATION -> R.string.surface_notification
        else -> R.string.surface_widget
    }

private fun surfaceHint(surface: String): Int =
    when (surface) {
        Surfaces.HOME -> R.string.surface_home_hint
        Surfaces.SHORTCUTS -> R.string.surface_shortcuts_hint
        Surfaces.NOTIFICATION -> R.string.surface_notification_hint
        else -> R.string.surface_widget_hint
    }
