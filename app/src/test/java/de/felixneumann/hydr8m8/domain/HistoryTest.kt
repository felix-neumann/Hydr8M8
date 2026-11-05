package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HistoryTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val clock = DayClock(zone, DayClock.DEFAULT_DAY_START)
    private val day1 = LocalDate.of(2026, 11, 1)

    private var nextId = 1L

    private fun entry(
        dateTime: String,
        ml: Int,
        kind: DrinkKind = DrinkKind.WATER,
        abv: Double? = null,
    ): Entry {
        val at = LocalDateTime.parse(dateTime).atZone(zone).toInstant()
        val icon = if (kind == DrinkKind.WATER) DrinkIcon.GLASS else DrinkIcon.BEER
        return Entry(nextId++, at, kind, ml, abv, icon, at)
    }

    private fun history(
        entries: List<Entry>,
        today: LocalDate = day1.plusDays(2),
        weather: Map<LocalDate, Int> = emptyMap(),
    ) = History(clock, entries, listOf(GoalChange(day1, 2000)), weather, 2000, today)

    @Test
    fun `alcohol raises goal and does not count as water`() {
        val h = history(listOf(entry("2026-11-01T10:00", 1000), entry("2026-11-01T20:00", 500, DrinkKind.ALCOHOL, 5.0)))
        val d = h.day(day1)
        assertThat(d.waterMl).isEqualTo(1000)
        assertThat(d.goalMl).isEqualTo(2200)
        assertThat(d.hasAlcohol).isTrue()
    }

    @Test
    fun `weather bonus is added to the goal`() {
        val h = history(emptyList(), weather = mapOf(day1 to 250))
        assertThat(h.day(day1).goalMl).isEqualTo(2250)
    }

    @Test
    fun `late night drink counts for previous day`() {
        val h = history(listOf(entry("2026-11-02T01:30", 2000)))
        assertThat(h.day(day1).reached).isTrue()
        assertThat(h.day(day1.plusDays(1)).waterMl).isEqualTo(0)
    }

    @Test
    fun `back-filling a missed day restores the streak`() {
        val base =
            listOf(
                entry("2026-11-01T10:00", 2000),
                entry("2026-11-03T10:00", 2000),
            )
        assertThat(history(base, today = day1.plusDays(2)).streak.currentDays).isEqualTo(1)
        val backFilled = base + entry("2026-11-02T10:00", 2000)
        assertThat(history(backFilled, today = day1.plusDays(2)).streak.currentDays).isEqualTo(3)
    }

    @Test
    fun `totals and averages`() {
        val h = history(listOf(entry("2026-11-01T10:00", 1500), entry("2026-11-02T10:00", 1500)))
        assertThat(h.totalWaterMl).isEqualTo(3000)
        assertThat(h.trackedDays).isEqualTo(3)
        assertThat(h.averagePerDayMl).isEqualTo(1000)
    }
}
