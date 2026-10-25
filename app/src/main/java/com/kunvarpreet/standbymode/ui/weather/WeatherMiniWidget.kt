package com.kunvarpreet.standbymode.ui.weather

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.R
import com.kunvarpreet.standbymode.core.weather.WeatherForecast
import com.kunvarpreet.standbymode.core.weather.WeatherLogic
import com.kunvarpreet.standbymode.core.weather.WeatherUiState
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * Compact ambient Weather & Forecast mini-widget for Duo Layout widget stacks.
 * Displays locality name, current temperature, condition icon & text,
 * daily high/low, precipitation probability, last-updated timestamp,
 * and Open-Meteo attribution.
 */
@Composable
fun WeatherMiniWidget(
    modifier: Modifier = Modifier,
    uiState: WeatherUiState,
    accentColor: Color,
    isNightMode: Boolean = false,
    onOpenPermissionSettings: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val surfaceColor = if (isNightMode) StandByNightSurface else StandBySurfaceCard
    val borderColor = if (isNightMode) StandByNightSurface else StandByBorder
    val primaryTextColor = if (isNightMode) StandByNightText else StandByTextPrimary
    val mutedTextColor = if (isNightMode) StandByNightTextMuted else StandByTextMuted
    val activeAccentColor = if (isNightMode) StandByNightText else accentColor

    Surface(
        modifier = modifier
            .widthIn(min = 260.dp, max = 380.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                contentDescription = when (uiState) {
                    is WeatherUiState.Loaded -> {
                        val f = uiState.forecast
                        "Weather widget: ${f.location.localityName}, ${WeatherLogic.formatTemperature(f.current.temperatureC)}, ${f.current.condition.description}, ${WeatherLogic.formatHighLow(f.today.maxTempC, f.today.minTempC)}"
                    }
                    is WeatherUiState.PermissionRequired -> "Location permission required for weather"
                    is WeatherUiState.LocationUnavailable -> "Location unavailable for weather"
                    is WeatherUiState.NoConnection -> "No internet connection for weather"
                    is WeatherUiState.Loading -> "Loading weather forecast"
                    is WeatherUiState.Error -> "Weather error: ${uiState.message}"
                }
            },
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState) {
                is WeatherUiState.Loading -> {
                    WeatherLoadingState(mutedTextColor = mutedTextColor)
                }
                is WeatherUiState.PermissionRequired -> {
                    WeatherPermissionRequiredState(
                        primaryTextColor = primaryTextColor,
                        mutedTextColor = mutedTextColor,
                        accentColor = activeAccentColor,
                        onOpenSettings = onOpenPermissionSettings
                    )
                }
                is WeatherUiState.LocationUnavailable -> {
                    WeatherLocationUnavailableState(
                        primaryTextColor = primaryTextColor,
                        mutedTextColor = mutedTextColor,
                        accentColor = activeAccentColor,
                        onRetry = onRetry
                    )
                }
                is WeatherUiState.NoConnection -> {
                    WeatherNoConnectionState(
                        message = uiState.message,
                        primaryTextColor = primaryTextColor,
                        mutedTextColor = mutedTextColor,
                        accentColor = activeAccentColor,
                        onRetry = onRetry
                    )
                }
                is WeatherUiState.Error -> {
                    WeatherErrorState(
                        message = uiState.message,
                        primaryTextColor = primaryTextColor,
                        mutedTextColor = mutedTextColor,
                        accentColor = activeAccentColor,
                        onRetry = onRetry
                    )
                }
                is WeatherUiState.Loaded -> {
                    WeatherLoadedContent(
                        forecast = uiState.forecast,
                        primaryTextColor = primaryTextColor,
                        mutedTextColor = mutedTextColor,
                        accentColor = activeAccentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherLoadedContent(
    forecast: WeatherForecast,
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Locality header
        Text(
            text = forecast.location.localityName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Main temperature & condition row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = forecast.current.condition.iconRes),
                contentDescription = forecast.current.condition.description,
                tint = accentColor,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = WeatherLogic.formatTemperature(forecast.current.temperatureC),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                letterSpacing = (-1).sp
            )
        }

        // Readable condition description
        Text(
            text = forecast.current.condition.description,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = mutedTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // High / Low & Precipitation probability
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = WeatherLogic.formatHighLow(
                    maxC = forecast.today.maxTempC,
                    minC = forecast.today.minTempC
                ),
                fontSize = 12.sp,
                color = primaryTextColor,
                fontWeight = FontWeight.Medium
            )

            if (forecast.today.precipitationProbabilityMax > 0) {
                Text(
                    text = "  •  ${WeatherLogic.formatPrecipitation(forecast.today.precipitationProbabilityMax)} precip",
                    fontSize = 12.sp,
                    color = mutedTextColor,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Footer: Compact last-updated + Open-Meteo attribution
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val updatedText = WeatherLogic.formatLastUpdated(forecast.lastUpdatedMillis)
            Text(
                text = updatedText,
                fontSize = 10.sp,
                color = mutedTextColor
            )

            Text(
                text = "Weather by Open-Meteo",
                fontSize = 10.sp,
                color = mutedTextColor
            )
        }
    }
}

@Composable
private fun WeatherLoadingState(mutedTextColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_weather_default),
            contentDescription = null,
            tint = mutedTextColor,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Loading weather…",
            fontSize = 13.sp,
            color = mutedTextColor
        )
    }
}

@Composable
private fun WeatherPermissionRequiredState(
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_weather_default),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Location Access Needed",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryTextColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Grant approximate location access to view local weather.",
            fontSize = 12.sp,
            color = mutedTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = onOpenSettings) {
            Text(
                text = "Grant Permission",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun WeatherLocationUnavailableState(
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_weather_default),
            contentDescription = null,
            tint = mutedTextColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Location Unavailable",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryTextColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Ensure device location is turned on to retrieve your local forecast.",
            fontSize = 12.sp,
            color = mutedTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = onRetry) {
            Text(
                text = "Retry",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun WeatherNoConnectionState(
    message: String,
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_weather_default),
            contentDescription = null,
            tint = mutedTextColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "No Connection",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryTextColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = mutedTextColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = onRetry) {
            Text(
                text = "Retry",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun WeatherErrorState(
    message: String,
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_weather_default),
            contentDescription = null,
            tint = mutedTextColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Weather Unavailable",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryTextColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = mutedTextColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = onRetry) {
            Text(
                text = "Retry",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}
