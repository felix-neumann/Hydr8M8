package de.felixneumann.hydr8m8.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.History
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.components.SectionCard
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** One bar of a chart: label, value and the goal it is compared against. */
private data class Bar(
    val label: String,
    val valueMl: Int,
    val goalMl: Int,
)

private const val WEEK_DAYS = 7L
private const val MONTHS_PER_YEAR = 12L
private const val ALCOHOL_WEEKS = 8L

@Composable
fun StatsScreen(
    history: History,
    modifier: Modifier = Modifier,
) {
    val colors = HydrTheme.colors
    var range by rememberSaveable { mutableIntStateOf(0) }
    val locale = LocalConfiguration.current.locales[0]
    val bars = remember(history, range) { bars(history, range, locale) }
    val streak = history.streak

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.nav_stats),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                stringResource(R.string.stat_total),
                Format.litres(history.totalWaterMl, decimals = 1),
                Modifier.weight(1f),
            )
            StatTile(stringResource(R.string.stat_average), Format.litres(history.averagePerDayMl), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                stringResource(R.string.stat_streak),
                "🔥 ${streak.currentDays}",
                Modifier.weight(1f),
                sub = stringResource(R.string.stat_best, streak.bestDays),
            )
            StatTile(
                stringResource(R.string.jokers),
                "🛡 ${streak.jokers}",
                Modifier.weight(1f),
                sub =
                    pluralStringResource(
                        R.plurals.tracked_days,
                        history.trackedDays.toInt(),
                        history.trackedDays.toInt(),
                    ),
            )
        }

        SectionCard {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(R.string.range_week, R.string.range_month, R.string.range_year).forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = range == i,
                        onClick = { range = i },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                    ) { Text(stringResource(label)) }
                }
            }
            Spacer(Modifier.height(12.dp))
            BarChart(bars, colors.water, colors.track, colors.muted)
            val avg = bars.filter { it.valueMl > 0 }.map { it.valueMl }.average().takeIf { !it.isNaN() } ?: 0.0
            Text(
                stringResource(R.string.chart_average, Format.litres(avg.toInt())),
                fontSize = 12.sp,
                color = colors.muted,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        SectionCard(title = stringResource(R.string.alcohol_per_week)) {
            val weeks = remember(history) { alcoholWeeks(history) }
            BarChart(
                weeks.map { (start, grams) -> Bar("${start.dayOfMonth}.${start.monthValue}.", grams.toInt(), 0) },
                colors.alcohol,
                colors.track,
                colors.muted,
                valueLabel = { "${it}g" },
            )
            Text(stringResource(R.string.alcohol_hint), fontSize = 12.sp, color = colors.muted)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
) {
    SectionCard(modifier) {
        Text(label, fontSize = 12.sp, color = HydrTheme.colors.muted)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, fontSize = 11.sp, color = HydrTheme.colors.muted)
    }
}

@Composable
private fun BarChart(
    bars: List<Bar>,
    barColor: Color,
    trackColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    valueLabel: ((Int) -> String)? = null,
) {
    Column(modifier) {
        BarCanvas(bars, barColor, trackColor)
        BarLabels(bars, labelColor, valueLabel)
    }
}

@Composable
private fun BarCanvas(
    bars: List<Bar>,
    barColor: Color,
    trackColor: Color,
) {
    val max = (bars.maxOfOrNull { maxOf(it.valueMl, it.goalMl) } ?: 1).coerceAtLeast(1)
    val goalLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val gap = 4.dp.toPx()
        val barW = (size.width - gap * (bars.size - 1)) / bars.size.coerceAtLeast(1)
        bars.forEachIndexed { i, bar ->
            val x = i * (barW + gap)
            drawRoundRect(trackColor, Offset(x, 0f), Size(barW, size.height), CornerRadius(6f))
            val h = size.height * bar.valueMl / max
            val reached = bar.goalMl > 0 && bar.valueMl >= bar.goalMl
            drawRoundRect(
                if (reached || bar.goalMl == 0) barColor else barColor.copy(alpha = 0.55f),
                Offset(x, size.height - h),
                Size(barW, h),
                CornerRadius(6f),
            )
            if (bar.goalMl > 0) {
                val gy = size.height - size.height * bar.goalMl / max
                drawLine(
                    goalLineColor,
                    Offset(x, gy),
                    Offset(x + barW, gy),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                )
            }
        }
    }
}

@Composable
private fun BarLabels(
    bars: List<Bar>,
    labelColor: Color,
    valueLabel: ((Int) -> String)?,
) {
    Row(Modifier.fillMaxWidth()) {
        bars.forEach { bar ->
            Text(
                bar.label + (valueLabel?.let { "\n" + it(bar.valueMl) } ?: ""),
                modifier = Modifier.weight(1f),
                fontSize = 9.sp,
                color = labelColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

private fun bars(
    history: History,
    range: Int,
    locale: Locale,
): List<Bar> =
    when (range) {
        0 -> {
            history.days(history.today.minusDays(WEEK_DAYS - 1), history.today).map {
                Bar(it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), it.waterMl, it.goalMl)
            }
        }

        1 -> {
            val start = history.today.withDayOfMonth(1)
            history.days(start, start.plusMonths(1).minusDays(1)).map {
                val label = if (it.date.dayOfMonth % 5 == 0 || it.date.dayOfMonth == 1) "${it.date.dayOfMonth}" else ""
                if (it.date.isAfter(history.today)) Bar(label, 0, 0) else Bar(label, it.waterMl, it.goalMl)
            }
        }

        else -> {
            val current = YearMonth.from(history.today)
            (MONTHS_PER_YEAR - 1 downTo 0).map { back ->
                val ym = current.minusMonths(back)
                val end = minOf(ym.atEndOfMonth(), history.today)
                val days =
                    history.days(ym.atDay(1), end).filter { d ->
                        history.firstDate?.let { !d.date.isBefore(it) } ==
                            true
                    }
                val avg = if (days.isEmpty()) 0 else days.sumOf { it.waterMl } / days.size
                val goal = if (days.isEmpty()) 0 else days.sumOf { it.goalMl } / days.size
                Bar(ym.month.getDisplayName(TextStyle.NARROW, locale), avg, goal)
            }
        }
    }

/** Grams of alcohol per week (week starting Monday) for the last 8 weeks. */
private fun alcoholWeeks(history: History): List<Pair<LocalDate, Double>> {
    val thisWeek = history.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (ALCOHOL_WEEKS - 1 downTo 0).map { back ->
        val start = thisWeek.minusWeeks(back)
        val end = minOf(start.plusDays(WEEK_DAYS - 1), history.today)
        start to history.days(start, end).sumOf { it.alcoholGrams }
    }
}
