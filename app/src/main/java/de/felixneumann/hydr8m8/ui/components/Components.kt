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
fun DrinkIconView(
    icon: DrinkIcon,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = HydrTheme.colors.forKind(icon.kind),
) {
    Icon(
        painter = painterResource(icon.drawable()),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}

@Composable
fun MateView(
    mood: MateMood,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Image(painter = painterResource(mood.drawable()), contentDescription = null, modifier = modifier.size(size))
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            leading?.invoke()
            Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Tappable preset tile used on the home screen and in pickers. */
@Composable
fun PresetTile(
    preset: Preset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (dashed) 0.5f else 1f)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DrinkIconView(preset.icon, size = 26.dp)
            Spacer(Modifier.size(3.dp))
            Text(
                Format.presetLabel(preset),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            if (title != null) {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(8.dp))
            }
            content()
        }
    }
}

/** Centered empty-state style helper text. */
@Composable
fun HintText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier = modifier,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun TintedIcon(
    res: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
