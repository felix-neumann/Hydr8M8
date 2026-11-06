package de.felixneumann.hydr8m8.domain

import kotlin.math.roundToInt

enum class Sex { FEMALE, MALE, DIVERSE }

/** Body build ("Statur"); adjusts the normal weight like the classic frame-size correction (±10 %). */
enum class BodyBuild(
    val normalWeightFactor: Double,
) {
    SLIM(0.9),
    NORMAL(1.0),
    STOCKY(1.1),
}

data class BodyProfile(
    val sex: Sex,
    val ageYears: Int,
    val heightCm: Int,
    val weightKg: Int,
    val build: BodyBuild,
) {
    companion object {
        val AGE_RANGE = 14..100
        val HEIGHT_RANGE = 120..220
        val WEIGHT_RANGE = 35..200
        val DEFAULT = BodyProfile(Sex.FEMALE, 35, 170, 70, BodyBuild.NORMAL)
    }
}

/**
 * Recommended daily drinking amount from established references:
 *
 * 1. Reference weight: the body weight; with obesity (BMI ≥ 30) the adjusted body weight
 *    `normal + 0.4 × (weight − normal)`, because fat tissue holds little water. The normal weight
 *    follows Devine (by sex and height), corrected by the body build (±10 %).
 * 2. Total water need: DGE reference values per kg by age – 40 ml (under 19), 35 ml (19–50),
 *    30 ml (51+).
 * 3. Drinks: EFSA assumes ~20 % of water comes from food, so 80 % of the total is the drinking
 *    goal, rounded to 50 ml and kept within a safe range.
 */
object GoalCalculator {
    private const val CM_PER_INCH_BASE = 152.4 // 5 ft
    private const val DEVINE_KG_PER_CM = 2.3 / 2.54
    private const val DEVINE_BASE_MALE_KG = 50.0
    private const val DEVINE_BASE_FEMALE_KG = 45.5
    private const val OBESITY_BMI = 30.0
    private const val ADJUSTED_WEIGHT_SHARE = 0.4
    private const val DRINKS_SHARE = 0.8
    private const val ROUND_TO_ML = 50
    val RESULT_RANGE_ML = 1500..4000

    fun normalWeightKg(
        sex: Sex,
        heightCm: Int,
        build: BodyBuild,
    ): Double {
        val above = (heightCm - CM_PER_INCH_BASE).coerceAtLeast(0.0) * DEVINE_KG_PER_CM
        val base =
            when (sex) {
                Sex.MALE -> DEVINE_BASE_MALE_KG
                Sex.FEMALE -> DEVINE_BASE_FEMALE_KG
                Sex.DIVERSE -> (DEVINE_BASE_MALE_KG + DEVINE_BASE_FEMALE_KG) / 2
            }
        return (base + above) * build.normalWeightFactor
    }

    fun bmi(profile: BodyProfile): Double {
        val m = profile.heightCm / 100.0
        return profile.weightKg / (m * m)
    }

    fun referenceWeightKg(profile: BodyProfile): Double {
        if (bmi(profile) < OBESITY_BMI) return profile.weightKg.toDouble()
        val normal = normalWeightKg(profile.sex, profile.heightCm, profile.build)
        return if (profile.weightKg <= normal) {
            profile.weightKg.toDouble()
        } else {
            normal + ADJUSTED_WEIGHT_SHARE * (profile.weightKg - normal)
        }
    }

    /** DGE total water per kg body weight. */
    fun mlPerKg(ageYears: Int): Int =
        when {
            ageYears < 19 -> 40
            ageYears < 51 -> 35
            else -> 30
        }

    fun recommendedMl(profile: BodyProfile): Int {
        val total = referenceWeightKg(profile) * mlPerKg(profile.ageYears)
        val drinks = (total * DRINKS_SHARE / ROUND_TO_ML).roundToInt() * ROUND_TO_ML
        return drinks.coerceIn(RESULT_RANGE_ML)
    }
}
