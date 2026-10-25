package com.kunvarpreet.standbymode.core.weather

import com.kunvarpreet.standbymode.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class WeatherLogicTest {

    @Test
    fun mapWeatherCode_clearSky_distinguishesDayAndNight() {
        val dayCondition = WeatherLogic.mapWeatherCode(0, isDay = true)
        assertEquals("Clear sky", dayCondition.description)
        assertEquals(R.drawable.ic_weather_sunny, dayCondition.iconRes)

        val nightCondition = WeatherLogic.mapWeatherCode(0, isDay = false)
        assertEquals("Clear sky", nightCondition.description)
        assertEquals(R.drawable.ic_weather_night, nightCondition.iconRes)
    }

    @Test
    fun mapWeatherCode_mainlyClear_distinguishesDayAndNight() {
        val dayCondition = WeatherLogic.mapWeatherCode(1, isDay = true)
        assertEquals("Mainly clear", dayCondition.description)
        assertEquals(R.drawable.ic_weather_sunny, dayCondition.iconRes)

        val nightCondition = WeatherLogic.mapWeatherCode(1, isDay = false)
        assertEquals("Mainly clear", nightCondition.description)
        assertEquals(R.drawable.ic_weather_night, nightCondition.iconRes)
    }

    @Test
    fun mapWeatherCode_representativeConditions() {
        assertEquals("Partly cloudy", WeatherLogic.mapWeatherCode(2).description)
        assertEquals(R.drawable.ic_weather_partly_cloudy, WeatherLogic.mapWeatherCode(2).iconRes)

        assertEquals("Overcast", WeatherLogic.mapWeatherCode(3).description)
        assertEquals(R.drawable.ic_weather_cloudy, WeatherLogic.mapWeatherCode(3).iconRes)

        assertEquals("Fog", WeatherLogic.mapWeatherCode(45).description)
        assertEquals(R.drawable.ic_weather_fog, WeatherLogic.mapWeatherCode(45).iconRes)

        assertEquals("Light drizzle", WeatherLogic.mapWeatherCode(51).description)
        assertEquals(R.drawable.ic_weather_rain, WeatherLogic.mapWeatherCode(51).iconRes)

        assertEquals("Slight rain", WeatherLogic.mapWeatherCode(61).description)
        assertEquals("Moderate rain", WeatherLogic.mapWeatherCode(63).description)
        assertEquals("Heavy rain", WeatherLogic.mapWeatherCode(65).description)
        assertEquals(R.drawable.ic_weather_rain, WeatherLogic.mapWeatherCode(61).iconRes)

        assertEquals("Slight snow", WeatherLogic.mapWeatherCode(71).description)
        assertEquals("Heavy snow", WeatherLogic.mapWeatherCode(75).description)
        assertEquals(R.drawable.ic_weather_snow, WeatherLogic.mapWeatherCode(71).iconRes)

        assertEquals("Rain showers", WeatherLogic.mapWeatherCode(80).description)
        assertEquals(R.drawable.ic_weather_rain, WeatherLogic.mapWeatherCode(80).iconRes)

        assertEquals("Thunderstorm", WeatherLogic.mapWeatherCode(95).description)
        assertEquals(R.drawable.ic_weather_thunderstorm, WeatherLogic.mapWeatherCode(95).iconRes)

        assertEquals("Thunderstorm with hail", WeatherLogic.mapWeatherCode(99).description)
        assertEquals(R.drawable.ic_weather_thunderstorm, WeatherLogic.mapWeatherCode(99).iconRes)

        // Unknown code fallback
        val unknown = WeatherLogic.mapWeatherCode(999)
        assertEquals("Weather", unknown.description)
        assertEquals(R.drawable.ic_weather_default, unknown.iconRes)
    }

    @Test
    fun parseForecastJson_validOpenMeteoPayload() {
        val json = """
            {
              "latitude": 28.625,
              "longitude": 77.25,
              "current": {
                "time": "2026-10-24T23:45",
                "interval": 900,
                "temperature_2m": 22.8,
                "weather_code": 3,
                "is_day": 0
              },
              "daily": {
                "time": ["2026-10-24"],
                "temperature_2m_max": [31.5],
                "temperature_2m_min": [21.7],
                "precipitation_probability_max": [15]
              }
            }
        """.trimIndent()

        val parsed = WeatherLogic.parseForecastJson(json)
        assertEquals(22.8, parsed.temperatureC, 0.001)
        assertEquals(3, parsed.weatherCode)
        assertFalse(parsed.isDay)
        assertEquals(31.5, parsed.maxTempC, 0.001)
        assertEquals(21.7, parsed.minTempC, 0.001)
        assertEquals(15, parsed.precipitationProbabilityMax)
    }

    @Test
    fun parseForecastJson_negativeTemperaturesAndZeroPrecipitation() {
        val json = """
            {
              "current": {
                "temperature_2m": -5.2,
                "weather_code": 71,
                "is_day": 1
              },
              "daily": {
                "temperature_2m_max": [-2.0],
                "temperature_2m_min": [-10.4],
                "precipitation_probability_max": [0]
              }
            }
        """.trimIndent()

        val parsed = WeatherLogic.parseForecastJson(json)
        assertEquals(-5.2, parsed.temperatureC, 0.001)
        assertEquals(71, parsed.weatherCode)
        assertTrue(parsed.isDay)
        assertEquals(-2.0, parsed.maxTempC, 0.001)
        assertEquals(-10.4, parsed.minTempC, 0.001)
        assertEquals(0, parsed.precipitationProbabilityMax)
    }

    @Test
    fun parseForecastJson_missingCurrent_throwsIllegalArgumentException() {
        val json = """{"daily": {"temperature_2m_max": [25.0]}}"""
        assertThrows(IllegalArgumentException::class.java) {
            WeatherLogic.parseForecastJson(json)
        }
    }

    @Test
    fun parseForecastJson_missingDaily_throwsIllegalArgumentException() {
        val json = """{"current": {"temperature_2m": 20.0, "weather_code": 0}}"""
        assertThrows(IllegalArgumentException::class.java) {
            WeatherLogic.parseForecastJson(json)
        }
    }

    @Test
    fun parseForecastJson_temperatureOutOfRange_throwsIllegalArgumentException() {
        val json = """
            {
              "current": {
                "temperature_2m": 120.0,
                "weather_code": 0,
                "is_day": 1
              },
              "daily": {
                "temperature_2m_max": [125.0],
                "temperature_2m_min": [115.0]
              }
            }
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            WeatherLogic.parseForecastJson(json)
        }
    }

    @Test
    fun parseForecastJson_malformedJson_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            WeatherLogic.parseForecastJson("{ invalid json text ")
        }
    }

    @Test
    fun formatTemperature_roundsCorrectly() {
        assertEquals("23°", WeatherLogic.formatTemperature(22.8))
        assertEquals("22°", WeatherLogic.formatTemperature(22.2))
        assertEquals("-5°", WeatherLogic.formatTemperature(-5.4))
        assertEquals("-6°", WeatherLogic.formatTemperature(-5.6))
        assertEquals("0°", WeatherLogic.formatTemperature(0.1))
    }

    @Test
    fun formatHighLow_formatsCorrectly() {
        assertEquals("H:32°  L:22°", WeatherLogic.formatHighLow(31.5, 21.7))
        assertEquals("H:-2°  L:-10°", WeatherLogic.formatHighLow(-2.1, -10.4))
    }

    @Test
    fun formatPrecipitation_clampsAndFormats() {
        assertEquals("35%", WeatherLogic.formatPrecipitation(35))
        assertEquals("0%", WeatherLogic.formatPrecipitation(0))
        assertEquals("100%", WeatherLogic.formatPrecipitation(100))
        assertEquals("0%", WeatherLogic.formatPrecipitation(-5))
        assertEquals("100%", WeatherLogic.formatPrecipitation(120))
    }

    @Test
    fun formatLastUpdated_formatsWithZone() {
        val zone = ZoneId.of("UTC")
        // Epoch 1700000000000 = 2023-11-14T22:13:20Z
        val formatted = WeatherLogic.formatLastUpdated(1700000000000L, zone)
        assertEquals("Updated 22:13", formatted)

        assertEquals("", WeatherLogic.formatLastUpdated(0L, zone))
        assertEquals("", WeatherLogic.formatLastUpdated(-1L, zone))
    }

    @Test
    fun isCacheFresh_evaluatesFreshnessCorrectly() {
        val now = 100_000_000L

        // Fresh within 20 minutes (20 * 60 * 1000 = 1,200,000ms)
        assertTrue(WeatherLogic.isCacheFresh(now - 60_000L, now)) // 1 min ago
        assertTrue(WeatherLogic.isCacheFresh(now - 1_199_000L, now)) // 19.9 min ago

        // Stale after 20 minutes
        assertFalse(WeatherLogic.isCacheFresh(now - 1_200_001L, now)) // 20.01 min ago
        assertFalse(WeatherLogic.isCacheFresh(now - 3_600_000L, now)) // 1 hour ago

        // Invalid timestamps
        assertFalse(WeatherLogic.isCacheFresh(0L, now))
        assertFalse(WeatherLogic.isCacheFresh(-1L, now))
        assertFalse(WeatherLogic.isCacheFresh(now + 10_000L, now)) // future timestamp
    }
}
