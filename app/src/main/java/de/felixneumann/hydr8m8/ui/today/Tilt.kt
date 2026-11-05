package de.felixneumann.hydr8m8.ui.today

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.tan

private const val MAX_TILT_RAD = 0.52f // ~30°
private const val MIN_UPRIGHT_GRAVITY = 3f // m/s²; below this the phone lies (almost) flat

/**
 * Slope (dy/dx in screen coordinates) that keeps a liquid surface level with the real horizon while
 * the phone is tilted sideways. Driven by the gravity sensor and smoothed with a soft spring so the
 * water sloshes a little. Returns 0 when there is no sensor or the phone lies flat.
 */
@Composable
fun rememberLiquidSlope(): State<Float> {
    val context = LocalContext.current
    var target by remember { mutableFloatStateOf(0f) }
    DisposableEffect(context) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val sensor =
            sensors?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val (gx, gy) = event.values
                    target =
                        if (hypot(gx, gy) < MIN_UPRIGHT_GRAVITY) {
                            0f
                        } else {
                            tan(atan2(gx, gy).coerceIn(-MAX_TILT_RAD, MAX_TILT_RAD))
                        }
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int,
                ) = Unit
            }
        if (sensor != null) sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensors?.unregisterListener(listener) }
    }
    return animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "liquidSlope",
    )
}
