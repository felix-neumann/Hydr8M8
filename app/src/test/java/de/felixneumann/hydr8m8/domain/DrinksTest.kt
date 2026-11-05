package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DrinksTest {
    @Test
    fun `volumes snap to 10 ml and respect per-kind limits`() {
        assertThat(DrinkLimits.snapVolume(254f, DrinkKind.WATER)).isEqualTo(250)
        assertThat(DrinkLimits.snapVolume(256f, DrinkKind.WATER)).isEqualTo(260)
        assertThat(DrinkLimits.snapVolume(0f, DrinkKind.WATER)).isEqualTo(10)
        assertThat(DrinkLimits.snapVolume(5000f, DrinkKind.WATER)).isEqualTo(2000)
        assertThat(DrinkLimits.snapVolume(5000f, DrinkKind.ALCOHOL)).isEqualTo(1000)
    }

    @Test
    fun `abv snaps to half percent within range`() {
        assertThat(DrinkLimits.snapAbv(5.2)).isEqualTo(5.0)
        assertThat(DrinkLimits.snapAbv(12.3)).isEqualTo(12.5)
        assertThat(DrinkLimits.snapAbv(0.0)).isEqualTo(0.5)
        assertThat(DrinkLimits.snapAbv(99.0)).isEqualTo(70.0)
    }

    @Test
    fun `every icon has a built-in template with matching kind`() {
        DrinkIcon.entries.forEach { icon ->
            val template = BuiltInPresets.templateFor(icon)
            assertThat(template).isNotNull()
            assertThat(template!!.icon.kind).isEqualTo(icon.kind)
            assertThat(template.abvPercent != null).isEqualTo(icon.kind == DrinkKind.ALCOHOL)
        }
    }

    @Test
    fun `alcohol does not count as water`() {
        val beer =
            Entry(1, java.time.Instant.EPOCH, DrinkKind.ALCOHOL, 500, 5.0, DrinkIcon.BEER, java.time.Instant.EPOCH)
        assertThat(beer.waterMl).isEqualTo(0)
        assertThat(beer.alcoholGoalBonusMl).isEqualTo(200)
    }
}
