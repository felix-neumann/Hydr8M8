package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class GoalTest {
    @Test
    fun `half litre beer at 5 percent raises goal by 200 ml`() {
        assertThat(AlcoholMath.goalBonusMl(volumeMl = 500, abvPercent = 5.0)).isEqualTo(200)
    }

    @Test
    fun `shot of 2 cl at 40 percent raises goal by 60 ml`() {
        // 20 ml * 0.40 * 0.789 = 6.3 g -> 63 ml -> rounded to 60 ml
        assertThat(AlcoholMath.goalBonusMl(volumeMl = 20, abvPercent = 40.0)).isEqualTo(60)
    }

    @Test
    fun `weather bonus follows thresholds`() {
        val rules = WeatherRules()
        assertThat(rules.bonusFor(24.9)).isEqualTo(0)
        assertThat(rules.bonusFor(25.0)).isEqualTo(250)
        assertThat(rules.bonusFor(29.9)).isEqualTo(250)
        assertThat(rules.bonusFor(30.0)).isEqualTo(500)
        assertThat(rules.copy(enabled = false).bonusFor(35.0)).isEqualTo(0)
    }

    @Test
    fun `day goal sums base alcohol and weather`() {
        assertThat(DayGoal(baseMl = 2200, alcoholBonusMl = 200, weatherBonusMl = 250).totalMl).isEqualTo(2650)
    }

    @Test
    fun `base goal comes from latest change on or before the date`() {
        val changes =
            listOf(
                GoalChange(LocalDate.of(2026, 1, 1), 2000),
                GoalChange(LocalDate.of(2026, 3, 1), 2500),
            )
        assertThat(GoalHistory.baseGoalOn(LocalDate.of(2026, 2, 28), changes, fallbackMl = 1)).isEqualTo(2000)
        assertThat(GoalHistory.baseGoalOn(LocalDate.of(2026, 3, 1), changes, fallbackMl = 1)).isEqualTo(2500)
        // Days before the first change use the first known goal.
        assertThat(GoalHistory.baseGoalOn(LocalDate.of(2025, 12, 1), changes, fallbackMl = 1)).isEqualTo(2000)
        assertThat(GoalHistory.baseGoalOn(LocalDate.of(2025, 12, 1), emptyList(), fallbackMl = 1800)).isEqualTo(1800)
    }

    @Test
    fun `percent is floored so that 100 means goal truly reached`() {
        assertThat(Progress.percent(intakeMl = 1995, goalMl = 2000)).isEqualTo(99)
        assertThat(Progress.percent(intakeMl = 2000, goalMl = 2000)).isEqualTo(100)
    }

    @Test
    fun `calendar levels`() {
        assertThat(CalendarLevel.of(0, 2000)).isEqualTo(CalendarLevel.NONE)
        assertThat(CalendarLevel.of(10, 2000)).isEqualTo(CalendarLevel.LOW)
        assertThat(CalendarLevel.of(1819, 2000)).isEqualTo(CalendarLevel.LOW) // 90 %
        assertThat(CalendarLevel.of(1820, 2000)).isEqualTo(CalendarLevel.NEAR) // 91 %
        assertThat(CalendarLevel.of(1999, 2000)).isEqualTo(CalendarLevel.NEAR)
        assertThat(CalendarLevel.of(2000, 2000)).isEqualTo(CalendarLevel.REACHED)
        assertThat(CalendarLevel.of(3000, 2000)).isEqualTo(CalendarLevel.REACHED)
    }
}
