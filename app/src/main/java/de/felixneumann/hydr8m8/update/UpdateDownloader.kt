package de.felixneumann.hydr8m8.update

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Outcome of a finished download. */
sealed interface DownloadOutcome {
    data object Verified : DownloadOutcome

    data class Rejected(
        val reason: UpdateVerification.Result,
    ) : DownloadOutcome

    data object Failed : DownloadOutcome
}

/**
 * Downloads release APKs itself (progress), verifies them in private storage and only then
 * publishes them to the Download folder, from where the user installs them with the system file
 * manager.
 *
 * Why not DownloadManager or an in-app install: Android attributes such files to this app and
 * would then require `REQUEST_INSTALL_PACKAGES`, which made Play Protect rate the app as
 * "potentially unwanted" (see security rules). Files opened from the file manager are installed
 * on behalf of the file manager instead.
 */
class UpdateDownloader(
    private val context: Context,
) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    /** Downloads [release] reporting (downloaded, total) bytes, verifies and publishes it. */
    suspend fun download(
        release: ReleaseInfo,
        onProgress: (Long, Long) -> Unit,
    ): DownloadOutcome =
        withContext(Dispatchers.IO) {
            removePrevious()
            val temp = File(context.cacheDir, "update-download.apk")
            try {
                if (!fetch(release, temp, onProgress)) return@withContext DownloadOutcome.Failed
                val result = verify(temp)
                if (result != UpdateVerification.Result.Ok) return@withContext DownloadOutcome.Rejected(result)
                publish(temp, "Hydr8M8-${release.version}.apk")
                DownloadOutcome.Verified
            } finally {
                temp.delete()
            }
        }

    /** Opens the system file manager in the Download folder, where tapping the APK installs it. */
    fun openFolder() {
        val folder =
            DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, "primary:$RELATIVE_DIR")
        val view =
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(folder, DocumentsContract.Document.MIME_TYPE_DIR)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(view)
        } catch (_: ActivityNotFoundException) {
            // Some file managers cannot open a folder directly; open the app and let the dialog
            // text tell the user where the file is.
            context.startActivity(
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_FILES)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** Deletes the last published update file (before a new download and once it is installed). */
    fun removePrevious() {
        prefs.getString(KEY_LAST_URI, null)?.let { uri ->
            runCatching { context.contentResolver.delete(uri.toUri(), null, null) }
        }
        prefs.edit { remove(KEY_LAST_URI) }
    }

    private suspend fun fetch(
        release: ReleaseInfo,
        target: File,
        onProgress: (Long, Long) -> Unit,
    ): Boolean {
        val conn = URL(release.apkUrl).openConnection() as HttpURLConnection
        conn.instanceFollowRedirects = true
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return false
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSizeBytes
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    var done = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        onProgress(done, total)
                    }
                }
            }
            true
        } catch (_: java.io.IOException) {
            false
        } finally {
            conn.disconnect()
        }
    }

    private fun verify(apkFile: File): UpdateVerification.Result {
        val pm = context.packageManager
        val apk = pm.getPackageArchiveInfo(apkFile.path, PackageManager.GET_SIGNING_CERTIFICATES)
        val own = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        return UpdateVerification.verify(
            expectedPackage = context.packageName,
            installedVersionCode = own.longVersionCode,
            ownSigners = own.signerFingerprints(),
            apkPackage = apk?.packageName,
            apkVersionCode = apk?.longVersionCode,
            apkSigners = apk?.signerFingerprints().orEmpty(),
        )
    }

    /** Copies the verified APK to the Download folder via MediaStore (no storage permission needed). */
    private fun publish(
        source: File,
        displayName: String,
    ) {
        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, APK_MIME)
                put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DIR)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        val uri: Uri =
            checkNotNull(
                resolver.insert(MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values),
            )
        prefs.edit { putString(KEY_LAST_URI, uri.toString()) }
        resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
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

    companion object {
        /** Shown to the user, so it uses the folder names of the file manager. */
        const val RELATIVE_DIR = "Download"
        private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
        private const val APK_MIME = "application/vnd.android.package-archive"
        private const val KEY_LAST_URI = "last_update_uri"
        private const val TIMEOUT_MS = 30_000
        private const val BUFFER_BYTES = 64 * 1024
    }
}
