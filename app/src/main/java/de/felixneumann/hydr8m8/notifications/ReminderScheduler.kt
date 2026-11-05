package de.felixneumann.hydr8m8.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.felixneumann.hydr8m8.container
import de.felixneumann.hydr8m8.data.HydrationRepository
import de.felixneumann.hydr8m8.data.SettingsRepository
import de.felixneumann.hydr8m8.domain.DayClock
import de.felixneumann.hydr8m8.domain.History
import de.felixneumann.hydr8m8.domain.ReminderInput
import de.felixneumann.hydr8m8.domain.ReminderPlanner
import de.felixneumann.hydr8m8.domain.Surfaces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * Owns two alarms: the next drink reminder and a "tick" at the next day/wake/sleep boundary that
 * refreshes widgets and the progress notification.
 */
class ReminderScheduler(
    private val context: Context,
    private val repository: HydrationRepository,
    private val settings: SettingsRepository,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    suspend fun nextReminderAt(history: History? = null): Instant? {
        val h = history ?: repository.historyNow()
        val s = settings.current()
        val runtime = settings.currentRuntime()
        val today = h.todaySummary
        return ReminderPlanner.nextReminderAt(
            ReminderInput(
                now = repository.now(),
                logicalDate = h.today,
                intakeMl = today.waterMl,
                goalMl = today.goalMl,
                lastEntryAt = repository.lastEntryAt(),
                lastReminderAt = runtime.lastReminderAt,
                snoozedUntil = runtime.snoozedUntil,
            ),
            s.schedule,
            s.reminders,
            ZoneId.systemDefault(),
        )
    }

    suspend fun reschedule(history: History? = null) {
        val h = history ?: repository.historyNow()
        val next = nextReminderAt(h)
        val reminderIntent = pending(ACTION_REMINDER)
        if (next == null) alarms.cancel(reminderIntent) else setAlarm(next, reminderIntent)
        if (h.todaySummary.reached) Notifications.cancelReminder(context)
        setAlarm(nextBoundary(), pending(ACTION_TICK))
    }

    /** Next moment where day-dependent UI must refresh: wake, sleep or logical day start. */
    private suspend fun nextBoundary(): Instant {
        val s = settings.current()
        val zone = ZoneId.systemDefault()
        val now = repository.now()
        val clock = DayClock(zone, s.dayStart)
        val today = clock.logicalDate(now)
        val candidates =
            (-1L..1L).flatMap { offset ->
                val date = today.plusDays(offset)
                val window = s.schedule.windowFor(date, zone)
                listOf(window.start, window.end, clock.startOf(date))
            }
        return candidates.filter { it.isAfter(now) }.min().plusSeconds(1)
    }

    private fun setAlarm(
        at: Instant,
        intent: PendingIntent,
    ) {
        if (alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        }
    }

    private fun pending(action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        const val ACTION_REMINDER = "de.felixneumann.hydr8m8.REMINDER"
        const val ACTION_TICK = "de.felixneumann.hydr8m8.TICK"
    }
}

/** Receives reminder/tick alarms and system events (boot, time change, app update). */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pending = goAsync()
        val c = context.container
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (intent.action == ReminderScheduler.ACTION_REMINDER) maybeRemind(context)
                c.repository.ensureSeeded()
                c.actions.refreshAll()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun maybeRemind(context: Context) {
        val c = context.container
        val history = c.repository.historyNow()
        val due = c.reminderScheduler.nextReminderAt(history) ?: return
        val now = c.repository.now()
        if (due.isAfter(now.plusSeconds(TOLERANCE_SECONDS))) return // stale alarm, refresh reschedules
        val settings = c.settings.current()
        c.settings.setLastReminderAt(now)
        c.settings.setSnoozedUntil(null)
        if (settings.reminders.respectDoNotDisturb && isDoNotDisturbActive(context)) return
        if (!settings.schedule.isAwake(now, ZoneId.systemDefault())) return
        Notifications.postReminder(
            context,
            history,
            c.repository.lastEntryAt(),
            now,
            c.repository.surfaceNow(Surfaces.NOTIFICATION),
        )
    }

    private fun isDoNotDisturbActive(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).currentInterruptionFilter !=
            NotificationManager.INTERRUPTION_FILTER_ALL

    private companion object {
        const val TOLERANCE_SECONDS = 60L
    }
}
