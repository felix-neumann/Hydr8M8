package de.felixneumann.hydr8m8.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.CalendarLevel
import de.felixneumann.hydr8m8.domain.Entry
import de.felixneumann.hydr8m8.domain.History
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.components.DrinkIconView
import de.felixneumann.hydr8m8.ui.components.SectionCard
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private const val DAYS_PER_WEEK = 7

@Composable
fun CalendarScreen(
    history: History,
    onEditEntry: (Entry) -> Unit,
    onAddForDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.from(history.today)) }
    var selected by rememberSaveable { mutableStateOf(history.today) }
    val colors = HydrTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_month))
            }
            Text(
                month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.from(history.today)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_month))
            }
        }
        val monthDays = history.days(month.atDay(1), month.atEndOfMonth().coerceAtMost(history.today))
        val reachedCount = monthDays.count { it.reached }
        Text(
            pluralStringResource(R.plurals.calendar_summary, monthDays.size, reachedCount, monthDays.size),
            fontSize = 12.sp,
            color = colors.muted,
            modifier = Modifier.padding(start = 12.dp),
        )

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            (0 until DAYS_PER_WEEK).forEach { i ->
                val dow = firstDayOfWeek.plus(i.toLong())
                Text(
                    dow.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = colors.muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        MonthGrid(
            month = month,
            firstDayOfWeek = firstDayOfWeek,
            history = history,
            selected = selected,
            onSelect = { selected = it },
        )
        Legend()

        Spacer(Modifier.height(14.dp))
        DayDetail(history, selected, onEditEntry, onAddForDay)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    history: History,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = HydrTheme.colors
    val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + DAYS_PER_WEEK) % DAYS_PER_WEEK
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    cells.chunked(DAYS_PER_WEEK).forEach { week ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            week.forEach { date ->
                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                    if (date != null) {
                        val future = date.isAfter(history.today)
                        val summary = if (future) null else history.day(date)
                        val level = summary?.level ?: CalendarLevel.NONE
                        var cell =
                            Modifier
                                .fillMaxSize()
                                .alpha(if (future) 0.35f else 1f)
                                .background(colors.level(level), CircleShape)
                        if (level ==
                            CalendarLevel.REACHED
                        ) {
                            cell =
                                cell.border(2.dp, colors.levelReached.copy(alpha = 0.5f), CircleShape).padding(3.dp)
                        }
                        if (date ==
                            selected
                        ) {
                            cell = cell.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                        }
                        Box(
                            cell.clickable(enabled = !future) { onSelect(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                date.dayOfMonth.toString(),
                                fontSize = 12.sp,
                                fontWeight = if (date == history.today) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = colors.onLevel(level),
                            )
                        }
                        if (date in history.streak.jokerDays) {
                            Text("🛡", fontSize = 10.sp, modifier = Modifier.align(Alignment.TopEnd))
                        }
                    }
                }
            }
            repeat(DAYS_PER_WEEK - week.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun Legend() {
    val colors = HydrTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            CalendarLevel.NONE to "0",
            CalendarLevel.LOW to "1–90 %",
            CalendarLevel.NEAR to "91–99 %",
            CalendarLevel.REACHED to "100 %+",
        ).forEach { (level, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(11.dp).background(colors.level(level), CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 10.sp, color = colors.muted)
            }
        }
        Text("🛡 " + stringResource(R.string.joker), fontSize = 10.sp, color = colors.muted)
    }
}

@Composable
private fun DayDetail(
    history: History,
    date: LocalDate,
    onEditEntry: (Entry) -> Unit,
    onAddForDay: (LocalDate) -> Unit,
) {
    val day = remember(history, date) { history.day(date) }
    val colors = HydrTheme.colors
    SectionCard {
        Text(
            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) + " · ${day.percent} %",
            fontWeight = FontWeight.Bold,
        )
        Text(
            "${Format.litres(day.waterMl)} / ${Format.litres(day.goalMl)}",
            fontSize = 12.sp,
            color = colors.muted,
        )
        if (date in history.streak.jokerDays) {
            Text("🛡 " + stringResource(R.string.saved_by_joker), fontSize = 12.sp, color = colors.muted)
        }
        Spacer(Modifier.height(6.dp))
        if (day.entries.isEmpty()) {
            Text(stringResource(R.string.no_entries), fontSize = 13.sp, color = colors.muted)
        }
        day.entries.forEach { entry ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onEditEntry(entry) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrinkIconView(entry.icon, size = 22.dp)
                Spacer(Modifier.width(10.dp))
                Text(Format.time(entry.at), modifier = Modifier.width(64.dp), color = colors.muted, fontSize = 13.sp)
                Text(
                    Format.volume(entry.volumeMl) + (entry.abvPercent?.let { " · " + Format.abv(it) } ?: ""),
                    modifier = Modifier.weight(1f),
                )
                if (entry.alcoholGoalBonusMl > 0) {
                    Text("+" + Format.volume(entry.alcoholGoalBonusMl), fontSize = 12.sp, color = colors.alcohol)
                }
                Text("  ✎", color = colors.muted)
            }
        }
        TextButton(onClick = { onAddForDay(date) }) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Unspecified)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_entry_for_day))
        }
    }
}
