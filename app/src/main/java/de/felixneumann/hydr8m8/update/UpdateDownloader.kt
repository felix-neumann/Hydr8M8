package de.felixneumann.hydr8m8.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Outcome of a finished download. */
sealed interface DownloadOutcome {
    data object Verified : DownloadOutcome

    data class Rejected(
        val reason: UpdateVerification.Result,
    ) : DownloadOutcome

    data class Failed(
        val reason: Int,
    ) : DownloadOutcome
}

/**
 * Downloads release APKs with the system [DownloadManager] into the public Downloads folder and
 * verifies them. Installing is left to the system (Files / Downloads app), so the app needs no
 * install permission (least privilege; also what Play Protect expects from non-store apps).
 */
class UpdateDownloader(
    private val context: Context,
) {
    private val dm = context.getSystemService(DownloadManager::class.java)
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    /** Downloads [release], reporting (downloaded, total) bytes, then verifies the file. */
    suspend fun download(
        release: ReleaseInfo,
        onProgress: (Long, Long) -> Unit,
    ): DownloadOutcome {
        removePrevious()
        val request =
            DownloadManager
                .Request(release.apkUrl.toUri())
                .setTitle("Hydr8M8 ${release.version}")
                .setMimeType(APK_MIME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Hydr8M8-${release.version}.apk")
        val id = dm.enqueue(request)
        prefs.edit { putLong(KEY_LAST_ID, id) }
        while (true) {
            val (status, done, total, reason) = query(id) ?: return DownloadOutcome.Failed(-1)
            onProgress(done, if (total > 0) total else release.apkSizeBytes)
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    return verify(id)
                }

                DownloadManager.STATUS_FAILED -> {
                    dm.remove(id)
                    return DownloadOutcome.Failed(reason)
                }
            }
            delay(POLL_MS)
        }
    }

    /** Opens the system downloads list, where tapping the APK starts the system installer. */
    fun openDownloads() {
        context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Deletes the last update file (called before a new download and once an update is installed). */
    fun removePrevious() {
        val id = prefs.getLong(KEY_LAST_ID, -1)
        if (id >= 0) dm.remove(id)
        prefs.edit { remove(KEY_LAST_ID) }
    }

    private data class Progress(
        val status: Int,
        val done: Long,
        val total: Long,
        val reason: Int,
    )

    private fun query(id: Long): Progress? =
        dm.query(DownloadManager.Query().setFilterById(id))?.use { c ->
            if (!c.moveToFirst()) return null
            Progress(
                status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
            )
        }

    /** Copies the download to private cache, checks package, signer and version, deletes it if rejected. */
    private suspend fun verify(id: Long): DownloadOutcome =
        withContext(Dispatchers.IO) {
            val copy = File(context.cacheDir, "verify-update.apk")
            try {
                dm.openDownloadedFile(id).use { pfd ->
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                        copy.outputStream().use { input.copyTo(it) }
                    }
                }
                val pm = context.packageManager
                val apk = pm.getPackageArchiveInfo(copy.path, PackageManager.GET_SIGNING_CERTIFICATES)
                val own = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val result =
                    UpdateVerification.verify(
                        expectedPackage = context.packageName,
                        installedVersionCode = own.longVersionCode,
                        ownSigners = own.signerFingerprints(),
                        apkPackage = apk?.packageName,
                        apkVersionCode = apk?.longVersionCode,
                        apkSigners = apk?.signerFingerprints().orEmpty(),
                    )
                if (result == UpdateVerification.Result.Ok) {
                    DownloadOutcome.Verified
                } else {
                    removePrevious()
                    DownloadOutcome.Rejected(result)
                }
            } finally {
                copy.delete()
            }
        }

    private fun PackageInfo.signerFingerprints(): Set<String> {
        val info = signingInfo ?: return emptySet()
        val certs = if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        return certs
            .orEmpty()
            .map { cert ->
                MessageDigest.getInstance("SHA-256").digest(cert.toByteArray()).joinToString("") { "%02X".format(it) }
            }
            .toSet()
    }

    private companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val KEY_LAST_ID = "last_download_id"
        const val POLL_MS = 250L
    }
}
