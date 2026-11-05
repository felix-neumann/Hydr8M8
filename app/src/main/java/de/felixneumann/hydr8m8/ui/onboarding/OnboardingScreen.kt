package de.felixneumann.hydr8m8.ui.onboarding

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.data.AppSettings
import de.felixneumann.hydr8m8.domain.MateMood
import de.felixneumann.hydr8m8.domain.Preset
import de.felixneumann.hydr8m8.domain.Surfaces
import de.felixneumann.hydr8m8.domain.WakeSchedule
import de.felixneumann.hydr8m8.domain.WakeWindow
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.components.MateView
import de.felixneumann.hydr8m8.ui.components.SectionCard
import de.felixneumann.hydr8m8.ui.components.SettingRow
import de.felixneumann.hydr8m8.ui.components.SurfaceEditor
import de.felixneumann.hydr8m8.ui.components.TimePickerDialog
import de.felixneumann.hydr8m8.ui.settings.Permissions
import de.felixneumann.hydr8m8.ui.settings.rememberPermissionRequest
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private const val STEPS = 4
private const val GOAL_STEP = 50
private const val MIN_GOAL = 500
private const val MAX_GOAL = 5000

/** First-launch flow: goal, wake/sleep windows, home-screen amounts, permissions. */
@Composable
fun OnboardingScreen(
    initialHomePresets: List<Preset>,
    library: List<Preset>,
    onFinish: (goalMl: Int, schedule: WakeSchedule, homeIds: List<Long>) -> Unit,
    onLocationGrant: () -> Unit,
    onCreatePreset: (Preset, (Long) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var goal by rememberSaveable { mutableIntStateOf(AppSettings.DEFAULT_GOAL_ML) }
    var schedule by remember { mutableStateOf(WakeSchedule.DEFAULT) }
    var homeIds by remember(initialHomePresets) { mutableStateOf(initialHomePresets.map { it.id }) }

    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(progress = { (step + 1f) / STEPS }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        MateView(if (step == STEPS - 1) MateMood.CHAMPION else MateMood.HAPPY, size = 96.dp)
        Spacer(Modifier.height(12.dp))
        when (step) {
            0 -> {
                GoalStep(goal) { goal = it }
            }

            1 -> {
                ScheduleStep(schedule) { schedule = it }
            }

            2 -> {
                HomeStep(homeIds.mapNotNull { id -> library.find { it.id == id } }, library, onCreatePreset) {
                    homeIds =
                        it
                }
            }

            else -> {
                PermissionStep(onLocationGrant)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            if (step > 0) {
                OutlinedButton(onClick = {
                    step--
                }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.back)) }
            }
            Button(
                onClick = { if (step < STEPS - 1) step++ else onFinish(goal, schedule, homeIds) },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(if (step < STEPS - 1) R.string.next else R.string.lets_go)) }
        }
    }
}

@Composable
private fun Title(
    title: String,
    text: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun GoalStep(
    goal: Int,
    onGoal: (Int) -> Unit,
) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Title(stringResource(R.string.onboarding_welcome), stringResource(R.string.onboarding_goal_text))
    Text(Format.litres(goal), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Slider(
        value = goal.toFloat(),
        onValueChange = { onGoal(((it / GOAL_STEP).roundToInt() * GOAL_STEP).coerceIn(MIN_GOAL, MAX_GOAL)) },
        valueRange = MIN_GOAL.toFloat()..MAX_GOAL.toFloat(),
    )
    Text(
        stringResource(R.string.onboarding_goal_hint),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ScheduleStep(
    schedule: WakeSchedule,
    onChange: (WakeSchedule) -> Unit,
) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Title(stringResource(R.string.onboarding_sleep_title), stringResource(R.string.onboarding_sleep_text))
    var editing by remember { mutableStateOf<Pair<Boolean, Boolean>?>(null) } // (weekend, sleep)

    fun LocalTime.short() = format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    SectionCard {
        listOf(false, true).forEach { weekend ->
            val w = if (weekend) schedule.weekend else schedule.weekday
            Text(
                stringResource(if (weekend) R.string.weekend else R.string.weekdays),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingRow(stringResource(R.string.wake_up), w.wake.short(), { editing = weekend to false })
            SettingRow(stringResource(R.string.bedtime), w.sleep.short(), { editing = weekend to true })
        }
    }
    editing?.let { (weekend, sleep) ->
        val w = if (weekend) schedule.weekend else schedule.weekday
        TimePickerDialog(
            initial = if (sleep) w.sleep else w.wake,
            onConfirm = { t ->
                val updated = if (sleep) WakeWindow(w.wake, t) else WakeWindow(t, w.sleep)
                onChange(if (weekend) schedule.copy(weekend = updated) else schedule.copy(weekday = updated))
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun HomeStep(
    selected: List<Preset>,
    library: List<Preset>,
    onCreatePreset: (Preset, (Long) -> Unit) -> Unit,
    onChange: (List<Long>) -> Unit,
) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Title(stringResource(R.string.onboarding_home_title), stringResource(R.string.onboarding_home_text))
    SectionCard { SurfaceEditor(selected, library, Surfaces.MAX_HOME, onChange, onCreatePreset = onCreatePreset) }
}

@Composable
private fun PermissionStep(onLocationGrant: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val notif = remember(tick) { Permissions.notificationsGranted(context) }
    val location = remember(tick) { Permissions.locationGranted(context) }
    val battery = remember(tick) { Permissions.batteryUnrestricted(context) }
    val notifLauncher = rememberPermissionRequest { tick++ }
    val locationLauncher =
        rememberPermissionRequest {
            tick++
            if (it) onLocationGrant()
        }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Title(stringResource(R.string.onboarding_perm_title), stringResource(R.string.onboarding_perm_text))
        SectionCard {
            SettingRow(
                stringResource(R.string.perm_notifications),
                stringResource(if (notif) R.string.granted else R.string.grant),
                { if (!notif) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                supporting = stringResource(R.string.perm_notifications_hint),
            )
            SettingRow(
                stringResource(R.string.perm_location),
                stringResource(if (location) R.string.granted else R.string.grant),
                { if (!location) locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                supporting = stringResource(R.string.perm_location_hint),
            )
            SettingRow(
                stringResource(R.string.perm_battery),
                stringResource(if (battery) R.string.granted else R.string.grant),
                { if (!battery) Permissions.requestBatteryExemption(context) },
                supporting = stringResource(R.string.perm_battery_hint),
            )
        }
    }
}
