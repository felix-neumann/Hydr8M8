package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GoalCalculatorTest {
    private fun profile(
        sex: Sex = Sex.MALE,
        age: Int = 35,
        height: Int = 180,
        weight: Int = 80,
        build: BodyBuild = BodyBuild.NORMAL,
    ) = BodyProfile(sex, age, height, weight, build)

    @Test
    fun `normal weight adult uses 35 ml per kg and 80 percent from drinks`() {
        // 80 kg * 35 ml = 2800 ml total -> 2240 ml drinks -> 2250 ml
        assertThat(GoalCalculator.recommendedMl(profile())).isEqualTo(2250)
        // 65 kg * 35 ml = 2275 ml -> 1820 ml -> 1800 ml
        assertThat(GoalCalculator.recommendedMl(profile(sex = Sex.FEMALE, height = 168, weight = 65))).isEqualTo(1800)
    }

    @Test
    fun `age groups follow DGE values`() {
        assertThat(GoalCalculator.mlPerKg(16)).isEqualTo(40)
        assertThat(GoalCalculator.mlPerKg(19)).isEqualTo(35)
        assertThat(GoalCalculator.mlPerKg(50)).isEqualTo(35)
        assertThat(GoalCalculator.mlPerKg(51)).isEqualTo(30)
        // 80 kg * 30 ml = 2400 -> 1920 -> 1900
        assertThat(GoalCalculator.recommendedMl(profile(age = 60))).isEqualTo(1900)
    }

    @Test
    fun `devine normal weight by sex height and build`() {
        assertThat(GoalCalculator.normalWeightKg(Sex.MALE, 180, BodyBuild.NORMAL)).isWithin(0.1).of(75.0)
        assertThat(GoalCalculator.normalWeightKg(Sex.FEMALE, 180, BodyBuild.NORMAL)).isWithin(0.1).of(70.5)
        assertThat(GoalCalculator.normalWeightKg(Sex.DIVERSE, 180, BodyBuild.NORMAL)).isWithin(0.1).of(72.75)
        assertThat(GoalCalculator.normalWeightKg(Sex.MALE, 180, BodyBuild.STOCKY)).isWithin(0.1).of(82.5)
        assertThat(GoalCalculator.normalWeightKg(Sex.MALE, 180, BodyBuild.SLIM)).isWithin(0.1).of(67.5)
    }

    @Test
    fun `obesity uses adjusted body weight`() {
        val obese = profile(weight = 120) // BMI 37
        // normal 75 kg -> 75 + 0.4 * 45 = 93 kg -> 3255 ml total -> 2604 -> 2600
        assertThat(GoalCalculator.referenceWeightKg(obese)).isWithin(0.1).of(93.0)
        assertThat(GoalCalculator.recommendedMl(obese)).isEqualTo(2600)
        // A stocky build raises the normal weight and with it the goal.
        assertThat(GoalCalculator.recommendedMl(obese.copy(build = BodyBuild.STOCKY)))
            .isGreaterThan(GoalCalculator.recommendedMl(obese))
    }

    @Test
    fun `build does not change the goal below the obesity threshold`() {
        assertThat(GoalCalculator.recommendedMl(profile(build = BodyBuild.SLIM)))
            .isEqualTo(GoalCalculator.recommendedMl(profile(build = BodyBuild.STOCKY)))
    }

    @Test
    fun `result stays within a safe range`() {
        assertThat(GoalCalculator.recommendedMl(profile(weight = 35, height = 150, age = 80))).isEqualTo(1500)
        assertThat(
            GoalCalculator.recommendedMl(profile(weight = 200, height = 220, age = 16, build = BodyBuild.STOCKY)),
        )
            .isAtMost(4000)
    }
}
