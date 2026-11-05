package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderPlannerTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val wednesday = LocalDate.of(2026, 11, 4)
    private val schedule = WakeSchedule.DEFAULT // 07:00 - 22:30 every day
    private val settings = ReminderSettings()

    private fun at(time: String, date: LocalDate = wednesday): Instant =
        LocalDateTime.of(date, LocalTime.parse(time)).atZone(zone).toInstant()

    private fun next(
        now: Instant,
        intakeMl: Int = 0,
        lastEntryAt: Instant? = null,
        lastReminderAt: Instant? = null,
        snoozedUntil: Instant? = null,
        goalMl: Int = 2000,
        settings: ReminderSettings = this.settings,
    ) = ReminderPlanner.nextReminderAt(
        ReminderInput(
            now = now,
            logicalDate = wednesday,
            intakeMl = intakeMl,
            goalMl = goalMl,
            lastEntryAt = lastEntryAt,
            lastReminderAt = lastReminderAt,
            snoozedUntil = snoozedUntil,
        ),
        schedule = schedule,
        settings = settings,
        zone = zone,
    )

    @Test
    fun `first reminder is one interval after waking`() {
        assertThat(next(now = at("06:00"))).isEqualTo(at("09:00"))
    }

    @Test
    fun `reminder follows last entry when on pace`() {
        assertThat(next(now = at("10:05"), intakeMl = 1000, lastEntryAt = at("10:00"))).isEqualTo(at("12:00"))
    }

    @Test
    fun `interval is halved when behind pace`() {
        assertThat(next(now = at("12:01"), intakeMl = 200, lastEntryAt = at("12:00"))).isEqualTo(at("13:00"))
    }

    @Test
    fun `halving can be switched off`() {
        val noHalving = settings.copy(halveWhenBehind = false)
        assertThat(next(now = at("12:01"), intakeMl = 200, lastEntryAt = at("12:00"), settings = noHalving))
            .isEqualTo(at("14:00"))
    }

    @Test
    fun `last reminder also resets the interval`() {
        // Nothing logged, reminder fired at 09:00 -> behind at 10:00 -> halved.
        assertThat(next(now = at("09:00:01"), lastReminderAt = at("09:00"))).isEqualTo(at("10:00"))
    }

    @Test
    fun `no more reminders after goal is reached - next day instead`() {
        val thursday = wednesday.plusDays(1)
        assertThat(next(now = at("15:00"), intakeMl = 2000, lastEntryAt = at("15:00"))).isEqualTo(at("09:00", thursday))
    }

    @Test
    fun `reminder that would fall into sleep window moves to next day`() {
        val thursday = wednesday.plusDays(1)
        assertThat(next(now = at("21:31"), intakeMl = 1900, lastEntryAt = at("21:30"))).isEqualTo(at("09:00", thursday))
    }

    @Test
    fun `quiet period after waking is respected`() {
        val short = settings.copy(intervalMinutes = 15)
        assertThat(next(now = at("06:00"), settings = short)).isEqualTo(at("07:30"))
    }

    @Test
    fun `snooze wins`() {
        assertThat(
            next(now = at("09:00"), lastReminderAt = at("09:00"), snoozedUntil = at("09:30")),
        ).isEqualTo(at("09:30"))
    }

    @Test
    fun `overdue reminder fires now`() {
        val now = at("11:00")
        assertThat(next(now = now, intakeMl = 1000, lastEntryAt = at("08:00"))).isEqualTo(now)
    }

    @Test
    fun `disabled reminders yield null`() {
        assertThat(next(now = at("10:00"), settings = settings.copy(enabled = false))).isNull()
    }
}
