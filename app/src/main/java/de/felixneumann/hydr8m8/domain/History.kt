package de.felixneumann.hydr8m8.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DaySummary(
    val date: LocalDate,
    val goal: DayGoal,
    val waterMl: Int,
    val entries: List<Entry>,
) {
    val goalMl: Int get() = goal.totalMl
    val percent: Int get() = Progress.percent(waterMl, goalMl)
    val reached: Boolean get() = Progress.isReached(waterMl, goalMl)
    val level: CalendarLevel get() = CalendarLevel.of(waterMl, goalMl)
    val hasAlcohol: Boolean get() = entries.any { it.kind == DrinkKind.ALCOHOL }
    val alcoholGrams: Double
        get() =
            entries.filter { it.kind == DrinkKind.ALCOHOL }.sumOf {
                AlcoholMath.gramsOfAlcohol(it.volumeMl, it.abvPercent ?: 0.0)
            }
}

/**
 * Everything derived from the raw data: per-day summaries, streak and totals. Recomputed from
 * scratch whenever data changes; the dataset (a few thousand entries per year) is small.
 */
class History(
    private val clock: DayClock,
    entries: List<Entry>,
    private val goalChanges: List<GoalChange>,
    private val weatherBonus: Map<LocalDate, Int>,
    private val fallbackGoalMl: Int,
    val today: LocalDate,
) {
    private val entriesByDay: Map<LocalDate, List<Entry>> =
        entries.sortedBy { it.at }.groupBy { clock.logicalDate(it.at) }

    val firstDate: LocalDate? = entriesByDay.keys.minOrNull()

    fun day(date: LocalDate): DaySummary {
        val dayEntries = entriesByDay[date].orEmpty()
        val goal =
            DayGoal(
                baseMl = GoalHistory.baseGoalOn(date, goalChanges, fallbackGoalMl),
                alcoholBonusMl = dayEntries.sumOf { it.alcoholGoalBonusMl },
                weatherBonusMl = weatherBonus[date] ?: 0,
            )
        return DaySummary(date, goal, dayEntries.sumOf { it.waterMl }, dayEntries)
    }

    val todaySummary: DaySummary by lazy { day(today) }

    val streak: StreakState by lazy {
        StreakCalculator.calculate(firstDate, today) { day(it).reached }
    }

    val totalWaterMl: Long by lazy { entriesByDay.values.sumOf { list -> list.sumOf { it.waterMl.toLong() } } }

    /** Days since first use, including today. */
    val trackedDays: Long
        get() = firstDate?.let { ChronoUnit.DAYS.between(it, today) + 1 } ?: 0

    val averagePerDayMl: Int
        get() = if (trackedDays > 0) (totalWaterMl / trackedDays).toInt() else 0

    fun days(
        from: LocalDate,
        toInclusive: LocalDate,
    ): List<DaySummary> =
        generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(toInclusive) }
            .map { day(it) }
            .toList()
}
