package de.felixneumann.hydr8m8.ui.components

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.domain.WakeSchedule
import de.felixneumann.hydr8m8.domain.WakeWindow
import de.felixneumann.hydr8m8.system.Permissions
import de.felixneumann.hydr8m8.system.rememberPermissionRequest

/** Wake/sleep times for weekdays and weekend. */
@Composable
fun WakeScheduleEditor(
    schedule: WakeSchedule,
    onChange: (WakeSchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        WakeWindowRows(stringResource(R.string.weekdays), schedule.weekday) { onChange(schedule.copy(weekday = it)) }
        WakeWindowRows(stringResource(R.string.weekend), schedule.weekend) { onChange(schedule.copy(weekend = it)) }
    }
}

@Composable
private fun WakeWindowRows(
    title: String,
    window: WakeWindow,
    onChange: (WakeWindow) -> Unit,
) {
    Column {
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TimeSettingRow(stringResource(R.string.wake_up), window.wake, { onChange(window.copy(wake = it)) })
        TimeSettingRow(stringResource(R.string.bedtime), window.sleep, { onChange(window.copy(sleep = it)) })
    }
}

/** Current state of the permissions the app asks for. */
data class PermissionStatus(
    val notifications: Boolean,
    val location: Boolean,
    val battery: Boolean,
)

/** Permission state that refreshes whenever the screen resumes (e.g. back from system settings). */
@Composable
fun rememberPermissionStatus(): Pair<PermissionStatus, () -> Unit> {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val status =
        remember(tick) {
            PermissionStatus(
                notifications = Permissions.notificationsGranted(context),
                location = Permissions.locationGranted(context),
                battery = Permissions.batteryUnrestricted(context),
            )
        }
    return status to { tick++ }
}

/**
 * Notification, location and battery permission rows with "Grant" actions, shared by onboarding
 * and settings. Tapping the granted notification row opens the system notification settings.
 */
@Composable
fun PermissionRows(
    onLocationGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val (status, refresh) = rememberPermissionStatus()
    val notificationLauncher = rememberPermissionRequest { refresh() }
    val locationLauncher =
        rememberPermissionRequest { granted ->
            refresh()
            if (granted) onLocationGrant()
        }
    val granted = stringResource(R.string.granted)
    val grant = stringResource(R.string.grant)

    Column(modifier) {
        SettingRow(
            stringResource(R.string.perm_notifications),
            if (status.notifications) granted else grant,
            {
                if (status.notifications) {
                    Permissions.openNotificationSettings(context)
                } else {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            supporting = stringResource(R.string.perm_notifications_hint),
        )
        SettingRow(
            stringResource(R.string.perm_location),
            if (status.location) granted else grant,
            { if (!status.location) locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            supporting = stringResource(R.string.perm_location_hint),
        )
        SettingRow(
            stringResource(R.string.perm_battery),
            if (status.battery) granted else grant,
            { if (!status.battery) Permissions.requestBatteryExemption(context) },
            supporting = stringResource(R.string.perm_battery_hint),
        )
    }
}
