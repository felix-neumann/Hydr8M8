package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class WakeScheduleTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val schedule =
        WakeSchedule(
            weekday = WakeWindow(LocalTime.of(6, 30), LocalTime.of(22, 0)),
            weekend = WakeWindow(LocalTime.of(9, 0), LocalTime.of(0, 30)),
        )

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant()

    @Test
    fun `weekday window`() {
        val w = schedule.windowFor(LocalDate.of(2026, 11, 4), zone)
        assertThat(w.start).isEqualTo(at("2026-11-04T06:30"))
        assertThat(w.end).isEqualTo(at("2026-11-04T22:00"))
    }

    @Test
    fun `weekend window may end after midnight`() {
        val w = schedule.windowFor(LocalDate.of(2026, 11, 7), zone)
        assertThat(w.start).isEqualTo(at("2026-11-07T09:00"))
        assertThat(w.end).isEqualTo(at("2026-11-08T00:30"))
    }

    @Test
    fun `awake check covers windows crossing midnight`() {
        assertThat(schedule.isAwake(at("2026-11-08T00:15"), zone)).isTrue()
        assertThat(schedule.isAwake(at("2026-11-08T01:00"), zone)).isFalse()
        assertThat(schedule.isAwake(at("2026-11-04T12:00"), zone)).isTrue()
        assertThat(schedule.isAwake(at("2026-11-04T06:00"), zone)).isFalse()
    }

    @Test
    fun `pace grows linearly across the window`() {
        val w = WakeSchedule.DEFAULT.windowFor(LocalDate.of(2026, 11, 4), zone) // 07:00-22:30 = 15.5 h
        assertThat(Pace.expectedMl(at("2026-11-04T06:00"), w, 3100)).isEqualTo(0)
        assertThat(Pace.expectedMl(at("2026-11-04T14:45"), w, 3100)).isEqualTo(1550)
        assertThat(Pace.expectedMl(at("2026-11-04T23:00"), w, 3100)).isEqualTo(3100)
    }
}
