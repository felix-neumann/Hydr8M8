package de.felixneumann.hydr8m8.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.felixneumann.hydr8m8.domain.DayClock
import de.felixneumann.hydr8m8.domain.ReminderSettings
import de.felixneumann.hydr8m8.domain.WakeSchedule
import de.felixneumann.hydr8m8.domain.WakeWindow
import de.felixneumann.hydr8m8.domain.WeatherRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalTime

data class AppSettings(
    val onboardingDone: Boolean = false,
    val defaultGoalMl: Int = DEFAULT_GOAL_ML,
    val dayStart: LocalTime = DayClock.DEFAULT_DAY_START,
    val schedule: WakeSchedule = WakeSchedule.DEFAULT,
    val reminders: ReminderSettings = ReminderSettings(),
    val weather: WeatherRules = WeatherRules(),
    val liveUpdateEnabled: Boolean = true,
    val soundsEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_GOAL_ML = 2000
    }
}

/** Volatile bookkeeping that is not user-facing configuration. */
data class RuntimeState(
    val lastReminderAt: Instant? = null,
    val snoozedUntil: Instant? = null,
    val lastMilestoneKey: String? = null,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context,
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DEFAULT_GOAL = intPreferencesKey("default_goal_ml")
        val DAY_START = intPreferencesKey("day_start_minutes")
        val WEEKDAY_WAKE = intPreferencesKey("weekday_wake_minutes")
        val WEEKDAY_SLEEP = intPreferencesKey("weekday_sleep_minutes")
        val WEEKEND_WAKE = intPreferencesKey("weekend_wake_minutes")
        val WEEKEND_SLEEP = intPreferencesKey("weekend_sleep_minutes")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_INTERVAL = intPreferencesKey("reminder_interval_minutes")
        val HALVE_WHEN_BEHIND = booleanPreferencesKey("reminder_halve_when_behind")
        val BEHIND_THRESHOLD = intPreferencesKey("reminder_behind_threshold_percent")
        val QUIET_AFTER_WAKE = intPreferencesKey("reminder_quiet_after_wake_minutes")
        val SNOOZE_MINUTES = intPreferencesKey("reminder_snooze_minutes")
        val RESPECT_DND = booleanPreferencesKey("reminder_respect_dnd")
        val WEATHER_ENABLED = booleanPreferencesKey("weather_enabled")
        val WEATHER_WARM_C = doublePreferencesKey("weather_warm_c")
        val WEATHER_WARM_ML = intPreferencesKey("weather_warm_ml")
        val WEATHER_HOT_C = doublePreferencesKey("weather_hot_c")
        val WEATHER_HOT_ML = intPreferencesKey("weather_hot_ml")
        val LIVE_UPDATE = booleanPreferencesKey("live_update_enabled")
        val SOUNDS = booleanPreferencesKey("sounds_enabled")
        val LAST_REMINDER = longPreferencesKey("last_reminder_at")
        val SNOOZED_UNTIL = longPreferencesKey("snoozed_until")
        val LAST_MILESTONE = stringPreferencesKey("last_milestone_key")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    val runtime: Flow<RuntimeState> =
        context.dataStore.data.map {
            RuntimeState(
                lastReminderAt = it[Keys.LAST_REMINDER]?.let(Instant::ofEpochMilli),
                snoozedUntil = it[Keys.SNOOZED_UNTIL]?.let(Instant::ofEpochMilli),
                lastMilestoneKey = it[Keys.LAST_MILESTONE],
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun currentRuntime(): RuntimeState = runtime.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.ONBOARDING_DONE] = s.onboardingDone
            prefs[Keys.DEFAULT_GOAL] = s.defaultGoalMl
            prefs[Keys.DAY_START] = s.dayStart.toMinutes()
            prefs[Keys.WEEKDAY_WAKE] = s.schedule.weekday.wake.toMinutes()
            prefs[Keys.WEEKDAY_SLEEP] = s.schedule.weekday.sleep.toMinutes()
            prefs[Keys.WEEKEND_WAKE] = s.schedule.weekend.wake.toMinutes()
            prefs[Keys.WEEKEND_SLEEP] = s.schedule.weekend.sleep.toMinutes()
            prefs[Keys.REMINDERS_ENABLED] = s.reminders.enabled
            prefs[Keys.REMINDER_INTERVAL] = s.reminders.intervalMinutes
            prefs[Keys.HALVE_WHEN_BEHIND] = s.reminders.halveWhenBehind
            prefs[Keys.BEHIND_THRESHOLD] = s.reminders.behindThresholdPercent
            prefs[Keys.QUIET_AFTER_WAKE] = s.reminders.quietAfterWakeMinutes
            prefs[Keys.SNOOZE_MINUTES] = s.reminders.snoozeMinutes
            prefs[Keys.RESPECT_DND] = s.reminders.respectDoNotDisturb
            prefs[Keys.WEATHER_ENABLED] = s.weather.enabled
            prefs[Keys.WEATHER_WARM_C] = s.weather.warmThresholdC
            prefs[Keys.WEATHER_WARM_ML] = s.weather.warmBonusMl
            prefs[Keys.WEATHER_HOT_C] = s.weather.hotThresholdC
            prefs[Keys.WEATHER_HOT_ML] = s.weather.hotBonusMl
            prefs[Keys.LIVE_UPDATE] = s.liveUpdateEnabled
            prefs[Keys.SOUNDS] = s.soundsEnabled
        }
    }

    suspend fun setLastReminderAt(at: Instant) {
        context.dataStore.edit { it[Keys.LAST_REMINDER] = at.toEpochMilli() }
    }

    suspend fun setSnoozedUntil(at: Instant?) {
        context.dataStore.edit {
            if (at == null) it.remove(Keys.SNOOZED_UNTIL) else it[Keys.SNOOZED_UNTIL] = at.toEpochMilli()
        }
    }

    suspend fun setLastMilestoneKey(key: String) {
        context.dataStore.edit { it[Keys.LAST_MILESTONE] = key }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()

        fun time(
            key: Preferences.Key<Int>,
            fallback: LocalTime,
        ) = this[key]?.let(::minutesToTime) ?: fallback
        return AppSettings(
            onboardingDone = this[Keys.ONBOARDING_DONE] ?: d.onboardingDone,
            defaultGoalMl = this[Keys.DEFAULT_GOAL] ?: d.defaultGoalMl,
            dayStart = time(Keys.DAY_START, d.dayStart),
            schedule =
                WakeSchedule(
                    weekday =
                        WakeWindow(
                            time(Keys.WEEKDAY_WAKE, d.schedule.weekday.wake),
                            time(Keys.WEEKDAY_SLEEP, d.schedule.weekday.sleep),
                        ),
                    weekend =
                        WakeWindow(
                            time(Keys.WEEKEND_WAKE, d.schedule.weekend.wake),
                            time(Keys.WEEKEND_SLEEP, d.schedule.weekend.sleep),
                        ),
                ),
            reminders =
                ReminderSettings(
                    enabled = this[Keys.REMINDERS_ENABLED] ?: d.reminders.enabled,
                    intervalMinutes = this[Keys.REMINDER_INTERVAL] ?: d.reminders.intervalMinutes,
                    halveWhenBehind = this[Keys.HALVE_WHEN_BEHIND] ?: d.reminders.halveWhenBehind,
                    behindThresholdPercent = this[Keys.BEHIND_THRESHOLD] ?: d.reminders.behindThresholdPercent,
                    quietAfterWakeMinutes = this[Keys.QUIET_AFTER_WAKE] ?: d.reminders.quietAfterWakeMinutes,
                    snoozeMinutes = this[Keys.SNOOZE_MINUTES] ?: d.reminders.snoozeMinutes,
                    respectDoNotDisturb = this[Keys.RESPECT_DND] ?: d.reminders.respectDoNotDisturb,
                ),
            weather =
                WeatherRules(
                    enabled = this[Keys.WEATHER_ENABLED] ?: d.weather.enabled,
                    warmThresholdC = this[Keys.WEATHER_WARM_C] ?: d.weather.warmThresholdC,
                    warmBonusMl = this[Keys.WEATHER_WARM_ML] ?: d.weather.warmBonusMl,
                    hotThresholdC = this[Keys.WEATHER_HOT_C] ?: d.weather.hotThresholdC,
                    hotBonusMl = this[Keys.WEATHER_HOT_ML] ?: d.weather.hotBonusMl,
                ),
            liveUpdateEnabled = this[Keys.LIVE_UPDATE] ?: d.liveUpdateEnabled,
            soundsEnabled = this[Keys.SOUNDS] ?: d.soundsEnabled,
        )
    }
}

private const val MINUTES_PER_HOUR = 60

private fun LocalTime.toMinutes(): Int = hour * MINUTES_PER_HOUR + minute

private fun minutesToTime(minutes: Int): LocalTime =
    LocalTime.of(
        minutes / MINUTES_PER_HOUR,
        minutes % MINUTES_PER_HOUR,
    )
