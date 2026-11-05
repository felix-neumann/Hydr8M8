package de.felixneumann.hydr8m8.domain

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class TimeWindow(
    val start: Instant,
    val end: Instant,
) {
    operator fun contains(instant: Instant): Boolean = !instant.isBefore(start) && instant.isBefore(end)

    val duration: Duration get() = Duration.between(start, end)
}

/** Wake/sleep times. If [sleep] is not after [wake] the window ends on the next calendar day. */
data class WakeWindow(
    val wake: LocalTime,
    val sleep: LocalTime,
)

data class WakeSchedule(
    val weekday: WakeWindow,
    val weekend: WakeWindow,
) {
    fun windowOf(date: LocalDate): WakeWindow =
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) weekend else weekday

    /** Wake window that starts on [date]. */
    fun windowFor(
        date: LocalDate,
        zone: ZoneId,
    ): TimeWindow {
        val w = windowOf(date)
        val start = date.atTime(w.wake).atZone(zone).toInstant()
        val endDate = if (w.sleep.isAfter(w.wake)) date else date.plusDays(1)
        val end = endDate.atTime(w.sleep).atZone(zone).toInstant()
        return TimeWindow(start, end)
    }

    fun isAwake(
        instant: Instant,
        zone: ZoneId,
    ): Boolean {
        val date = instant.atZone(zone).toLocalDate()
        return instant in windowFor(date, zone) || instant in windowFor(date.minusDays(1), zone)
    }

    companion object {
        val DEFAULT_WINDOW = WakeWindow(LocalTime.of(7, 0), LocalTime.of(22, 30))
        val DEFAULT = WakeSchedule(DEFAULT_WINDOW, DEFAULT_WINDOW)
    }
}

/** Linear drinking plan across the wake window. */
object Pace {
    fun expectedMl(
        at: Instant,
        window: TimeWindow,
        goalMl: Int,
    ): Int {
        val total = window.duration.toMillis()
        if (total <= 0) return goalMl
        val elapsed = Duration.between(window.start, at).toMillis().coerceIn(0, total)
        return (goalMl.toLong() * elapsed / total).toInt()
    }

    /** How far behind plan, as a fraction of the goal (negative = ahead). */
    fun behindFraction(
        at: Instant,
        window: TimeWindow,
        intakeMl: Int,
        goalMl: Int,
    ): Double = if (goalMl <= 0) 0.0 else (expectedMl(at, window, goalMl) - intakeMl).toDouble() / goalMl
}
