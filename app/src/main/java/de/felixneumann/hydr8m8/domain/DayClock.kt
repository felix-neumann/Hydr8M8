package de.felixneumann.hydr8m8.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Maps instants to "logical" days. A logical day starts at [dayStart] (default 02:00), so a drink
 * at 01:30 still belongs to the previous day.
 */
class DayClock(
    val zone: ZoneId,
    val dayStart: LocalTime,
) {
    fun logicalDate(instant: Instant): LocalDate {
        val local = LocalDateTime.ofInstant(instant, zone)
        return if (local.toLocalTime() < dayStart) local.toLocalDate().minusDays(1) else local.toLocalDate()
    }

    fun startOf(date: LocalDate): Instant = date.atTime(dayStart).atZone(zone).toInstant()

    fun endOf(date: LocalDate): Instant = startOf(date.plusDays(1))

    companion object {
        val DEFAULT_DAY_START: LocalTime = LocalTime.of(2, 0)
    }
}
