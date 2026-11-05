package de.felixneumann.hydr8m8.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import de.felixneumann.hydr8m8.ui.BrandColors

/** Glance cannot draw custom shapes, so progress visuals are rendered into bitmaps. */
object WidgetBitmaps {
    private const val WATER_TOP = 0xFF5CC3FF.toInt()
    private const val WATER_BOTTOM = 0xFF1572C9.toInt()
    private const val GLASS_STROKE = 0xFF9FD2FF.toInt()

    fun ring(
        fraction: Float,
        sizePx: Int,
        strokePx: Float,
        trackColor: Int = BrandColors.TRACK,
    ): Bitmap {
        val bmp = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bmp)
        val inset = strokePx / 2 + 1
        val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokePx
                strokeCap = Paint.Cap.ROUND
                color = trackColor
            }
        canvas.drawArc(rect, 0f, FULL_CIRCLE, false, paint)
        paint.color = BrandColors.WATER
        canvas.drawArc(rect, START_ANGLE, FULL_CIRCLE * fraction.coerceIn(0f, 1f), false, paint)
        return bmp
    }

    fun bar(
        fraction: Float,
        widthPx: Int,
        heightPx: Int,
        trackColor: Int = BrandColors.TRACK,
    ): Bitmap {
        val bmp = createBitmap(widthPx, heightPx)
        val canvas = Canvas(bmp)
        val r = heightPx / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = trackColor }
        canvas.drawRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), r, r, paint)
        val filled = widthPx * fraction.coerceIn(0f, 1f)
        if (filled > 0) {
            paint.shader = LinearGradient(0f, 0f, widthPx.toFloat(), 0f, WATER_BOTTOM, WATER_TOP, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(RectF(0f, 0f, maxOf(filled, heightPx.toFloat()), heightPx.toFloat()), r, r, paint)
        }
        return bmp
    }

    /** Tapered glass filled with water up to [fraction]. */
    fun glass(
        fraction: Float,
        widthPx: Int,
        heightPx: Int,
    ): Bitmap {
        val bmp = createBitmap(widthPx, heightPx)
        val canvas = Canvas(bmp)
        val stroke = widthPx * STROKE_RATIO
        val taper = widthPx * TAPER_RATIO
        val outline =
            Path().apply {
                moveTo(stroke, stroke)
                lineTo(widthPx - stroke, stroke)
                lineTo(widthPx - taper, heightPx - stroke)
                lineTo(taper, heightPx - stroke)
                close()
            }
        val level = heightPx - stroke - (heightPx - 2 * stroke) * fraction.coerceIn(0f, 1f)
        val bottom = heightPx.toFloat()
        val water =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, level, 0f, bottom, WATER_TOP, WATER_BOTTOM, Shader.TileMode.CLAMP)
            }
        val wave = Path()
        val amp = heightPx * WAVE_RATIO
        wave.moveTo(0f, level)
        val step = widthPx / WAVE_SEGMENTS
        for (i in 0 until WAVE_SEGMENTS.toInt()) {
            val x = i * step
            val dir = if (i % 2 == 0) -1 else 1
            wave.quadTo(x + step / 2, level + dir * amp, x + step, level)
        }
        wave.lineTo(widthPx.toFloat(), bottom)
        wave.lineTo(0f, bottom)
        wave.close()
        canvas.withClip(outline) { drawPath(wave, water) }
        canvas.drawPath(
            outline,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeJoin = Paint.Join.ROUND
                color = GLASS_STROKE
            },
        )
        return bmp
    }

    private const val FULL_CIRCLE = 360f
    private const val START_ANGLE = -90f
    private const val STROKE_RATIO = 0.035f
    private const val TAPER_RATIO = 0.16f
    private const val WAVE_RATIO = 0.03f
    private const val WAVE_SEGMENTS = 4f
}
