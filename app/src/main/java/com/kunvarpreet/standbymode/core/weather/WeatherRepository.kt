package com.kunvarpreet.standbymode.core.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * Repository responsible for:
 * 1. Safely checking ACCESS_COARSE_LOCATION permission.
 * 2. Acquiring one-shot approximate device location with cancellation/timeout.
 * 3. Resolving concise locality name via Geocoder.
 * 4. Fetching forecast from Open-Meteo HTTPS endpoint.
 * 5. Maintaining an in-memory session cache and lifecycle-safe execution.
 */
class WeatherRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val tag = "WeatherRepository"

    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private var activeJob: Job? = null
    private val requestCounter = AtomicLong(0)

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var cachedForecast: WeatherForecast? = null
    private var isObserving = false

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Called when the weather widget becomes actively visible and enabled.
     */
    fun startObserving() {
        if (isObserving) return
        isObserving = true

        val cached = cachedForecast
        val now = System.currentTimeMillis()
        if (cached != null && WeatherLogic.isCacheFresh(cached.lastUpdatedMillis, now)) {
            _uiState.value = WeatherUiState.Loaded(cached)
            return
        }

        refresh()
    }

    /**
     * Called when the weather widget is hidden, screen changed, or app backgrounded.
     */
    fun stopObserving() {
        if (!isObserving) return
        isObserving = false
        activeJob?.cancel()
        activeJob = null
    }

    /**
     * Triggers a fresh location retrieval and forecast query.
     */
    fun refresh() {
        if (!hasLocationPermission()) {
            _uiState.value = WeatherUiState.PermissionRequired()
            return
        }

        val cached = cachedForecast
        val now = System.currentTimeMillis()
        if (cached != null && WeatherLogic.isCacheFresh(cached.lastUpdatedMillis, now) && _uiState.value is WeatherUiState.Loaded) {
            return
        }

        activeJob?.cancel()
        val currentRequestId = requestCounter.incrementAndGet()

        activeJob = scope.launch {
            if (_uiState.value !is WeatherUiState.Loaded) {
                _uiState.value = WeatherUiState.Loading
            }

            try {
                val location = withContext(ioDispatcher) {
                    acquireApproximateLocation()
                }

                if (location == null) {
                    if (currentRequestId == requestCounter.get()) {
                        _uiState.value = WeatherUiState.LocationUnavailable
                    }
                    return@launch
                }

                val localityName = withContext(ioDispatcher) {
                    resolveLocality(location.latitude, location.longitude)
                }

                val forecast = withContext(ioDispatcher) {
                    fetchForecast(location.latitude, location.longitude, localityName)
                }

                if (currentRequestId == requestCounter.get()) {
                    cachedForecast = forecast
                    _uiState.value = WeatherUiState.Loaded(forecast)
                }
            } catch (e: UnknownHostException) {
                Log.w(tag, "No network connection for weather forecast", e)
                if (currentRequestId == requestCounter.get()) {
                    _uiState.value = WeatherUiState.NoConnection()
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to load weather forecast", e)
                if (currentRequestId == requestCounter.get()) {
                    val message = e.localizedMessage ?: "Failed to load weather data"
                    _uiState.value = WeatherUiState.Error(message)
                }
            }
        }
    }

    /**
     * One-shot approximate location acquisition using LocationManager with a timeout.
     */
    private suspend fun acquireApproximateLocation(): Location? {
        if (!hasLocationPermission()) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        // 1. Try last known location first (fast and battery-efficient for approximate weather)
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER
        )
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val lastKnown = locationManager.getLastKnownLocation(provider)
                    if (lastKnown != null) {
                        return lastKnown
                    }
                }
            } catch (e: SecurityException) {
                Log.w(tag, "SecurityException reading last known location", e)
                return null
            } catch (e: Exception) {
                Log.w(tag, "Exception reading last known location for $provider", e)
            }
        }

        // 2. Request a one-shot current location with a 10s timeout
        return withTimeoutOrNull(10_000L.milliseconds) {
            suspendCancellableCoroutine { continuation ->
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation {
                    cancellationSignal.cancel()
                }

                val directExecutor = Executor { it.run() }
                try {
                    locationManager.getCurrentLocation(
                        LocationManager.NETWORK_PROVIDER,
                        cancellationSignal,
                        directExecutor
                    ) { location ->
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                } catch (e: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                } catch (e: Exception) {
                    // Network provider might not be available; try passive or return null
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    /**
     * Resolves concise locality name from coordinates using platform Geocoder.
     * Fallbacks safely to "Local weather". Never exposes raw latitude/longitude.
     */
    private suspend fun resolveLocality(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            withTimeoutOrNull(5000L.milliseconds) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val addr = addresses.firstOrNull()
                            val locality = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: "Local weather"
                            if (continuation.isActive) {
                                continuation.resume(locality)
                            }
                        }
                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume("Local weather")
                            }
                        }
                    })
                }
            } ?: "Local weather"
        } catch (e: Exception) {
            Log.w(tag, "Geocoder lookup failed, using fallback locality", e)
            "Local weather"
        }
    }

    /**
     * Executes HTTPS GET to Open-Meteo Forecast API and parses response into [WeatherForecast].
     */
    private fun fetchForecast(latitude: Double, longitude: Double, localityName: String): WeatherForecast {
        val urlString = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,weather_code,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto"

        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "StandByMode-Android")

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("Forecast provider returned HTTP $responseCode")
            }

            val jsonText = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }

            val parsed = WeatherLogic.parseForecastJson(jsonText)
            val condition = WeatherLogic.mapWeatherCode(parsed.weatherCode, parsed.isDay)

            val nowMillis = System.currentTimeMillis()
            return WeatherForecast(
                location = WeatherLocation(
                    latitude = latitude,
                    longitude = longitude,
                    localityName = localityName
                ),
                current = WeatherCurrent(
                    temperatureC = parsed.temperatureC,
                    weatherCode = parsed.weatherCode,
                    isDay = parsed.isDay,
                    condition = condition
                ),
                today = WeatherDaily(
                    maxTempC = parsed.maxTempC,
                    minTempC = parsed.minTempC,
                    precipitationProbabilityMax = parsed.precipitationProbabilityMax
                ),
                lastUpdatedMillis = nowMillis
            )
        } finally {
            connection.disconnect()
        }
    }
}
