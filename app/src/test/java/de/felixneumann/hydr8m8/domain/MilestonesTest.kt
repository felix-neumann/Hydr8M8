package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class MilestonesTest {
    private val start = LocalDate.of(2026, 1, 15)

    private fun milestone(
        days: Int,
        lastCovered: LocalDate,
    ) = Milestones.highestReached(streakStart = start, streakDays = days, lastCoveredDay = lastCovered)

    @Test
    fun `nothing before one week`() {
        assertThat(milestone(6, start.plusDays(5))).isNull()
    }

    @Test
    fun `week milestone after 7 days`() {
        assertThat(milestone(7, start.plusDays(6))?.type).isEqualTo(MilestoneType.WEEK)
    }

    @Test
    fun `month milestone after one full calendar month`() {
        // 15 Jan .. 14 Feb inclusive = 1 full month
        val m = milestone(31, LocalDate.of(2026, 2, 14))
        assertThat(m?.type).isEqualTo(MilestoneType.MONTH)
        assertThat(m?.months).isEqualTo(1)
        assertThat(milestone(30, LocalDate.of(2026, 2, 13))?.type).isEqualTo(MilestoneType.WEEK)
    }

    @Test
    fun `half year and year replace month milestones`() {
        assertThat(milestone(181, LocalDate.of(2026, 7, 14))?.type).isEqualTo(MilestoneType.HALF_YEAR)
        assertThat(milestone(210, LocalDate.of(2026, 8, 14))?.type).isEqualTo(MilestoneType.MONTH)
        assertThat(milestone(365, LocalDate.of(2027, 1, 14))?.type).isEqualTo(MilestoneType.YEAR)
        assertThat(milestone(546, LocalDate.of(2027, 7, 14))?.type).isEqualTo(MilestoneType.HALF_YEAR)
        assertThat(milestone(730, LocalDate.of(2028, 1, 14))?.type).isEqualTo(MilestoneType.YEAR)
    }

    @Test
    fun `key differs per streak start and step`() {
        val a = milestone(7, start.plusDays(6))!!
        val b = milestone(31, LocalDate.of(2026, 2, 14))!!
        assertThat(a.key).isNotEqualTo(b.key)
    }
}
