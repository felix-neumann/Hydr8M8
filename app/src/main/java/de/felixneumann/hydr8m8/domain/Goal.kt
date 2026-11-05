package de.felixneumann.hydr8m8.domain

import java.time.LocalDate
import kotlin.math.roundToInt

/** Alcohol does not count as intake; it raises the day goal to compensate its diuretic effect. */
object AlcoholMath {
    private const val ETHANOL_DENSITY_G_PER_ML = 0.789
    private const val EXTRA_WATER_ML_PER_GRAM = 10.0

    fun gramsOfAlcohol(
        volumeMl: Int,
        abvPercent: Double,
    ): Double = volumeMl * abvPercent / 100.0 * ETHANOL_DENSITY_G_PER_ML

    fun goalBonusMl(
        volumeMl: Int,
        abvPercent: Double,
    ): Int = roundToTen(gramsOfAlcohol(volumeMl, abvPercent) * EXTRA_WATER_ML_PER_GRAM)

    private fun roundToTen(value: Double): Int = (value / 10.0).roundToInt() * 10
}

/** Extra water on warm days, determined once per morning from the forecast max temperature. */
data class WeatherRules(
    val enabled: Boolean = true,
    val warmThresholdC: Double = 25.0,
    val warmBonusMl: Int = 250,
    val hotThresholdC: Double = 30.0,
    val hotBonusMl: Int = 500,
) {
    fun bonusFor(maxTempC: Double): Int =
        when {
            !enabled -> 0
            maxTempC >= hotThresholdC -> hotBonusMl
            maxTempC >= warmThresholdC -> warmBonusMl
            else -> 0
        }
}

data class DayGoal(
    val baseMl: Int,
    val alcoholBonusMl: Int = 0,
    val weatherBonusMl: Int = 0,
) {
    val totalMl: Int get() = baseMl + alcoholBonusMl + weatherBonusMl
}

/** A base-goal change that applies from [effectiveFrom] onwards. Past days keep their old goal. */
data class GoalChange(
    val effectiveFrom: LocalDate,
    val goalMl: Int,
)

object GoalHistory {
    fun baseGoalOn(
        date: LocalDate,
        changes: List<GoalChange>,
        fallbackMl: Int,
    ): Int {
        val sorted = changes.sortedBy { it.effectiveFrom }
        return sorted.lastOrNull { !it.effectiveFrom.isAfter(date) }?.goalMl
            ?: sorted.firstOrNull()?.goalMl
            ?: fallbackMl
    }
}

object Progress {
    /** Floored percentage, so 100 only shows when the goal is truly reached. */
    fun percent(
        intakeMl: Int,
        goalMl: Int,
    ): Int = if (goalMl <= 0) 0 else (intakeMl.toLong() * 100 / goalMl).toInt()

    fun isReached(
        intakeMl: Int,
        goalMl: Int,
    ): Boolean = goalMl > 0 && intakeMl >= goalMl
}

enum class CalendarLevel {
    NONE,
    LOW,
    NEAR,
    REACHED,
    ;

    companion object {
        private const val NEAR_FROM_PERCENT = 91

        fun of(
            intakeMl: Int,
            goalMl: Int,
        ): CalendarLevel =
            when {
                intakeMl <= 0 -> NONE
                Progress.isReached(intakeMl, goalMl) -> REACHED
                Progress.percent(intakeMl, goalMl) >= NEAR_FROM_PERCENT -> NEAR
                else -> LOW
            }
    }
}
