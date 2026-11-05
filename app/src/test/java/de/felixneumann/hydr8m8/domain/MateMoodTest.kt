package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class MateMoodTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val window = WakeSchedule.DEFAULT.windowFor(LocalDate.of(2026, 11, 4), zone) // 07:00-22:30

    private fun mood(
        time: String,
        intake: Int,
        alcohol: Boolean = false,
        goal: Int = 3100,
    ) = MateMood.of(
        now = LocalDateTime.parse("2026-11-04T$time").atZone(zone).toInstant(),
        window = window,
        intakeMl = intake,
        goalMl = goal,
        alcoholToday = alcohol,
    )

    @Test
    fun `sleepy outside wake window`() {
        assertThat(mood("23:00", 0)).isEqualTo(MateMood.SLEEPY)
        assertThat(mood("06:00", 0)).isEqualTo(MateMood.SLEEPY)
    }

    @Test
    fun `champion when goal reached`() {
        assertThat(mood("14:45", 3100)).isEqualTo(MateMood.CHAMPION)
    }

    @Test
    fun `happy when on pace`() {
        assertThat(mood("14:45", 1550)).isEqualTo(MateMood.HAPPY)
    }

    @Test
    fun `thirsty when more than 15 percent behind`() {
        // expected 1550, intake 1050 -> 500 behind = 16 %
        assertThat(mood("14:45", 1050)).isEqualTo(MateMood.THIRSTY)
    }

    @Test
    fun `very thirsty when more than 35 percent behind`() {
        // expected 1550, intake 400 -> 1150 behind = 37 %
        assertThat(mood("14:45", 400)).isEqualTo(MateMood.VERY_THIRSTY)
    }

    @Test
    fun `tipsy with alcohol when not behind`() {
        assertThat(mood("14:45", 1550, alcohol = true)).isEqualTo(MateMood.TIPSY)
        assertThat(mood("14:45", 400, alcohol = true)).isEqualTo(MateMood.VERY_THIRSTY)
    }
}
