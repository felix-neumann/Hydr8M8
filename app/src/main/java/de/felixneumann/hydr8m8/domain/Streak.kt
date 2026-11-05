package de.felixneumann.hydr8m8.domain

import java.time.LocalDate
import java.time.Period

data class StreakState(
    /** Successful days in the current streak (joker days excluded). */
    val currentDays: Int = 0,
    val currentStart: LocalDate? = null,
    val jokers: Int = 0,
    /** Days that were saved by a joker, across the whole history. */
    val jokerDays: Set<LocalDate> = emptySet(),
    val bestDays: Int = 0,
    /** True if today already reached the goal and is part of [currentDays]. */
    val todayCounted: Boolean = false,
)

/**
 * Derives the streak from scratch out of day results, so back-filled entries automatically fix
 * (and refund jokers for) past days.
 *
 * Rules: a successful day adds to the streak; every 7th counted day earns a joker (max 3). A missed
 * day consumes a joker (the day itself does not count). Without a joker the streak ends and all
 * jokers are lost. Today only counts once successful and never breaks the streak while running.
 */
object StreakCalculator {
    const val DAYS_PER_JOKER = 7
    const val MAX_JOKERS = 3

    fun calculate(
        firstDate: LocalDate?,
        today: LocalDate,
        isSuccess: (LocalDate) -> Boolean,
    ): StreakState {
        if (firstDate == null || firstDate.isAfter(today)) return StreakState()

        var count = 0
        var start: LocalDate? = null
        var jokers = 0
        var best = 0
        val jokerDays = mutableSetOf<LocalDate>()

        fun countSuccess(day: LocalDate) {
            if (count == 0) start = day
            count++
            if (count % DAYS_PER_JOKER == 0) jokers = minOf(MAX_JOKERS, jokers + 1)
            best = maxOf(best, count)
        }

        var day: LocalDate = firstDate
        while (day.isBefore(today)) {
            when {
                isSuccess(day) -> {
                    countSuccess(day)
                }

                count > 0 && jokers > 0 -> {
                    jokers--
                    jokerDays += day
                }

                else -> {
                    count = 0
                    start = null
                    jokers = 0
                }
            }
            day = day.plusDays(1)
        }

        val todaySuccess = isSuccess(today)
        if (todaySuccess) countSuccess(today)

        return StreakState(
            currentDays = count,
            currentStart = start,
            jokers = jokers,
            jokerDays = jokerDays,
            bestDays = best,
            todayCounted = todaySuccess,
        )
    }
}

enum class MilestoneType { WEEK, MONTH, HALF_YEAR, YEAR }

data class Milestone(
    val type: MilestoneType,
    /** Full calendar months since the streak start (0 for [MilestoneType.WEEK]). */
    val months: Int,
    val streakStart: LocalDate,
) {
    /** Stable identity used to notify each milestone only once. */
    val key: String get() = "$streakStart:$type:$months"
}

object Milestones {
    private const val WEEK_DAYS = 7
    private const val HALF_YEAR_MONTHS = 6
    private const val YEAR_MONTHS = 12

    /** The highest milestone the current streak has reached, or null if none yet. */
    fun highestReached(
        streakStart: LocalDate?,
        streakDays: Int,
        lastCoveredDay: LocalDate,
    ): Milestone? {
        if (streakStart == null || streakDays < WEEK_DAYS) return null
        val months = Period.between(streakStart, lastCoveredDay.plusDays(1)).toTotalMonths().toInt()
        val type =
            when {
                months <= 0 -> MilestoneType.WEEK
                months % YEAR_MONTHS == 0 -> MilestoneType.YEAR
                months % HALF_YEAR_MONTHS == 0 -> MilestoneType.HALF_YEAR
                else -> MilestoneType.MONTH
            }
        return Milestone(type, maxOf(months, 0), streakStart)
    }
}
