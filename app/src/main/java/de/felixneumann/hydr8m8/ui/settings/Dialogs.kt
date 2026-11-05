package de.felixneumann.hydr8m8.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import de.felixneumann.hydr8m8.R
import kotlin.math.roundToInt

/** Dialog with a stepped slider for a numeric setting. */
@Composable
fun SliderDialog(
    title: String,
    initial: Int,
    range: IntRange,
    step: Int,
    format: (Int) -> String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableFloatStateOf(initial.toFloat()) }
    val snapped = ((value / step).roundToInt() * step).coerceIn(range.first, range.last)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(format(snapped), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(snapped) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

object Permissions {
    fun has(
        context: Context,
        permission: String,
    ): Boolean = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    fun notificationsGranted(context: Context) = has(context, Manifest.permission.POST_NOTIFICATIONS)

    fun locationGranted(context: Context) = has(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun batteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /** Opens the system dialog to exempt the app from battery optimisation (needed on Samsung). */
    @android.annotation.SuppressLint("BatteryLife") // Sideloaded app; reminders must not be deferred.
    fun requestBatteryExemption(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openNotificationSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** Launcher for a single runtime permission; [onResult] receives whether it was granted. */
@Composable
fun rememberPermissionRequest(onResult: (Boolean) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
