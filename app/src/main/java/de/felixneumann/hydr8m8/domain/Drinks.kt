package de.felixneumann.hydr8m8.domain

import java.time.Instant
import kotlin.math.roundToInt

enum class DrinkKind { WATER, ALCOHOL }

/** The shared icon set. Every preset and entry carries one of these. */
enum class DrinkIcon(
    val kind: DrinkKind,
) {
    GLASS_SMALL(DrinkKind.WATER),
    GLASS(DrinkKind.WATER),
    MUG(DrinkKind.WATER),
    GLASS_LARGE(DrinkKind.WATER),
    BOTTLE_SMALL(DrinkKind.WATER),
    BOTTLE(DrinkKind.WATER),
    SPORTS_BOTTLE(DrinkKind.WATER),
    CARAFE(DrinkKind.WATER),
    BOTTLE_LARGE(DrinkKind.WATER),
    SPARKLING(DrinkKind.ALCOHOL),
    WINE(DrinkKind.ALCOHOL),
    BEER(DrinkKind.ALCOHOL),
    WHEAT_BEER(DrinkKind.ALCOHOL),
    LONG_DRINK(DrinkKind.ALCOHOL),
    SHOT(DrinkKind.ALCOHOL),
    ;

    companion object {
        fun forKind(kind: DrinkKind): List<DrinkIcon> = entries.filter { it.kind == kind }
    }
}

data class Preset(
    val id: Long,
    val kind: DrinkKind,
    val volumeMl: Int,
    val abvPercent: Double?,
    val icon: DrinkIcon,
    /** Non-null for built-in presets (they can be edited and hidden, never deleted). */
    val builtInKey: String?,
    val hidden: Boolean,
    val sortOrder: Int,
) {
    val isBuiltIn: Boolean get() = builtInKey != null
}

data class Entry(
    val id: Long,
    val at: Instant,
    val kind: DrinkKind,
    val volumeMl: Int,
    val abvPercent: Double?,
    val icon: DrinkIcon,
    val createdAt: Instant,
) {
    /** Water counted toward intake. Alcohol never counts. */
    val waterMl: Int get() = if (kind == DrinkKind.WATER) volumeMl else 0

    val alcoholGoalBonusMl: Int
        get() = if (kind == DrinkKind.ALCOHOL) AlcoholMath.goalBonusMl(volumeMl, abvPercent ?: 0.0) else 0
}

/** Template used to seed the preset library. */
data class PresetTemplate(
    val key: String,
    val icon: DrinkIcon,
    val volumeMl: Int,
    val abvPercent: Double? = null,
)

object BuiltInPresets {
    val all: List<PresetTemplate> =
        listOf(
            PresetTemplate("water_150", DrinkIcon.GLASS_SMALL, 150),
            PresetTemplate("water_200", DrinkIcon.GLASS, 200),
            PresetTemplate("water_250", DrinkIcon.MUG, 250),
            PresetTemplate("water_300", DrinkIcon.GLASS_LARGE, 300),
            PresetTemplate("water_330", DrinkIcon.BOTTLE_SMALL, 330),
            PresetTemplate("water_500", DrinkIcon.BOTTLE, 500),
            PresetTemplate("water_750", DrinkIcon.SPORTS_BOTTLE, 750),
            PresetTemplate("water_1000", DrinkIcon.CARAFE, 1000),
            PresetTemplate("water_1500", DrinkIcon.BOTTLE_LARGE, 1500),
            PresetTemplate("sparkling", DrinkIcon.SPARKLING, 100, 11.0),
            PresetTemplate("wine", DrinkIcon.WINE, 200, 12.5),
            PresetTemplate("beer", DrinkIcon.BEER, 500, 5.0),
            PresetTemplate("wheat_beer", DrinkIcon.WHEAT_BEER, 500, 5.4),
            PresetTemplate("long_drink", DrinkIcon.LONG_DRINK, 300, 6.0),
            PresetTemplate("shot", DrinkIcon.SHOT, 20, 40.0),
        )

    fun templateFor(icon: DrinkIcon): PresetTemplate? = all.firstOrNull { it.icon == icon }

    /** Icon a fresh entry of [kind] starts with. */
    fun defaultIcon(kind: DrinkKind): DrinkIcon = if (kind == DrinkKind.ALCOHOL) DrinkIcon.BEER else DrinkIcon.MUG

    /** Default per-surface selections (by template key). */
    val defaultHome = listOf("water_200", "water_250", "water_330", "water_500")
    val defaultShortcuts = listOf("water_200", "water_330", "water_500")
    val defaultNotification = listOf("water_200", "water_500")
    val defaultWidget = listOf("water_200", "water_250", "water_500")
}

/** Input limits and snapping for amounts and alcohol content (10 ml / 0.5 % steps). */
object DrinkLimits {
    const val STEP_ML = 10
    const val MIN_ML = STEP_ML
    private const val MAX_WATER_ML = 2000
    private const val MAX_ALCOHOL_ML = 1000
    const val ABV_STEP = 0.5
    const val MIN_ABV = 0.5
    const val MAX_ABV = 70.0

    fun maxMl(kind: DrinkKind): Int = if (kind == DrinkKind.ALCOHOL) MAX_ALCOHOL_ML else MAX_WATER_ML

    fun snapVolume(
        ml: Float,
        kind: DrinkKind,
    ): Int = ((ml / STEP_ML).roundToInt() * STEP_ML).coerceIn(MIN_ML, maxMl(kind))

    fun snapAbv(percent: Double): Double = ((percent / ABV_STEP).roundToInt() * ABV_STEP).coerceIn(MIN_ABV, MAX_ABV)
}

/** Where a preset selection is shown. Widgets use one key per widget instance. */
object Surfaces {
    const val HOME = "home"
    const val SHORTCUTS = "shortcuts"
    const val NOTIFICATION = "notification"

    fun widget(appWidgetId: Int): String = "widget:$appWidgetId"

    const val MAX_HOME = 8
    const val MAX_SHORTCUTS = 3
    const val MAX_NOTIFICATION = 2
    const val MAX_WIDGET = 4

    fun maxFor(surface: String): Int =
        when (surface) {
            HOME -> MAX_HOME
            SHORTCUTS -> MAX_SHORTCUTS
            NOTIFICATION -> MAX_NOTIFICATION
            else -> MAX_WIDGET
        }
}
