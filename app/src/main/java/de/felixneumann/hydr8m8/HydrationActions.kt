package de.felixneumann.hydr8m8

import de.felixneumann.hydr8m8.domain.DrinkIcon
import de.felixneumann.hydr8m8.domain.DrinkKind
import de.felixneumann.hydr8m8.domain.Entry
import de.felixneumann.hydr8m8.domain.History
import de.felixneumann.hydr8m8.domain.Milestone
import de.felixneumann.hydr8m8.domain.Milestones
import de.felixneumann.hydr8m8.notifications.Notifications
import de.felixneumann.hydr8m8.widget.WidgetUpdater
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/** What a logging action changed, so the UI can pick the right sound / message. */
data class LogResult(
    val entry: Entry,
    val goalJustReached: Boolean,
    val newMilestone: Milestone?,
    val history: History,
)

/**
 * Single entry point for every data change (app, widget, shortcut, notification). Each change is
 * followed by [refreshAll], which keeps reminders, widgets, shortcuts, the progress notification
 * and milestone notifications in sync.
 */
class HydrationActions(
    private val c: AppContainer,
) {
    private val refreshMutex = Mutex()

    suspend fun logPreset(presetId: Long): LogResult? {
        val preset = c.repository.preset(presetId) ?: return null
        return log(preset.kind, preset.volumeMl, preset.abvPercent, preset.icon)
    }

    suspend fun log(
        kind: DrinkKind,
        volumeMl: Int,
        abvPercent: Double?,
        icon: DrinkIcon,
        at: Instant = c.repository.now(),
    ): LogResult {
        val before = c.repository.historyNow()
        val entry = c.repository.log(kind, volumeMl, abvPercent, icon, at)
        c.settings.setSnoozedUntil(null)
        val (after, milestone) = refreshAll()
        val date = (c.repository.dayClock()).logicalDate(at)
        val goalJustReached = !before.day(date).reached && after.day(date).reached
        return LogResult(entry, goalJustReached, milestone, after)
    }

    suspend fun updateEntry(entry: Entry) {
        c.repository.updateEntry(entry)
        refreshAll()
    }

    suspend fun deleteEntry(id: Long) {
        c.repository.deleteEntry(id)
        refreshAll()
    }

    suspend fun undoLast(): Entry? {
        val removed = c.repository.undoLast()
        if (removed != null) refreshAll()
        return removed
    }

    suspend fun snoozeReminder() {
        val s = c.settings.current()
        c.settings.setSnoozedUntil(c.repository.now().plusSeconds(s.reminders.snoozeMinutes * SECONDS_PER_MINUTE))
        Notifications.cancelReminder(c.appContext)
        c.reminderScheduler.reschedule()
    }

    /**
     * Brings every derived surface up to date. Returns the fresh history and a milestone if one was
     * newly reached (and notified) by this refresh.
     */
    suspend fun refreshAll(): Pair<History, Milestone?> =
        refreshMutex.withLock {
            val history = c.repository.historyNow()
            val settings = c.settings.current()
            val milestone = checkMilestone(history)
            c.reminderScheduler.reschedule(history)
            c.progressNotifier.update(history, settings)
            c.shortcuts.publish(c.repository)
            WidgetUpdater.updateAll(c.appContext)
            c.weather.fetchIfNeeded(history.today)
            history to milestone
        }

    private suspend fun checkMilestone(history: History): Milestone? {
        val streak = history.streak
        val lastCovered = if (streak.todayCounted) history.today else history.today.minusDays(1)
        val milestone = Milestones.highestReached(streak.currentStart, streak.currentDays, lastCovered) ?: return null
        val runtime = c.settings.currentRuntime()
        if (milestone.key == runtime.lastMilestoneKey) return null
        c.settings.setLastMilestoneKey(milestone.key)
        Notifications.postMilestone(c.appContext, milestone, history)
        return milestone
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60L
    }
}
