package de.felixneumann.hydr8m8.update

import com.google.common.truth.Truth.assertThat
import de.felixneumann.hydr8m8.domain.AppVersion
import org.junit.Test

class GitHubReleasesTest {
    private val sample =
        """
        {
          "tag_name": "v0.10.0",
          "body": "Notes",
          "assets": [
            {"name": "checksums.txt", "size": 10, "browser_download_url": "https://x/checksums.txt"},
            {"name": "Hydr8M8-0.10.0.apk", "size": 4557217, "browser_download_url": "https://x/Hydr8M8-0.10.0.apk"}
          ]
        }
        """.trimIndent()

    @Test
    fun `parses version and apk asset`() {
        val release = GitHubReleases.parseRelease(sample)!!
        assertThat(release.version).isEqualTo(AppVersion(0, 10, 0))
        assertThat(release.apkUrl).isEqualTo("https://x/Hydr8M8-0.10.0.apk")
        assertThat(release.apkSizeBytes).isEqualTo(4557217)
        assertThat(release.notes).isEqualTo("Notes")
    }

    @Test
    fun `release without apk yields null`() {
        assertThat(GitHubReleases.parseRelease("""{"tag_name":"v1.0.0","assets":[]}""")).isNull()
    }
}
