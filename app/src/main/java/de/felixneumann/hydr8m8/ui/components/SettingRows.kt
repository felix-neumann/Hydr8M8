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

@Composable
fun SettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface)
            if (supporting != null) {
                Text(supporting, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onChange(!checked) }
                .padding(vertical = 6.dp)
                .alpha(if (enabled) 1f else DISABLED_ALPHA),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface)
            if (supporting != null) {
                Text(supporting, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

private const val DISABLED_ALPHA = 0.5f

/** Row showing a time; tapping opens a time picker. */
@Composable
fun TimeSettingRow(
    label: String,
    time: LocalTime,
    onChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    SettingRow(label, Format.time(time), { open = true }, modifier, supporting)
    if (open) {
        TimePickerDialog(
            initial = time,
            onConfirm = {
                onChange(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

/** Row showing a number; tapping opens a stepped slider dialog. */
@Composable
fun NumberSettingRow(
    label: String,
    value: Int,
    range: IntRange,
    step: Int,
    format: (Int) -> String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    SettingRow(label, format(value), { open = true }, modifier, supporting)
    if (open) {
        SliderDialog(
            title = label,
            initial = value,
            range = range,
            step = step,
            format = format,
            onConfirm = {
                onChange(it)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

/** Row showing the selected option; tapping opens a single-choice dialog. */
@Composable
fun <T> ChoiceSettingRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    SettingRow(label, options.firstOrNull { it.first == selected }?.second.orEmpty(), { open = true }, modifier)
    if (open) {
        ChoiceDialog(
            title = label,
            options = options,
            selected = selected,
            onSelect = {
                open = false
                onSelect(it)
            },
            onDismiss = { open = false },
        )
    }
}
