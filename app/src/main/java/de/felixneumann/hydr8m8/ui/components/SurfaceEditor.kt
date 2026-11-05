package de.felixneumann.hydr8m8.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.DrinkIcon
import de.felixneumann.hydr8m8.domain.MateMood
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.drawable
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Editor for one surface's preset selection: drag the handle to reorder, remove with ×, add from
 * the library (up to [max]).
 */
@Composable
fun SurfaceEditor(
    selected: List<Preset>,
    library: List<Preset>,
    max: Int,
    onChange: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    onCreatePreset: ((Preset, (Long) -> Unit) -> Unit)? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val currentSelected by rememberUpdatedState(selected)
    val currentOnChange by rememberUpdatedState(onChange)
    Column(modifier) {
        ReorderableColumn(
            items = selected,
            key = { it.id },
            onMove = { from, to ->
                val list = currentSelected.toMutableList()
                list.add(to, list.removeAt(from))
                onChange(list.map { it.id })
            },
        ) { preset, handle ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = handle.padding(6.dp),
                )
                DrinkIconView(preset.icon)
                Spacer(Modifier.width(10.dp))
                Text(Format.presetLabel(preset), Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { onChange(currentSelected.filter { it.id != preset.id }.map { it.id }) }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove))
                }
            }
        }
        val canAdd = selected.size < max
        TextButton(onClick = { showPicker = true }, enabled = canAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(
                if (canAdd) stringResource(R.string.add_amount) else stringResource(R.string.max_reached, max),
            )
        }
    }
    if (showPicker) {
        val available = library.filter { p -> !p.hidden && selected.none { it.id == p.id } }
        PresetPickerDialog(
            presets = available,
            onPick = {
                onChange(selected.map { p -> p.id } + it.id)
                showPicker = false
            },
            onDismiss = { showPicker = false },
            onCreateNew =
                onCreatePreset?.let {
                    {
                        showPicker = false
                        creating = true
                    }
                },
        )
    }
    if (creating && onCreatePreset != null) {
        PresetEditDialog(
            preset = newPreset(),
            onSave = { preset ->
                creating = false
                onCreatePreset(preset) { id -> currentOnChange(currentSelected.map { it.id } + id) }
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
fun PresetPickerDialog(
    presets: List<Preset>,
    onPick: (Preset) -> Unit,
    onDismiss: () -> Unit,
    onCreateNew: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (onCreateNew != null) {
                TextButton(onClick = onCreateNew) { Text(stringResource(R.string.create_own_amount)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        title = { Text(stringResource(R.string.choose_amount)) },
        text = {
            LazyColumn {
                items(presets, key = { it.id }) { preset ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(preset) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DrinkIconView(preset.icon)
                        Spacer(Modifier.width(12.dp))
                        Text(Format.presetLabel(preset))
                    }
                }
            }
        },
    )
}

/** Simple drag-to-reorder column for short lists (≤ 8 items). */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, handle: Modifier) -> Unit,
) {
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<Any, Int>() }
    val currentItems by rememberUpdatedState(items)
    val currentOnMove by rememberUpdatedState(onMove)

    Column(modifier) {
        items.forEach { item ->
            val k = key(item)
            key(k) {
                val dragging = draggingKey == k
                val handle =
                    Modifier.pointerInput(k) {
                        detectDragGestures(
                            onDragStart = {
                                draggingKey = k
                                offset = 0f
                            },
                            onDragEnd = { draggingKey = null },
                            onDragCancel = { draggingKey = null },
                        ) { change, drag ->
                            change.consume()
                            offset += drag.y
                            val index = currentItems.indexOfFirst { key(it) == k }
                            val next = currentItems.getOrNull(index + 1)?.let { heights[key(it)] }
                            val prev = currentItems.getOrNull(index - 1)?.let { heights[key(it)] }
                            if (next != null && offset > next / 2f) {
                                currentOnMove(index, index + 1)
                                offset -= next
                            } else if (prev != null && offset < -prev / 2f) {
                                currentOnMove(index, index - 1)
                                offset += prev
                            }
                        }
                    }
                Surface(
                    color = if (dragging) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                    modifier =
                        Modifier
                            .onSizeChanged { heights[k] = it.height }
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer { translationY = if (dragging) offset else 0f },
                ) {
                    itemContent(item, handle)
                }
            }
        }
    }
}
