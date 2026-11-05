package de.felixneumann.hydr8m8.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppVersionTest {
    @Test
    fun `parses tags and version names`() {
        assertThat(AppVersion.parse("v0.9.0")).isEqualTo(AppVersion(0, 9, 0))
        assertThat(AppVersion.parse("1.2.3-debug")).isEqualTo(AppVersion(1, 2, 3))
        assertThat(AppVersion.parse("2.1")).isEqualTo(AppVersion(2, 1, 0))
        assertThat(AppVersion.parse("latest")).isNull()
    }

    @Test
    fun `compares numerically not lexically`() {
        assertThat(AppVersion(0, 10, 0)).isGreaterThan(AppVersion(0, 9, 9))
        assertThat(AppVersion(1, 0, 0)).isGreaterThan(AppVersion(0, 99, 99))
        assertThat(AppVersion(0, 9, 1)).isGreaterThan(AppVersion(0, 9, 0))
        assertThat(AppVersion(0, 9, 0)).isEquivalentAccordingToCompareTo(AppVersion(0, 9, 0))
    }
}
