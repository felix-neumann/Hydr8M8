package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class DayClockTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val clock = DayClock(zone, LocalTime.of(2, 0))

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant()

    @Test
    fun `entry after day start belongs to same calendar day`() {
        assertThat(clock.logicalDate(at("2026-11-05T02:00"))).isEqualTo(LocalDate.of(2026, 11, 5))
    }

    @Test
    fun `entry before day start belongs to previous day`() {
        assertThat(clock.logicalDate(at("2026-11-05T01:30"))).isEqualTo(LocalDate.of(2026, 11, 4))
    }

    @Test
    fun `day range spans from day start to next day start`() {
        val date = LocalDate.of(2026, 11, 5)
        assertThat(clock.startOf(date)).isEqualTo(at("2026-11-05T02:00"))
        assertThat(clock.endOf(date)).isEqualTo(at("2026-11-06T02:00"))
    }

    @Test
    fun `midnight day start behaves like calendar days`() {
        val midnight = DayClock(zone, LocalTime.MIDNIGHT)
        assertThat(midnight.logicalDate(at("2026-11-05T00:00"))).isEqualTo(LocalDate.of(2026, 11, 5))
        assertThat(midnight.logicalDate(at("2026-11-04T23:59"))).isEqualTo(LocalDate.of(2026, 11, 4))
    }
}
