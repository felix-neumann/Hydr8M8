package de.felixneumann.hydr8m8.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import de.felixneumann.hydr8m8.domain.CalendarLevel
import de.felixneumann.hydr8m8.domain.DrinkKind

/** Brand colors beyond the Material scheme (direction "B": dark, liquid). */
@Immutable
data class HydrColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val water: Color,
    val waterLight: Color,
    val waterDeep: Color,
    val alcohol: Color,
    val warm: Color,
    val track: Color,
    val muted: Color,
    val glassStroke: Color,
    val levelNone: Color,
    val levelLow: Color,
    val levelNear: Color,
    val levelReached: Color,
    val onLevelLight: Color,
    val onLevelDark: Color,
) {
    val backgroundBrush: Brush get() = Brush.verticalGradient(listOf(backgroundTop, backgroundBottom))

    fun forKind(kind: DrinkKind): Color = if (kind == DrinkKind.ALCOHOL) alcohol else water

    fun level(level: CalendarLevel): Color =
        when (level) {
            CalendarLevel.NONE -> levelNone
            CalendarLevel.LOW -> levelLow
            CalendarLevel.NEAR -> levelNear
            CalendarLevel.REACHED -> levelReached
        }

    fun onLevel(level: CalendarLevel): Color =
        when (level) {
            CalendarLevel.NONE -> muted
            CalendarLevel.LOW -> onLevelLight
            CalendarLevel.NEAR, CalendarLevel.REACHED -> onLevelDark
        }
}

private val DarkHydr =
    HydrColors(
        backgroundTop = Color(0xFF071A2C),
        backgroundBottom = Color(0xFF0B2C48),
        water = Color(0xFF3FA7FF),
        waterLight = Color(0xFF5CC3FF),
        waterDeep = Color(0xFF1572C9),
        alcohol = Color(0xFFF2B6FF),
        warm = Color(0xFFFFD27A),
        track = Color(0xFF1D4466),
        muted = Color(0xFF8FB5D6),
        glassStroke = Color(0xFF9FD2FF),
        levelNone = Color(0xFF12304B),
        levelLow = Color(0xFF1B5583),
        levelNear = Color(0xFF3FA7FF),
        levelReached = Color(0xFF9FE0FF),
        onLevelLight = Color(0xFFE6F3FF),
        onLevelDark = Color(0xFF04223D),
    )

private val LightHydr =
    HydrColors(
        backgroundTop = Color(0xFFF4F9FE),
        backgroundBottom = Color(0xFFDDEBF8),
        water = Color(0xFF1F7AD1),
        waterLight = Color(0xFF5CC3FF),
        waterDeep = Color(0xFF1572C9),
        alcohol = Color(0xFFB04FC4),
        warm = Color(0xFFC77800),
        track = Color(0xFFD3E2F0),
        muted = Color(0xFF53606C),
        glassStroke = Color(0xFF4A90D0),
        levelNone = Color(0xFFE6EDF4),
        levelLow = Color(0xFFA9CBEB),
        levelNear = Color(0xFF3FA7FF),
        levelReached = Color(0xFF1F6DAB),
        onLevelLight = Color(0xFF0F1D2A),
        onLevelDark = Color(0xFFFFFFFF),
    )

private val DarkScheme =
    darkColorScheme(
        primary = Color(0xFF3FA7FF),
        onPrimary = Color(0xFF04223D),
        primaryContainer = Color(0xFF173F63),
        onPrimaryContainer = Color(0xFFCFE4F7),
        secondary = Color(0xFF9FD2FF),
        secondaryContainer = Color(0xFF1D4466),
        onSecondaryContainer = Color(0xFFCFE4F7),
        tertiary = Color(0xFFF2B6FF),
        background = Color(0xFF071A2C),
        onBackground = Color(0xFFE6F3FF),
        surface = Color(0xFF0B2C48),
        onSurface = Color(0xFFE6F3FF),
        surfaceVariant = Color(0xFF10385A),
        onSurfaceVariant = Color(0xFF8FB5D6),
        surfaceContainer = Color(0xFF0F2F4C),
        surfaceContainerHigh = Color(0xFF133756),
        surfaceContainerHighest = Color(0xFF173F63),
        surfaceContainerLow = Color(0xFF0C2944),
        outline = Color(0xFF2F5B82),
        outlineVariant = Color(0xFF1D4466),
    )

private val LightScheme =
    lightColorScheme(
        primary = Color(0xFF1F6DAB),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD3E7FB),
        onPrimaryContainer = Color(0xFF0F2F4C),
        secondary = Color(0xFF2B6CB0),
        secondaryContainer = Color(0xFFD3E7FB),
        onSecondaryContainer = Color(0xFF0F2F4C),
        tertiary = Color(0xFFB04FC4),
        background = Color(0xFFF4F9FE),
        onBackground = Color(0xFF0F1D2A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F1D2A),
        surfaceVariant = Color(0xFFE3EDF6),
        onSurfaceVariant = Color(0xFF53606C),
        surfaceContainer = Color(0xFFFFFFFF),
        surfaceContainerHigh = Color(0xFFF0F5FA),
        surfaceContainerHighest = Color(0xFFE3EDF6),
        surfaceContainerLow = Color(0xFFF7FAFD),
        outline = Color(0xFFB4CFE8),
        outlineVariant = Color(0xFFD3E2F0),
    )

val LocalHydrColors = staticCompositionLocalOf { DarkHydr }

@Composable
fun Hydr8M8Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalHydrColors provides if (darkTheme) DarkHydr else LightHydr) {
        MaterialTheme(colorScheme = if (darkTheme) DarkScheme else LightScheme, content = content)
    }
}

object HydrTheme {
    val colors: HydrColors
        @Composable get() = LocalHydrColors.current
}
