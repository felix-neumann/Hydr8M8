package de.felixneumann.hydr8m8.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.DrinkIcon
import de.felixneumann.hydr8m8.domain.DrinkKind
import de.felixneumann.hydr8m8.domain.MateMood
import de.felixneumann.hydr8m8.domain.Preset
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Locale-aware formatting shared by app UI, widgets, notifications and shortcuts. */
object Format {
    private const val ML_PER_LITRE = 1000.0

    /** "250 ml" below one litre, "1.5 L" from one litre on. */
    fun volume(ml: Int): String =
        if (ml < ML_PER_LITRE) {
            "$ml ml"
        } else {
            number(ml / ML_PER_LITRE, maxDecimals = 2) + " L"
        }

    /** Always in litres with fixed decimals, e.g. "1.80 L". */
    fun litres(
        ml: Long,
        decimals: Int = 2,
    ): String = number(ml / ML_PER_LITRE, minDecimals = decimals, maxDecimals = decimals) + " L"

    fun litres(ml: Int): String = litres(ml.toLong())

    /** File size in megabytes with one decimal, e.g. "4.5 MB". */
    fun megabytes(bytes: Long): String = number(bytes / BYTES_PER_MB, minDecimals = 1, maxDecimals = 1) + " MB"

    private const val BYTES_PER_MB = 1_048_576.0

    fun abv(percent: Double): String = number(percent, maxDecimals = 1) + " %"

    fun number(
        value: Double,
        minDecimals: Int = 0,
        maxDecimals: Int = 2,
    ): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = minDecimals
            maximumFractionDigits = maxDecimals
        }.format(value)

    fun time(instant: Instant): String = time(instant.atZone(ZoneId.systemDefault()).toLocalTime())

    /** Short localized time, following the locale's 12/24 h convention. */
    fun time(time: LocalTime): String = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time)

    fun date(
        date: LocalDate,
        style: FormatStyle = FormatStyle.MEDIUM,
    ): String = DateTimeFormatter.ofLocalizedDate(style).format(date)

    /** Name if the user gave one, otherwise the amount label. For tight spaces (buttons, actions). */
    fun presetTitle(preset: Preset): String = preset.name ?: presetLabel(preset)

    /** Name and amount ("My bottle · 750 ml"), or just the amount for unnamed presets. */
    fun presetDisplay(preset: Preset): String =
        preset.name?.let { "$it · ${presetLabel(preset)}" } ?: presetLabel(preset)

    /** Short label of a preset: amount, plus ABV for alcohol. */
    fun presetLabel(preset: Preset): String =
        if (preset.kind == DrinkKind.ALCOHOL && preset.abvPercent != null) {
            "${volume(preset.volumeMl)} · ${abv(preset.abvPercent)}"
        } else {
            volume(preset.volumeMl)
        }
}

@StringRes
fun DrinkKind.label(): Int = if (this == DrinkKind.ALCOHOL) R.string.kind_alcohol else R.string.kind_water

@DrawableRes
fun DrinkIcon.drawable(): Int =
    when (this) {
        DrinkIcon.GLASS_SMALL -> R.drawable.ic_drink_glass_small
        DrinkIcon.GLASS -> R.drawable.ic_drink_glass
        DrinkIcon.MUG -> R.drawable.ic_drink_mug
        DrinkIcon.GLASS_LARGE -> R.drawable.ic_drink_glass_large
        DrinkIcon.BOTTLE_SMALL -> R.drawable.ic_drink_bottle_small
        DrinkIcon.BOTTLE -> R.drawable.ic_drink_bottle
        DrinkIcon.SPORTS_BOTTLE -> R.drawable.ic_drink_sports_bottle
        DrinkIcon.CARAFE -> R.drawable.ic_drink_carafe
        DrinkIcon.BOTTLE_LARGE -> R.drawable.ic_drink_bottle_large
        DrinkIcon.SPARKLING -> R.drawable.ic_drink_sparkling
        DrinkIcon.WINE -> R.drawable.ic_drink_wine
        DrinkIcon.BEER -> R.drawable.ic_drink_beer
        DrinkIcon.WHEAT_BEER -> R.drawable.ic_drink_wheat_beer
        DrinkIcon.LONG_DRINK -> R.drawable.ic_drink_long_drink
        DrinkIcon.SHOT -> R.drawable.ic_drink_shot
    }

@StringRes
fun DrinkIcon.label(): Int =
    when (this) {
        DrinkIcon.GLASS_SMALL -> R.string.icon_glass_small
        DrinkIcon.GLASS -> R.string.icon_glass
        DrinkIcon.MUG -> R.string.icon_mug
        DrinkIcon.GLASS_LARGE -> R.string.icon_glass_large
        DrinkIcon.BOTTLE_SMALL -> R.string.icon_bottle_small
        DrinkIcon.BOTTLE -> R.string.icon_bottle
        DrinkIcon.SPORTS_BOTTLE -> R.string.icon_sports_bottle
        DrinkIcon.CARAFE -> R.string.icon_carafe
        DrinkIcon.BOTTLE_LARGE -> R.string.icon_bottle_large
        DrinkIcon.SPARKLING -> R.string.icon_sparkling
        DrinkIcon.WINE -> R.string.icon_wine
        DrinkIcon.BEER -> R.string.icon_beer
        DrinkIcon.WHEAT_BEER -> R.string.icon_wheat_beer
        DrinkIcon.LONG_DRINK -> R.string.icon_long_drink
        DrinkIcon.SHOT -> R.string.icon_shot
    }

@DrawableRes
fun MateMood.drawable(): Int =
    when (this) {
        MateMood.CHAMPION -> R.drawable.mate_champion
        MateMood.HAPPY -> R.drawable.mate_happy
        MateMood.THIRSTY -> R.drawable.mate_thirsty
        MateMood.VERY_THIRSTY -> R.drawable.mate_very_thirsty
        MateMood.TIPSY -> R.drawable.mate_tipsy
        MateMood.SLEEPY -> R.drawable.mate_sleepy
    }

@StringRes
fun MateMood.label(): Int =
    when (this) {
        MateMood.CHAMPION -> R.string.mood_champion
        MateMood.HAPPY -> R.string.mood_happy
        MateMood.THIRSTY -> R.string.mood_thirsty
        MateMood.VERY_THIRSTY -> R.string.mood_very_thirsty
        MateMood.TIPSY -> R.string.mood_tipsy
        MateMood.SLEEPY -> R.string.mood_sleepy
    }

/** ARGB colors shared with non-Compose surfaces (widgets, shortcuts, notifications). */
object BrandColors {
    const val WATER = 0xFF3FA7FF.toInt()
    const val ALCOHOL = 0xFFF2B6FF.toInt()
    const val NIGHT = 0xFF071A2C.toInt()
    const val NIGHT_2 = 0xFF0B2C48.toInt()
    const val CARD = 0xFF0F2F4C.toInt()
    const val TEXT = 0xFFE6F3FF.toInt()
    const val MUTED = 0xFF8FB5D6.toInt()
    const val TRACK = 0xFF1D4466.toInt()

    fun forKind(kind: DrinkKind): Int = if (kind == DrinkKind.ALCOHOL) ALCOHOL else WATER
}
