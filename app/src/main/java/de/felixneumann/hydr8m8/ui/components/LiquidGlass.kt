package de.felixneumann.hydr8m8.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.ui.theme.HydrColors
import de.felixneumann.hydr8m8.ui.theme.HydrTheme
import kotlin.math.PI
import kotlin.math.sin

private const val WAVE_PERIOD_MS = 2600
private const val TEXT_ON_WATER_LEVEL = 0.45f
private const val TAPER = 0.12f
private const val WAVE_STEP_PX = 4f

/**
 * Tapered glass whose animated water level shows [fraction]. The surface waves gently and stays
 * level with the real horizon when the phone is tilted ([slope], see `rememberLiquidSlope`).
 */
@Composable
fun LiquidGlass(
    fraction: Float,
    label: String,
    modifier: Modifier = Modifier,
    slope: Float = 0f,
) {
    val colors = HydrTheme.colors
    val level by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 60f),
        label = "level",
    )
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(WAVE_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawGlass(colors, level, phase, slope) }
        Text(
            label,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = if (level > TEXT_ON_WATER_LEVEL) Color.White else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 60.dp),
        )
    }
}

private fun DrawScope.drawGlass(
    colors: HydrColors,
    level: Float,
    phase: Float,
    slope: Float,
) {
    val w = size.width
    val h = size.height
    val stroke = 3.dp.toPx()
    val outline =
        Path().apply {
            moveTo(stroke, stroke)
            lineTo(w - stroke, stroke)
            lineTo(w - w * TAPER, h - stroke)
            lineTo(w * TAPER, h - stroke)
            close()
        }
    val waterTop = h - stroke - (h - 2 * stroke) * level

    // Surface height at x: level + tilt (pivoting around the center) + travelling wave.
    fun surface(
        x: Float,
        amp: Float,
        shift: Float,
    ) = waterTop + slope * (x - w / 2) + amp * sin(x / w * 2 * PI.toFloat() * 1.5f + shift)

    clipPath(outline) {
        if (level <= 0f) return@clipPath
        for (layer in 0..1) {
            val amp = h * if (layer == 0) 0.022f else 0.016f
            val shift = if (layer == 0) phase else -phase * 1.3f
            val wave =
                Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, surface(0f, amp, shift))
                    var x = 0f
                    while (x <= w) {
                        lineTo(x, surface(x, amp, shift))
                        x += WAVE_STEP_PX
                    }
                    lineTo(w, h)
                    close()
                }
            val brush =
                if (layer == 0) {
                    Brush.verticalGradient(listOf(colors.waterLight, colors.waterDeep), startY = waterTop, endY = h)
                } else {
                    SolidColor(colors.waterDeep.copy(alpha = 0.5f))
                }
            drawPath(wave, brush = brush)
        }
        val bubbleY = waterTop + (h - waterTop) * (0.5f + 0.3f * sin(phase))
        if (bubbleY < h - stroke * 3) {
            drawCircle(colors.glassStroke.copy(alpha = 0.6f), radius = 3.dp.toPx(), center = Offset(w * 0.35f, bubbleY))
        }
    }
    drawPath(outline, colors.glassStroke, style = Stroke(width = stroke, join = StrokeJoin.Round))
}
