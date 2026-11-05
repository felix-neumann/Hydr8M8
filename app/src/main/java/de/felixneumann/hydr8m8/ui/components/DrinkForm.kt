package de.felixneumann.hydr8m8.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.AlcoholMath
import de.felixneumann.hydr8m8.domain.BuiltInPresets
import de.felixneumann.hydr8m8.domain.DrinkIcon
import de.felixneumann.hydr8m8.domain.DrinkKind
import de.felixneumann.hydr8m8.domain.DrinkLimits
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.label
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import kotlin.math.abs

private const val DEFAULT_ABV = 5.0

/**
 * Editable drink (kind, icon, amount, ABV) shared by the entry sheet and the preset dialog. Picking
 * a kind or an icon pre-fills the vessel's built-in amount and ABV; values are snapped to the
 * [DrinkLimits] grid.
 */
@Stable
class DrinkDraftState(
    kind: DrinkKind,
    icon: DrinkIcon,
    volumeMl: Int,
    abvPercent: Double?,
) {
    var kind by mutableStateOf(kind)
        private set
    var icon by mutableStateOf(icon)
        private set
    var volumeMl by mutableIntStateOf(volumeMl)
        private set
    var abvPercent by mutableDoubleStateOf(abvPercent ?: DEFAULT_ABV)
        private set

    val isAlcohol: Boolean get() = kind == DrinkKind.ALCOHOL

    /** ABV to store: only alcohol carries one. */
    val storedAbv: Double? get() = if (isAlcohol) abvPercent else null

    fun selectKind(newKind: DrinkKind) {
        if (newKind == kind) return
        kind = newKind
        selectIcon(BuiltInPresets.defaultIcon(newKind))
    }

    fun selectIcon(newIcon: DrinkIcon) {
        icon = newIcon
        BuiltInPresets.templateFor(newIcon)?.let { t ->
            volumeMl = t.volumeMl
            t.abvPercent?.let { abvPercent = it }
        }
    }

    fun setVolume(ml: Float) {
        volumeMl = DrinkLimits.snapVolume(ml, kind)
    }

    fun addVolume(deltaMl: Int) = setVolume((volumeMl + deltaMl).toFloat())

    fun setAbv(percent: Double) {
        abvPercent = DrinkLimits.snapAbv(percent)
    }
}

@Composable
fun rememberDrinkDraft(
    kind: DrinkKind,
    icon: DrinkIcon,
    volumeMl: Int,
    abvPercent: Double?,
): DrinkDraftState = remember { DrinkDraftState(kind, icon, volumeMl, abvPercent) }

/** Water / alcohol filter chips. */
@Composable
fun DrinkKindChips(
    selected: DrinkKind,
    onSelect: (DrinkKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DrinkKind.entries.forEach { kind ->
            FilterChip(
                selected = selected == kind,
                onClick = { onSelect(kind) },
                label = { Text(stringResource(kind.label())) },
            )
        }
    }
}

/** Horizontal picker over the icon set of one [kind]. */
@Composable
fun DrinkIconPicker(
    kind: DrinkKind,
    selected: DrinkIcon,
    onSelect: (DrinkIcon) -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 30.dp,
) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DrinkIcon.forKind(kind)) { option ->
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected == option) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            ) {
                DrinkIconView(option, size = iconSize, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

/**
 * Complete drink editor: optional kind chips, icon picker, amount with slider (and optional ±
 * buttons) and, for alcohol, the ABV slider with the resulting goal bonus.
 */
@Composable
fun DrinkForm(
    state: DrinkDraftState,
    modifier: Modifier = Modifier,
    showKindChips: Boolean = true,
    showStepButtons: Boolean = false,
    amountSize: TextUnit = 40.sp,
) {
    Column(modifier) {
        if (showKindChips) {
            DrinkKindChips(state.kind, state::selectKind)
            Spacer(Modifier.height(12.dp))
        }
        DrinkIconPicker(state.kind, state.icon, state::selectIcon)
        Text(
            Format.volume(state.volumeMl),
            fontSize = amountSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
        )
        Slider(
            value = state.volumeMl.toFloat(),
            onValueChange = state::setVolume,
            valueRange = DrinkLimits.MIN_ML.toFloat()..DrinkLimits.maxMl(state.kind).toFloat(),
        )
        if (showStepButtons) VolumeStepButtons(state::addVolume)
        if (state.isAlcohol) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.abv_label, Format.abv(state.abvPercent)) + " · " +
                    stringResource(
                        R.string.goal_bonus,
                        Format.volume(AlcoholMath.goalBonusMl(state.volumeMl, state.abvPercent)),
                    ),
                color = HydrTheme.colors.alcohol,
            )
            Slider(
                value = state.abvPercent.toFloat(),
                onValueChange = { state.setAbv(it.toDouble()) },
                valueRange = DrinkLimits.MIN_ABV.toFloat()..DrinkLimits.MAX_ABV.toFloat(),
            )
        }
    }
}

@Composable
private fun VolumeStepButtons(onAdd: (Int) -> Unit) {
    val step = DrinkLimits.STEP_ML
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(-step * 5, -step, step, step * 5).forEach { delta ->
            OutlinedButton(onClick = { onAdd(delta) }, modifier = Modifier.weight(1f)) {
                Text((if (delta > 0) "+" else "−") + abs(delta))
            }
        }
    }
}
