package com.kunvarpreet.standbymode.core.weather

import androidx.annotation.DrawableRes

/**
 * Weather condition category with readable description and vector icon resource.
 */
data class WeatherCondition(
    val code: Int,
    val description: String,
    @DrawableRes val iconRes: Int
)

/**
 * Geographical location details for the forecast.
 */
data class WeatherLocation(
    val latitude: Double,
    val longitude: Double,
    val localityName: String
)

/**
 * Current conditions parsed from the weather provider.
 */
data class WeatherCurrent(
    val temperatureC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val condition: WeatherCondition
)

/**
 * Today's daily forecast summary.
 */
data class WeatherDaily(
    val maxTempC: Double,
    val minTempC: Double,
    val precipitationProbabilityMax: Int
)

/**
 * Complete immutable weather forecast model for presentation.
 */
data class WeatherForecast(
    val location: WeatherLocation,
    val current: WeatherCurrent,
    val today: WeatherDaily,
    val lastUpdatedMillis: Long
)

/**
 * Distinct presentation states for WeatherMiniWidget.
 */
sealed interface WeatherUiState {
    data object Loading : WeatherUiState

    data class PermissionRequired(
        val isPermanentlyDenied: Boolean = false
    ) : WeatherUiState

    data object LocationUnavailable : WeatherUiState

    data class NoConnection(
        val message: String = "No internet connection"
    ) : WeatherUiState

    data class Loaded(
        val forecast: WeatherForecast
    ) : WeatherUiState

    data class Error(
        val message: String
    ) : WeatherUiState
}
