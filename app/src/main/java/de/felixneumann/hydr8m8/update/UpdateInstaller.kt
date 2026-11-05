package de.felixneumann.hydr8m8.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.provider.Settings
import androidx.core.net.toUri
import de.felixneumann.hydr8m8.container
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/** Downloads an APK and hands it to the system [PackageInstaller]. */
class UpdateInstaller(
    private val context: Context,
) {
    /** Whether the user allowed this app to install packages ("install unknown apps"). */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** Downloads [url] into the cache, reporting progress as (downloaded, total) bytes. */
    suspend fun download(
        url: String,
        expectedBytes: Long,
        onProgress: (Long, Long) -> Unit,
    ): File =
        withContext(Dispatchers.IO) {
            val target = File(context.cacheDir, "updates/update.apk").apply { parentFile?.mkdirs() }
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            try {
                check(conn.responseCode == HttpURLConnection.HTTP_OK) { "Download HTTP ${conn.responseCode}" }
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: expectedBytes
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
                target
            } finally {
                conn.disconnect()
            }
        }

    /** Starts the install session; the system then asks the user to confirm (see [UpdateInstallReceiver]). */
    suspend fun install(apk: File) =
        withContext(Dispatchers.IO) {
            val installer = context.packageManager.packageInstaller
            val params =
                PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(context.packageName)
                }
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("update.apk", 0, apk.length()).use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                val callback =
                    PendingIntent.getBroadcast(
                        context,
                        sessionId,
                        Intent(context, UpdateInstallReceiver::class.java),
                        // Mutable: the installer adds status extras to this explicit intent.
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                session.commit(callback.intentSender)
            }
        }

    private companion object {
        const val TIMEOUT_MS = 30_000
        const val BUFFER_BYTES = 64 * 1024
    }
}

/** Receives install session results; launches the system confirmation dialog when required. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val updates = context.container.updates
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)?.let {
                    context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Unit
            }

            // The app process is replaced by the new version.
            else -> {
                updates.onInstallFailed(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
            }
        }
    }
}
