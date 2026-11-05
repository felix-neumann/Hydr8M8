package de.felixneumann.hydr8m8.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.felixneumann.hydr8m8.container
import de.felixneumann.hydr8m8.data.HydrationRepository
import de.felixneumann.hydr8m8.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale
import kotlin.coroutines.resume

private val Context.locationStore by preferencesDataStore(name = "weather_location")

/**
 * Daily weather bonus. Uses approximate location only. The location is captured while the app is
 * in the foreground (no background-location permission needed) and cached for background runs.
 * The temperature comes from Open-Meteo (free, no API key).
 */
class WeatherService(
    private val context: Context,
    private val repository: HydrationRepository,
    private val settings: SettingsRepository,
) {
    private val latKey = doublePreferencesKey("lat")
    private val lonKey = doublePreferencesKey("lon")

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Called while the app is visible: refresh the cached approximate location. */
    suspend fun captureLocation() {
        if (!hasPermission()) return
        val location = currentLocation() ?: return
        context.locationStore.edit {
            it[latKey] = location.latitude
            it[lonKey] = location.longitude
        }
    }

    /** Enqueues today's weather fetch once per logical day, if enabled. */
    suspend fun fetchIfNeeded(today: LocalDate) {
        val s = settings.current()
        if (!s.weather.enabled || !hasPermission()) return
        if (repository.weatherFor(today) != null) return
        val request =
            OneTimeWorkRequestBuilder<WeatherWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork("weather-$today", ExistingWorkPolicy.KEEP, request)
    }

    suspend fun fetchNow(): Boolean {
        val prefs = context.locationStore.data.first()
        val lat = prefs[latKey] ?: return false
        val lon = prefs[lonKey] ?: return false
        val maxTemp = fetchMaxTemperature(lat, lon) ?: return false
        val today = repository.dayClock().logicalDate(repository.now())
        repository.saveWeather(today, maxTemp, settings.current().weather.bonusFor(maxTemp))
        return true
    }

    @Suppress("MissingPermission") // checked in hasPermission()
    private suspend fun currentLocation(): Location? {
        val lm = context.getSystemService(LocationManager::class.java)
        val provider =
            listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .firstOrNull { lm.isProviderEnabled(it) } ?: return null
        val fresh =
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    lm.getCurrentLocation(provider, null, context.mainExecutor) { cont.resume(it) }
                }
            }
        return fresh ?: lm.getLastKnownLocation(provider)
    }

    private suspend fun fetchMaxTemperature(
        lat: Double,
        lon: Double,
    ): Double? =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    URL(
                        String.format(
                            Locale.ROOT,
                            "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f" +
                                "&daily=temperature_2m_max&timezone=auto&forecast_days=1",
                            lat,
                            lon,
                        ),
                    )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = HTTP_TIMEOUT_MS
                conn.readTimeout = HTTP_TIMEOUT_MS
                try {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    Json
                        .parseToJsonElement(body)
                        .jsonObject["daily"]!!
                        .jsonObject["temperature_2m_max"]!!
                        .jsonArray[0]
                        .jsonPrimitive.double
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 10_000L
        const val HTTP_TIMEOUT_MS = 10_000
    }
}

class WeatherWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        return when {
            c.weather.fetchNow() -> {
                c.actions.refreshAll()
                Result.success()
            }

            runAttemptCount < MAX_ATTEMPTS -> {
                Result.retry()
            }

            else -> {
                Result.failure()
            }
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
