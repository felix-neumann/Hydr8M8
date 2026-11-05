package de.felixneumann.hydr8m8.ui.settings

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.felixneumann.hydr8m8.BuildConfig
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.data.AppSettings
import de.felixneumann.hydr8m8.data.ThemeMode
import de.felixneumann.hydr8m8.domain.Surfaces
import de.felixneumann.hydr8m8.domain.WakeWindow
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.components.ChoiceDialog
import de.felixneumann.hydr8m8.ui.components.SectionCard
import de.felixneumann.hydr8m8.ui.components.SettingRow
import de.felixneumann.hydr8m8.ui.components.SwitchRow
import de.felixneumann.hydr8m8.ui.components.TimePickerDialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private fun LocalTime.short(): String = format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private enum class TimeField { DAY_START, WEEKDAY_WAKE, WEEKDAY_SLEEP, WEEKEND_WAKE, WEEKEND_SLEEP }

private enum class NumberField { GOAL, INTERVAL, THRESHOLD, QUIET, SNOOZE, WARM_C, WARM_ML, HOT_C, HOT_ML }

@Composable
fun SettingsScreen(
    settings: AppSettings,
    currentGoalMl: Int,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onSetGoal: (Int) -> Unit,
    onOpenPresets: () -> Unit,
    onOpenSurface: (String) -> Unit,
    onLocationGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var timeField by remember { mutableStateOf<TimeField?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var numberField by remember { mutableStateOf<NumberField?>(null) }
    // Re-read permission state whenever we come back from system settings.
    var permissionTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionTick++ }
    val notificationsOk = remember(permissionTick) { Permissions.notificationsGranted(context) }
    val locationOk = remember(permissionTick) { Permissions.locationGranted(context) }
    val batteryOk = remember(permissionTick) { Permissions.batteryUnrestricted(context) }
    val notifLauncher = rememberPermissionRequest { permissionTick++ }
    val locationLauncher =
        rememberPermissionRequest { granted ->
            permissionTick++
            if (granted) onLocationGrant()
        }
    val r = settings.reminders
    val w = settings.weather

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.nav_settings),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp),
        )

        SectionCard(title = stringResource(R.string.section_goal)) {
            SettingRow(stringResource(R.string.daily_goal), Format.litres(currentGoalMl), {
                numberField =
                    NumberField.GOAL
            }, supporting = stringResource(R.string.daily_goal_hint))
            SettingRow(stringResource(R.string.day_start), settings.dayStart.short(), {
                timeField = TimeField.DAY_START
            }, supporting = stringResource(R.string.day_start_hint))
        }

        SectionCard(title = stringResource(R.string.section_amounts)) {
            SettingRow(stringResource(R.string.preset_library), "›", onOpenPresets)
            HorizontalDivider()
            SettingRow(stringResource(R.string.surface_home), "›", { onOpenSurface(Surfaces.HOME) })
            SettingRow(stringResource(R.string.surface_shortcuts), "›", { onOpenSurface(Surfaces.SHORTCUTS) })
            SettingRow(stringResource(R.string.surface_notification), "›", { onOpenSurface(Surfaces.NOTIFICATION) })
        }

        SectionCard(title = stringResource(R.string.section_sleep)) {
            Text(
                stringResource(R.string.weekdays),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingRow(stringResource(R.string.wake_up), settings.schedule.weekday.wake.short(), {
                timeField =
                    TimeField.WEEKDAY_WAKE
            })
            SettingRow(stringResource(R.string.bedtime), settings.schedule.weekday.sleep.short(), {
                timeField =
                    TimeField.WEEKDAY_SLEEP
            })
            Text(stringResource(R.string.weekend), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingRow(stringResource(R.string.wake_up), settings.schedule.weekend.wake.short(), {
                timeField =
                    TimeField.WEEKEND_WAKE
            })
            SettingRow(stringResource(R.string.bedtime), settings.schedule.weekend.sleep.short(), {
                timeField =
                    TimeField.WEEKEND_SLEEP
            })
        }

        SectionCard(title = stringResource(R.string.section_reminders)) {
            SwitchRow(stringResource(R.string.reminders_enabled), r.enabled, { v ->
                onUpdate { it.copy(reminders = it.reminders.copy(enabled = v)) }
            })
            SettingRow(stringResource(R.string.reminder_interval), minutesText(r.intervalMinutes), {
                numberField =
                    NumberField.INTERVAL
            }, supporting = stringResource(R.string.reminder_interval_hint))
            SwitchRow(
                stringResource(R.string.halve_when_behind),
                r.halveWhenBehind,
                { v -> onUpdate { it.copy(reminders = it.reminders.copy(halveWhenBehind = v)) } },
            )
            SettingRow(stringResource(R.string.behind_threshold), "${r.behindThresholdPercent} %", {
                numberField =
                    NumberField.THRESHOLD
            })
            SettingRow(stringResource(R.string.quiet_after_wake), minutesText(r.quietAfterWakeMinutes), {
                numberField =
                    NumberField.QUIET
            })
            SettingRow(stringResource(R.string.snooze_duration), minutesText(r.snoozeMinutes), {
                numberField =
                    NumberField.SNOOZE
            })
            SwitchRow(
                stringResource(R.string.respect_dnd),
                r.respectDoNotDisturb,
                { v -> onUpdate { it.copy(reminders = it.reminders.copy(respectDoNotDisturb = v)) } },
                supporting = stringResource(R.string.respect_dnd_hint),
            )
        }

        SectionCard(title = stringResource(R.string.section_weather)) {
            SwitchRow(
                stringResource(R.string.weather_enabled),
                w.enabled,
                { v ->
                    onUpdate { it.copy(weather = it.weather.copy(enabled = v)) }
                    if (v && !locationOk) locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                supporting = stringResource(R.string.weather_hint),
            )
            SettingRow(stringResource(R.string.warm_from), "${w.warmThresholdC.toInt()} °C", {
                numberField =
                    NumberField.WARM_C
            })
            SettingRow(stringResource(R.string.warm_bonus), "+" + Format.volume(w.warmBonusMl), {
                numberField =
                    NumberField.WARM_ML
            })
            SettingRow(stringResource(R.string.hot_from), "${w.hotThresholdC.toInt()} °C", {
                numberField =
                    NumberField.HOT_C
            })
            SettingRow(stringResource(R.string.hot_bonus), "+" + Format.volume(w.hotBonusMl), {
                numberField =
                    NumberField.HOT_ML
            })
        }

        SectionCard(title = stringResource(R.string.section_extras)) {
            SwitchRow(
                stringResource(R.string.live_update),
                settings.liveUpdateEnabled,
                { v -> onUpdate { it.copy(liveUpdateEnabled = v) } },
                supporting = stringResource(R.string.live_update_hint),
            )
            SwitchRow(
                stringResource(R.string.sounds),
                settings.soundsEnabled,
                { v -> onUpdate { it.copy(soundsEnabled = v) } },
                supporting = stringResource(R.string.sounds_hint),
            )
        }

        SectionCard(title = stringResource(R.string.section_appearance)) {
            SettingRow(stringResource(R.string.design), stringResource(themeLabel(settings.themeMode)), {
                showThemeDialog =
                    true
            })
            SettingRow(
                stringResource(R.string.language),
                languageLabel(AppLanguage.current(context)),
                { showLanguageDialog = true },
            )
        }

        SectionCard(title = stringResource(R.string.section_permissions)) {
            SettingRow(
                stringResource(R.string.perm_notifications),
                stringResource(if (notificationsOk) R.string.granted else R.string.grant),
                {
                    if (notificationsOk) {
                        Permissions.openNotificationSettings(context)
                    } else {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
            SettingRow(
                stringResource(R.string.perm_location),
                stringResource(if (locationOk) R.string.granted else R.string.grant),
                { if (!locationOk) locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                supporting = stringResource(R.string.perm_location_hint),
            )
            SettingRow(
                stringResource(R.string.perm_battery),
                stringResource(if (batteryOk) R.string.granted else R.string.grant),
                { if (!batteryOk) Permissions.requestBatteryExemption(context) },
                supporting = stringResource(R.string.perm_battery_hint),
            )
        }

        Text(
            stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.height(24.dp))
    }

    timeField?.let { field ->
        val initial =
            when (field) {
                TimeField.DAY_START -> settings.dayStart
                TimeField.WEEKDAY_WAKE -> settings.schedule.weekday.wake
                TimeField.WEEKDAY_SLEEP -> settings.schedule.weekday.sleep
                TimeField.WEEKEND_WAKE -> settings.schedule.weekend.wake
                TimeField.WEEKEND_SLEEP -> settings.schedule.weekend.sleep
            }
        TimePickerDialog(
            initial = initial,
            onConfirm = { t ->
                onUpdate { s -> applyTime(s, field, t) }
                timeField = null
            },
            onDismiss = { timeField = null },
        )
    }

    if (showThemeDialog) {
        ChoiceDialog(
            title = stringResource(R.string.design),
            options = ThemeMode.entries.map { it to stringResource(themeLabel(it)) },
            selected = settings.themeMode,
            onSelect = { mode ->
                onUpdate { it.copy(themeMode = mode) }
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title = stringResource(R.string.language),
            options = AppLanguage.supported.map { it to languageLabel(it) },
            selected = AppLanguage.current(context),
            onSelect = { tag ->
                showLanguageDialog = false
                AppLanguage.set(context, tag)
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    numberField?.let { field ->
        NumberDialog(field, settings, currentGoalMl, onUpdate, onSetGoal) { numberField = null }
    }
}

private fun themeLabel(mode: ThemeMode): Int =
    when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

/** Language names are shown in their own language; "system" is localized. */
@Composable
private fun languageLabel(tag: String): String =
    when (tag) {
        "de" -> "Deutsch"
        "en" -> "English"
        else -> stringResource(R.string.language_system)
    }

private fun applyTime(
    s: AppSettings,
    field: TimeField,
    t: LocalTime,
): AppSettings {
    fun WakeWindow.with(
        wake: LocalTime = this.wake,
        sleep: LocalTime = this.sleep,
    ) = WakeWindow(wake, sleep)
    val sch = s.schedule
    return when (field) {
        TimeField.DAY_START -> s.copy(dayStart = t)
        TimeField.WEEKDAY_WAKE -> s.copy(schedule = sch.copy(weekday = sch.weekday.with(wake = t)))
        TimeField.WEEKDAY_SLEEP -> s.copy(schedule = sch.copy(weekday = sch.weekday.with(sleep = t)))
        TimeField.WEEKEND_WAKE -> s.copy(schedule = sch.copy(weekend = sch.weekend.with(wake = t)))
        TimeField.WEEKEND_SLEEP -> s.copy(schedule = sch.copy(weekend = sch.weekend.with(sleep = t)))
    }
}

@Composable
private fun minutesText(minutes: Int): String = stringResource(R.string.minutes_value, minutes)

@Composable
private fun NumberDialog(
    field: NumberField,
    s: AppSettings,
    goalMl: Int,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onSetGoal: (Int) -> Unit,
    onDone: () -> Unit,
) {
    val minutes = stringResource(R.string.minutes_short)

    data class Spec(
        val title: Int,
        val initial: Int,
        val range: IntRange,
        val step: Int,
        val format: (Int) -> String,
        val apply: (Int) -> Unit,
    )
    val spec =
        when (field) {
            NumberField.GOAL -> {
                Spec(R.string.daily_goal, goalMl, 500..5000, 50, { Format.litres(it) }, onSetGoal)
            }

            NumberField.INTERVAL -> {
                Spec(R.string.reminder_interval, s.reminders.intervalMinutes, 30..240, 15, { "$it $minutes" }) { v ->
                    onUpdate { it.copy(reminders = it.reminders.copy(intervalMinutes = v)) }
                }
            }

            NumberField.THRESHOLD -> {
                Spec(R.string.behind_threshold, s.reminders.behindThresholdPercent, 5..50, 5, { "$it %" }) { v ->
                    onUpdate { it.copy(reminders = it.reminders.copy(behindThresholdPercent = v)) }
                }
            }

            NumberField.QUIET -> {
                Spec(R.string.quiet_after_wake, s.reminders.quietAfterWakeMinutes, 0..120, 15, {
                    "$it $minutes"
                }) { v ->
                    onUpdate { it.copy(reminders = it.reminders.copy(quietAfterWakeMinutes = v)) }
                }
            }

            NumberField.SNOOZE -> {
                Spec(R.string.snooze_duration, s.reminders.snoozeMinutes, 10..120, 5, { "$it $minutes" }) { v ->
                    onUpdate { it.copy(reminders = it.reminders.copy(snoozeMinutes = v)) }
                }
            }

            NumberField.WARM_C -> {
                Spec(R.string.warm_from, s.weather.warmThresholdC.toInt(), 18..35, 1, { "$it °C" }) { v ->
                    onUpdate { it.copy(weather = it.weather.copy(warmThresholdC = v.toDouble())) }
                }
            }

            NumberField.WARM_ML -> {
                Spec(R.string.warm_bonus, s.weather.warmBonusMl, 50..1000, 50, { "+" + Format.volume(it) }) { v ->
                    onUpdate { it.copy(weather = it.weather.copy(warmBonusMl = v)) }
                }
            }

            NumberField.HOT_C -> {
                Spec(R.string.hot_from, s.weather.hotThresholdC.toInt(), 20..45, 1, { "$it °C" }) { v ->
                    onUpdate { it.copy(weather = it.weather.copy(hotThresholdC = v.toDouble())) }
                }
            }

            NumberField.HOT_ML -> {
                Spec(R.string.hot_bonus, s.weather.hotBonusMl, 50..1500, 50, { "+" + Format.volume(it) }) { v ->
                    onUpdate { it.copy(weather = it.weather.copy(hotBonusMl = v)) }
                }
            }
        }
    SliderDialog(
        title = stringResource(spec.title),
        initial = spec.initial,
        range = spec.range,
        step = spec.step,
        format = spec.format,
        onConfirm = {
            spec.apply(it)
            onDone()
        },
        onDismiss = onDone,
    )
}
