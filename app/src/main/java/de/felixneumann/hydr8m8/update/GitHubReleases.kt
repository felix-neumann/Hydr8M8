package de.felixneumann.hydr8m8.update

import de.felixneumann.hydr8m8.domain.AppVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.HttpURLConnection
import java.net.URL

/** The newest published release and its APK asset. */
data class ReleaseInfo(
    val version: AppVersion,
    val tag: String,
    val notes: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
)

/** Reads the latest release of a public GitHub repository (no token needed). */
class GitHubReleases(
    private val repo: String,
) {
    suspend fun latest(): ReleaseInfo =
        withContext(Dispatchers.IO) {
            val conn = URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "Hydr8M8-Android")
            try {
                check(conn.responseCode == HttpURLConnection.HTTP_OK) { "GitHub HTTP ${conn.responseCode}" }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                checkNotNull(parseRelease(body)) { "No APK in latest release" }
            } finally {
                conn.disconnect()
            }
        }

    companion object {
        private const val TIMEOUT_MS = 15_000

        /** Extracts version and the first `.apk` asset from a GitHub "release" JSON document. */
        fun parseRelease(json: String): ReleaseInfo? {
            val root = Json.parseToJsonElement(json).jsonObject
            val tag = root.string("tag_name") ?: return null
            val version = AppVersion.parse(tag) ?: return null
            val apk =
                root["assets"]
                    ?.jsonArray
                    ?.map { it.jsonObject }
                    ?.firstOrNull { it.string("name")?.endsWith(".apk") == true }
                    ?: return null
            return ReleaseInfo(
                version = version,
                tag = tag,
                notes = root.string("body").orEmpty(),
                apkUrl = apk.string("browser_download_url") ?: return null,
                apkSizeBytes = apk["size"]?.jsonPrimitive?.longOrNull ?: 0L,
            )
        }

        private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    }
}
