package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {
    private val start = LocalDate.of(2026, 1, 1)

    /** Builds results from a pattern: '+' = success, '-' = miss. First char is [start]. */
    private fun calc(
        pattern: String,
        todaySuccess: Boolean? = null,
    ): StreakState {
        val days = pattern.mapIndexed { i, c -> start.plusDays(i.toLong()) to (c == '+') }.toMap()
        val today = start.plusDays(pattern.length.toLong())
        val results = if (todaySuccess != null) days + (today to todaySuccess) else days
        return StreakCalculator.calculate(firstDate = start, today = today) { results[it] ?: false }
    }

    @Test
    fun `consecutive successes build the streak`() {
        val s = calc("+++")
        assertThat(s.currentDays).isEqualTo(3)
        assertThat(s.currentStart).isEqualTo(start)
    }

    @Test
    fun `unfinished today does not break the streak`() {
        val s = calc("+++", todaySuccess = false)
        assertThat(s.currentDays).isEqualTo(3)
        assertThat(s.todayCounted).isFalse()
    }

    @Test
    fun `successful today counts immediately`() {
        val s = calc("+++", todaySuccess = true)
        assertThat(s.currentDays).isEqualTo(4)
        assertThat(s.todayCounted).isTrue()
    }

    @Test
    fun `miss without joker ends the streak`() {
        val s = calc("+++-++")
        assertThat(s.currentDays).isEqualTo(2)
        assertThat(s.currentStart).isEqualTo(start.plusDays(4))
        assertThat(s.bestDays).isEqualTo(3)
    }

    @Test
    fun `every 7 successful days earn a joker`() {
        assertThat(calc("++++++").jokers).isEqualTo(0)
        assertThat(calc("+++++++").jokers).isEqualTo(1)
        assertThat(calc("++++++++++++++").jokers).isEqualTo(2)
    }

    @Test
    fun `jokers are capped at three`() {
        assertThat(calc("+".repeat(35)).jokers).isEqualTo(3)
    }

    @Test
    fun `joker saves a missed day but the day does not count`() {
        val s = calc("+++++++-++")
        assertThat(s.currentDays).isEqualTo(9)
        assertThat(s.jokers).isEqualTo(0)
        assertThat(s.jokerDays).containsExactly(start.plusDays(7))
        assertThat(s.currentStart).isEqualTo(start)
    }

    @Test
    fun `losing the streak resets jokers`() {
        val s = calc("++++++++++++++--+")
        // 14 successes -> 2 jokers, both consumed by the two misses, streak survives.
        assertThat(s.currentDays).isEqualTo(15)
        assertThat(s.jokers).isEqualTo(0)
        val broken = calc("++++++++++++++---+")
        assertThat(broken.currentDays).isEqualTo(1)
        assertThat(broken.jokers).isEqualTo(0)
        assertThat(broken.bestDays).isEqualTo(14)
    }

    @Test
    fun `misses before any streak do not consume anything`() {
        val s = calc("--++")
        assertThat(s.currentDays).isEqualTo(2)
        assertThat(s.jokerDays).isEmpty()
    }

    @Test
    fun `no data yields empty streak`() {
        val s = StreakCalculator.calculate(firstDate = null, today = start) { false }
        assertThat(s.currentDays).isEqualTo(0)
        assertThat(s.currentStart).isNull()
    }
}
