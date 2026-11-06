package de.felixneumann.hydr8m8.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.BodyBuild
import de.felixneumann.hydr8m8.domain.BodyProfile
import de.felixneumann.hydr8m8.domain.GoalCalculator
import de.felixneumann.hydr8m8.domain.Sex
import de.felixneumann.hydr8m8.ui.Format
import kotlin.math.roundToInt

/**
 * Body data for the goal calculator (sex, age, height, weight, build) with the resulting
 * recommendation shown live. Used by onboarding and settings.
 */
@Composable
fun BodyProfileForm(
    profile: BodyProfile,
    onChange: (BodyProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        ChoiceChips(
            options = Sex.entries.map { it to stringResource(it.label()) },
            selected = profile.sex,
            onSelect = { onChange(profile.copy(sex = it)) },
        )
        LabeledSlider(
            stringResource(R.string.profile_age, profile.ageYears),
            profile.ageYears,
            BodyProfile.AGE_RANGE,
            onChange = { onChange(profile.copy(ageYears = it)) },
        )
        LabeledSlider(
            stringResource(R.string.profile_height, profile.heightCm),
            profile.heightCm,
            BodyProfile.HEIGHT_RANGE,
            onChange = { onChange(profile.copy(heightCm = it)) },
        )
        LabeledSlider(
            stringResource(R.string.profile_weight, profile.weightKg),
            profile.weightKg,
            BodyProfile.WEIGHT_RANGE,
            onChange = { onChange(profile.copy(weightKg = it)) },
        )
        Text(
            stringResource(R.string.profile_build),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChoiceChips(
            options = BodyBuild.entries.map { it to stringResource(it.label()) },
            selected = profile.build,
            onSelect = { onChange(profile.copy(build = it)) },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.profile_result, Format.litres(GoalCalculator.recommendedMl(profile))),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.profile_explanation),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Single-choice chips in a wrapping row. */
@Composable
fun <T> ChoiceChips(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

/** Integer slider with its current value shown in [label]. */
@Composable
fun LabeledSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(label, fontSize = 13.sp)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}

private fun Sex.label(): Int =
    when (this) {
        Sex.FEMALE -> R.string.sex_female
        Sex.MALE -> R.string.sex_male
        Sex.DIVERSE -> R.string.sex_diverse
    }

private fun BodyBuild.label(): Int =
    when (this) {
        BodyBuild.SLIM -> R.string.build_slim
        BodyBuild.NORMAL -> R.string.build_normal
        BodyBuild.STOCKY -> R.string.build_stocky
    }
