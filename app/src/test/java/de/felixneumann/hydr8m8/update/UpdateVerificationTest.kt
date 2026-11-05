package de.felixneumann.hydr8m8.update

import com.google.common.truth.Truth.assertThat
import de.felixneumann.hydr8m8.update.UpdateVerification.Result
import org.junit.Test

class UpdateVerificationTest {
    private val repo = "felix-neumann/Hydr8M8"
    private val pkg = "de.felixneumann.hydr8m8"
    private val ours = setOf("AA11")

    @Test
    fun `only assets of our own repository are trusted`() {
        assertThat(
            UpdateVerification.isTrustedAssetUrl(
                "https://github.com/felix-neumann/Hydr8M8/releases/download/v1.0.0/Hydr8M8-1.0.0.apk",
                repo,
            ),
        ).isTrue()
        assertThat(
            UpdateVerification.isTrustedAssetUrl(
                "https://github.com/someone/Hydr8M8/releases/download/v1.0.0/x.apk",
                repo,
            ),
        ).isFalse()
        assertThat(
            UpdateVerification.isTrustedAssetUrl(
                "http://github.com/felix-neumann/Hydr8M8/releases/download/v1/x.apk",
                repo,
            ),
        )
            .isFalse()
        assertThat(
            UpdateVerification.isTrustedAssetUrl(
                "https://github.com/felix-neumann/Hydr8M8/releases/download/../../evil/x.apk",
                repo,
            ),
        ).isFalse()
        assertThat(
            UpdateVerification.isTrustedAssetUrl("https://evil.example/felix-neumann/Hydr8M8/x.apk", repo),
        ).isFalse()
    }

    private fun verify(
        apkPackage: String? = pkg,
        apkVersion: Long? = 5,
        apkSigners: Set<String> = ours,
        ownSigners: Set<String> = ours,
    ) = UpdateVerification.verify(pkg, 4, ownSigners, apkPackage, apkVersion, apkSigners)

    @Test
    fun `accepts a newer apk signed with our key`() {
        assertThat(verify()).isEqualTo(Result.Ok)
    }

    @Test
    fun `rejects foreign signer, other package, old version and unreadable files`() {
        assertThat(verify(apkSigners = setOf("BB22"))).isEqualTo(Result.WrongSigner)
        assertThat(verify(apkSigners = ours + "BB22")).isEqualTo(Result.WrongSigner)
        assertThat(verify(ownSigners = emptySet(), apkSigners = emptySet())).isEqualTo(Result.WrongSigner)
        assertThat(verify(apkPackage = "com.evil")).isEqualTo(Result.WrongPackage)
        assertThat(verify(apkVersion = 4)).isEqualTo(Result.NotNewer)
        assertThat(verify(apkPackage = null)).isEqualTo(Result.Unreadable)
    }
}
