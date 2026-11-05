package de.felixneumann.hydr8m8.ui.today

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.DaySummary
import de.felixneumann.hydr8m8.domain.MateMood
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.domain.StreakState
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.components.MateView
import de.felixneumann.hydr8m8.ui.components.Pill
import de.felixneumann.hydr8m8.ui.components.PresetTile
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    date: LocalDate,
    today: DaySummary,
    streak: StreakState,
    mood: MateMood,
    nextReminder: Instant?,
    homePresets: List<Preset>,
    onPresetClick: (Preset) -> Unit,
    onConfigureHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HydrTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
            fontSize = 13.sp,
            color = colors.muted,
        )
        Text(
            stringResource(R.string.today),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(width = 170.dp, height = 230.dp)) {
                Glass(
                    fraction = if (today.goalMl > 0) today.waterMl.toFloat() / today.goalMl else 0f,
                    percent = today.percent,
                    modifier = Modifier.fillMaxSize().padding(top = 24.dp),
                )
                MateView(
                    mood = mood,
                    size = 64.dp,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 22.dp, y = (-8).dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp)) {
                    append(Format.litres(today.waterMl))
                }
                withStyle(SpanStyle(color = colors.muted, fontSize = 15.sp)) {
                    append("  / " + Format.litres(today.goalMl))
                }
            },
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        GoalLine(today, Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (nextReminder != null) {
                Pill(Format.time(nextReminder), leading = {
                    Icon(Icons.Default.NotificationsNone, null, Modifier.size(16.dp), tint = colors.muted)
                })
            }
            Pill(pluralStringResourceSafe(R.plurals.streak_days, streak.currentDays))
            Pill("× ${streak.jokers} " + stringResource(R.string.jokers), leading = {
                Icon(Icons.Outlined.Shield, null, Modifier.size(16.dp), tint = colors.muted)
            })
        }

        Spacer(Modifier.height(14.dp))
        if (homePresets.isEmpty()) {
            TextButton(onClick = onConfigureHome) { Text(stringResource(R.string.configure_home_presets)) }
        } else {
            homePresets.chunked(PRESETS_PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    row.forEach { preset ->
                        PresetTile(preset, onClick = { onPresetClick(preset) }, modifier = Modifier.weight(1f))
                    }
                    repeat(PRESETS_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

private const val PRESETS_PER_ROW = 4

@Composable
private fun pluralStringResourceSafe(
    id: Int,
    count: Int,
): String = androidx.compose.ui.res.pluralStringResource(id, count, count)

@Composable
private fun GoalLine(
    today: DaySummary,
    modifier: Modifier = Modifier,
) {
    val colors = HydrTheme.colors
    val goal = today.goal
    Text(
        buildAnnotatedString {
            append(stringResource(R.string.goal_label, Format.litres(goal.baseMl)))
            if (goal.alcoholBonusMl > 0) {
                withStyle(SpanStyle(color = colors.alcohol)) {
                    append("  +" + Format.litres(goal.alcoholBonusMl) + " " + stringResource(R.string.alcohol_short))
                }
            }
            if (goal.weatherBonusMl > 0) {
                withStyle(SpanStyle(color = colors.warm)) {
                    append("  +" + Format.litres(goal.weatherBonusMl) + " ☀")
                }
            }
        },
        fontSize = 12.sp,
        color = colors.muted,
        modifier = modifier,
    )
}

/** Tapered glass whose animated water level shows progress. */
@Composable
fun Glass(
    fraction: Float,
    percent: Int,
    modifier: Modifier = Modifier,
) {
    val colors = HydrTheme.colors
    val level by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 60f),
        label = "level",
    )
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(WAVE_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val stroke = 3.dp.toPx()
            val taper = w * 0.12f
            val outline =
                Path().apply {
                    moveTo(stroke, stroke)
                    lineTo(w - stroke, stroke)
                    lineTo(w - taper, h - stroke)
                    lineTo(taper, h - stroke)
                    close()
                }
            val waterTop = h - stroke - (h - 2 * stroke) * level
            clipPath(outline) {
                if (level > 0f) {
                    for (layer in 0..1) {
                        val amp = h * if (layer == 0) 0.022f else 0.016f
                        val shift = if (layer == 0) phase else -phase * 1.3f
                        val wave =
                            Path().apply {
                                moveTo(0f, h)
                                lineTo(0f, waterTop)
                                var x = 0f
                                while (x <= w) {
                                    lineTo(x, waterTop + amp * sin(x / w * 2 * PI.toFloat() * 1.5f + shift))
                                    x += 4f
                                }
                                lineTo(w, h)
                                close()
                            }
                        drawPath(
                            wave,
                            brush =
                                if (layer == 0) {
                                    Brush.verticalGradient(
                                        listOf(colors.waterLight, colors.waterDeep),
                                        startY = waterTop,
                                        endY = h,
                                    )
                                } else {
                                    SolidColor(colors.waterDeep.copy(alpha = 0.5f))
                                },
                        )
                    }
                    // A few bubbles for life.
                    val bubbleY = waterTop + (h - waterTop) * (0.5f + 0.3f * sin(phase))
                    if (bubbleY < h - stroke * 3) {
                        drawCircle(
                            colors.glassStroke.copy(alpha = 0.6f),
                            radius = 3.dp.toPx(),
                            center = Offset(w * 0.35f, bubbleY),
                        )
                    }
                }
            }
            drawPath(outline, colors.glassStroke, style = Stroke(width = stroke, join = StrokeJoin.Round))
        }
        Text(
            "$percent %",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = if (level > TEXT_ON_WATER_LEVEL) Color.White else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 60.dp),
        )
    }
}

private const val WAVE_PERIOD_MS = 2600
private const val TEXT_ON_WATER_LEVEL = 0.45f
