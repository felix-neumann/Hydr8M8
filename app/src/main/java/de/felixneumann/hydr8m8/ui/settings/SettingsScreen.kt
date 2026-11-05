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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.felixneumann.hydr8m8.BuildConfig
import de.felixneumann.hydr8m8.R
import de.felixneumann.hydr8m8.data.AppSettings
import de.felixneumann.hydr8m8.data.ThemeMode
import de.felixneumann.hydr8m8.domain.ReminderSettings
import de.felixneumann.hydr8m8.domain.Surfaces
import de.felixneumann.hydr8m8.domain.WeatherRules
import de.felixneumann.hydr8m8.system.AppLanguage
import de.felixneumann.hydr8m8.system.Permissions
import de.felixneumann.hydr8m8.system.rememberPermissionRequest
import de.felixneumann.hydr8m8.ui.Format
import de.felixneumann.hydr8m8.ui.components.ChoiceSettingRow
import de.felixneumann.hydr8m8.ui.components.NumberSettingRow
import de.felixneumann.hydr8m8.ui.components.PermissionRows
import de.felixneumann.hydr8m8.ui.components.SectionCard
import de.felixneumann.hydr8m8.ui.components.SettingRow
import de.felixneumann.hydr8m8.ui.components.SwitchRow
import de.felixneumann.hydr8m8.ui.components.TimeSettingRow
import de.felixneumann.hydr8m8.ui.components.WakeScheduleEditor
import de.felixneumann.hydr8m8.update.UpdateState

/** Changes the settings via a pure transform; persisted by the caller. */
typealias SettingsUpdate = ((AppSettings) -> AppSettings) -> Unit

@Composable
fun SettingsScreen(
    settings: AppSettings,
    currentGoalMl: Int,
    updateState: UpdateState,
    onUpdate: SettingsUpdate,
    onSetGoal: (Int) -> Unit,
    onOpenPresets: () -> Unit,
    onOpenSurface: (String) -> Unit,
    onLocationGrant: () -> Unit,
    onCheckForUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            modifier = Modifier.padding(top = 8.dp),
        )
        GoalSection(settings, currentGoalMl, onUpdate, onSetGoal)
        AmountsSection(onOpenPresets, onOpenSurface)
        SectionCard(title = stringResource(R.string.section_sleep)) {
            WakeScheduleEditor(settings.schedule, { schedule -> onUpdate { it.copy(schedule = schedule) } })
        }
        ReminderSection(settings.reminders) { change -> onUpdate { it.copy(reminders = change(it.reminders)) } }
        WeatherSection(settings.weather, onLocationGrant) { change ->
            onUpdate { it.copy(weather = change(it.weather)) }
        }
        ExtrasSection(settings, onUpdate)
        AppearanceSection(settings.themeMode) { mode -> onUpdate { it.copy(themeMode = mode) } }
        UpdateSection(settings, updateState, onUpdate, onCheckForUpdate)
        SectionCard(title = stringResource(R.string.section_permissions)) { PermissionRows(onLocationGrant) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GoalSection(
    settings: AppSettings,
    goalMl: Int,
    onUpdate: SettingsUpdate,
    onSetGoal: (Int) -> Unit,
) {
    SectionCard(title = stringResource(R.string.section_goal)) {
        NumberSettingRow(
            stringResource(R.string.daily_goal),
            goalMl,
            range = 500..5000,
            step = 50,
            format = Format::litres,
            onChange = onSetGoal,
            supporting = stringResource(R.string.daily_goal_hint),
        )
        TimeSettingRow(
            stringResource(R.string.day_start),
            settings.dayStart,
            { t -> onUpdate { it.copy(dayStart = t) } },
            supporting = stringResource(R.string.day_start_hint),
        )
    }
}

@Composable
private fun AmountsSection(
    onOpenPresets: () -> Unit,
    onOpenSurface: (String) -> Unit,
) {
    SectionCard(title = stringResource(R.string.section_amounts)) {
        SettingRow(stringResource(R.string.preset_library), "›", onOpenPresets)
        HorizontalDivider()
        SettingRow(stringResource(R.string.surface_home), "›", { onOpenSurface(Surfaces.HOME) })
        SettingRow(stringResource(R.string.surface_shortcuts), "›", { onOpenSurface(Surfaces.SHORTCUTS) })
        SettingRow(stringResource(R.string.surface_notification), "›", { onOpenSurface(Surfaces.NOTIFICATION) })
    }
}

@Composable
private fun ReminderSection(
    r: ReminderSettings,
    onChange: ((ReminderSettings) -> ReminderSettings) -> Unit,
) {
    val minuteUnit = stringResource(R.string.minutes_short)
    val minutesFormat: (Int) -> String = { "$it $minuteUnit" }
    SectionCard(title = stringResource(R.string.section_reminders)) {
        SwitchRow(stringResource(R.string.reminders_enabled), r.enabled, { v -> onChange { it.copy(enabled = v) } })
        NumberSettingRow(
            stringResource(R.string.reminder_interval),
            r.intervalMinutes,
            range = 30..240,
            step = 15,
            format = minutesFormat,
            onChange = { v -> onChange { it.copy(intervalMinutes = v) } },
            supporting = stringResource(R.string.reminder_interval_hint),
        )
        SwitchRow(
            stringResource(R.string.halve_when_behind),
            r.halveWhenBehind,
            { v -> onChange { it.copy(halveWhenBehind = v) } },
        )
        NumberSettingRow(
            stringResource(R.string.behind_threshold),
            r.behindThresholdPercent,
            range = 5..50,
            step = 5,
            format = { "$it %" },
            onChange = { v -> onChange { it.copy(behindThresholdPercent = v) } },
        )
        NumberSettingRow(
            stringResource(R.string.quiet_after_wake),
            r.quietAfterWakeMinutes,
            range = 0..120,
            step = 15,
            format = minutesFormat,
            onChange = { v -> onChange { it.copy(quietAfterWakeMinutes = v) } },
        )
        NumberSettingRow(
            stringResource(R.string.snooze_duration),
            r.snoozeMinutes,
            range = 10..120,
            step = 5,
            format = minutesFormat,
            onChange = { v -> onChange { it.copy(snoozeMinutes = v) } },
        )
        SwitchRow(
            stringResource(R.string.respect_dnd),
            r.respectDoNotDisturb,
            { v -> onChange { it.copy(respectDoNotDisturb = v) } },
            supporting = stringResource(R.string.respect_dnd_hint),
        )
    }
}

@Composable
private fun WeatherSection(
    w: WeatherRules,
    onLocationGrant: () -> Unit,
    onChange: ((WeatherRules) -> WeatherRules) -> Unit,
) {
    val context = LocalContext.current
    val locationLauncher = rememberPermissionRequest { granted -> if (granted) onLocationGrant() }
    val bonusFormat: (Int) -> String = { "+" + Format.volume(it) }
    SectionCard(title = stringResource(R.string.section_weather)) {
        SwitchRow(
            stringResource(R.string.weather_enabled),
            w.enabled,
            { v ->
                onChange { it.copy(enabled = v) }
                if (v && !Permissions.locationGranted(context)) {
                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            supporting = stringResource(R.string.weather_hint),
        )
        NumberSettingRow(
            stringResource(R.string.warm_from),
            w.warmThresholdC.toInt(),
            range = 18..35,
            step = 1,
            format = { "$it °C" },
            onChange = { v -> onChange { it.copy(warmThresholdC = v.toDouble()) } },
        )
        NumberSettingRow(
            stringResource(R.string.warm_bonus),
            w.warmBonusMl,
            range = 50..1000,
            step = 50,
            format = bonusFormat,
            onChange = { v -> onChange { it.copy(warmBonusMl = v) } },
        )
        NumberSettingRow(
            stringResource(R.string.hot_from),
            w.hotThresholdC.toInt(),
            range = 20..45,
            step = 1,
            format = { "$it °C" },
            onChange = { v -> onChange { it.copy(hotThresholdC = v.toDouble()) } },
        )
        NumberSettingRow(
            stringResource(R.string.hot_bonus),
            w.hotBonusMl,
            range = 50..1500,
            step = 50,
            format = bonusFormat,
            onChange = { v -> onChange { it.copy(hotBonusMl = v) } },
        )
    }
}

@Composable
private fun ExtrasSection(
    settings: AppSettings,
    onUpdate: SettingsUpdate,
) {
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
}

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
) {
    val context = LocalContext.current
    SectionCard(title = stringResource(R.string.section_appearance)) {
        ChoiceSettingRow(
            stringResource(R.string.design),
            ThemeMode.entries.map { it to stringResource(themeLabel(it)) },
            themeMode,
            onThemeMode,
        )
        ChoiceSettingRow(
            stringResource(R.string.language),
            AppLanguage.supported.map { it to languageLabel(it) },
            AppLanguage.current(context),
            { AppLanguage.set(context, it) },
        )
    }
}

@Composable
private fun UpdateSection(
    settings: AppSettings,
    state: UpdateState,
    onUpdate: SettingsUpdate,
    onCheck: () -> Unit,
) {
    SectionCard(title = stringResource(R.string.section_updates)) {
        SwitchRow(
            stringResource(R.string.auto_update_check),
            settings.autoUpdateCheck,
            { v -> onUpdate { it.copy(autoUpdateCheck = v) } },
            supporting = stringResource(R.string.auto_update_check_hint),
        )
        SettingRow(
            stringResource(R.string.check_updates),
            updateStatusText(state),
            onCheck,
            supporting = stringResource(R.string.installed_version, BuildConfig.VERSION_NAME),
        )
    }
}

@Composable
private fun updateStatusText(state: UpdateState): String =
    when (state) {
        UpdateState.Idle -> "›"
        UpdateState.Checking -> stringResource(R.string.update_checking)
        is UpdateState.UpToDate -> stringResource(R.string.update_up_to_date)
        is UpdateState.Available -> stringResource(R.string.update_available_short, state.release.version.toString())
        is UpdateState.Downloading -> "${(state.progress * 100).toInt()} %"
        is UpdateState.ReadyToInstall -> stringResource(R.string.update_ready_short)
        is UpdateState.Failed -> stringResource(R.string.update_failed_short)
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
