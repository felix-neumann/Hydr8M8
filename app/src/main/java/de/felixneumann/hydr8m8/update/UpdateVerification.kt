package de.felixneumann.hydr8m8.update

/**
 * Pure security checks for downloaded updates. Android itself refuses updates signed with another
 * key; these checks run earlier so a tampered or foreign file is rejected and deleted before the
 * user is ever asked to install it.
 */
object UpdateVerification {
    private const val GITHUB_DOWNLOAD_PREFIX = "https://github.com/%s/releases/download/"

    /** Only release assets of our own repository are downloaded. */
    fun isTrustedAssetUrl(
        url: String,
        repo: String,
    ): Boolean {
        val prefix = GITHUB_DOWNLOAD_PREFIX.format(repo)
        return url.startsWith(prefix) && !url.contains("..") && url.endsWith(".apk")
    }

    sealed interface Result {
        data object Ok : Result

        data object WrongPackage : Result

        data object WrongSigner : Result

        data object NotNewer : Result

        data object Unreadable : Result
    }

    /**
     * @param ownSigners SHA-256 fingerprints of the installed app's signing certificates.
     * @param apkSigners SHA-256 fingerprints of the downloaded APK's signing certificates.
     */
    fun verify(
        expectedPackage: String,
        installedVersionCode: Long,
        ownSigners: Set<String>,
        apkPackage: String?,
        apkVersionCode: Long?,
        apkSigners: Set<String>,
    ): Result =
        when {
            apkPackage == null || apkVersionCode == null -> Result.Unreadable
            apkPackage != expectedPackage -> Result.WrongPackage
            ownSigners.isEmpty() || apkSigners != ownSigners -> Result.WrongSigner
            apkVersionCode <= installedVersionCode -> Result.NotNewer
            else -> Result.Ok
        }
}
