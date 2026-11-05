package de.felixneumann.hydr8m8.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReminderSettings(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 120,
    val halveWhenBehind: Boolean = true,
    val behindThresholdPercent: Int = 15,
    val quietAfterWakeMinutes: Int = 30,
    val snoozeMinutes: Int = 30,
    val respectDoNotDisturb: Boolean = true,
)

data class ReminderInput(
    val now: Instant,
    /** Logical date that [now] belongs to. */
    val logicalDate: LocalDate,
    val intakeMl: Int,
    val goalMl: Int,
    val lastEntryAt: Instant?,
    val lastReminderAt: Instant?,
    val snoozedUntil: Instant?,
)

/**
 * Computes when the next "time to drink" reminder is due. The wake window of a logical day is the
 * one starting on the same calendar date.
 */
object ReminderPlanner {
    fun nextReminderAt(
        input: ReminderInput,
        schedule: WakeSchedule,
        settings: ReminderSettings,
        zone: ZoneId,
    ): Instant? {
        if (!settings.enabled) return null
        val today = schedule.windowFor(input.logicalDate, zone)
        val goalReached = Progress.isReached(input.intakeMl, input.goalMl)

        if (!goalReached && input.now.isBefore(today.end)) {
            val snooze = input.snoozedUntil
            if (snooze != null && snooze.isAfter(input.now)) {
                if (snooze.isBefore(today.end)) return snooze
            } else {
                val anchor = listOfNotNull(today.start, input.lastEntryAt, input.lastReminderAt).max()
                val candidate = candidateIn(today, anchor, input.intakeMl, input.goalMl, settings)
                if (candidate.isBefore(today.end)) return maxOf(candidate, input.now)
            }
        }

        // Nothing more today: first reminder of the next day (nothing logged there yet).
        val tomorrow = schedule.windowFor(input.logicalDate.plusDays(1), zone)
        return candidateIn(tomorrow, tomorrow.start, intakeMl = 0, goalMl = input.goalMl, settings = settings)
    }

    private fun candidateIn(
        window: TimeWindow,
        anchor: Instant,
        intakeMl: Int,
        goalMl: Int,
        settings: ReminderSettings,
    ): Instant {
        val interval = Duration.ofMinutes(settings.intervalMinutes.toLong())
        val normal = anchor + interval
        val early = anchor + interval.dividedBy(2)
        val behindAtEarly =
            Pace.behindFraction(early, window, intakeMl, goalMl) * 100 > settings.behindThresholdPercent
        val chosen = if (settings.halveWhenBehind && behindAtEarly) early else normal
        val earliest = window.start + Duration.ofMinutes(settings.quietAfterWakeMinutes.toLong())
        return maxOf(chosen, earliest)
    }
}
