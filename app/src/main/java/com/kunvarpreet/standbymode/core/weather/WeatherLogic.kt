package com.kunvarpreet.standbymode.core.weather

import com.kunvarpreet.standbymode.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Pure business logic for WMO weather code mapping, JSON parsing,
 * formatting, and cache freshness rules.
 */
object WeatherLogic {

    const val CACHE_EXPIRY_MILLIS: Long = 20 * 60 * 1000L // 20 minutes

    /**
     * Determines whether a cached forecast is still fresh.
     */
    fun isCacheFresh(lastUpdatedMillis: Long, currentMillis: Long): Boolean {
        if (lastUpdatedMillis <= 0L || currentMillis < lastUpdatedMillis) return false
        return (currentMillis - lastUpdatedMillis) < CACHE_EXPIRY_MILLIS
    }

    /**
     * Maps WMO weather interpretation code and daylight flag to condition details.
     */
    fun mapWeatherCode(weatherCode: Int, isDay: Boolean = true): WeatherCondition {
        return when (weatherCode) {
            0 -> WeatherCondition(
                code = 0,
                description = "Clear sky",
                iconRes = if (isDay) R.drawable.ic_weather_sunny else R.drawable.ic_weather_night
            )
            1 -> WeatherCondition(
                code = 1,
                description = "Mainly clear",
                iconRes = if (isDay) R.drawable.ic_weather_sunny else R.drawable.ic_weather_night
            )
            2 -> WeatherCondition(
                code = 2,
                description = "Partly cloudy",
                iconRes = R.drawable.ic_weather_partly_cloudy
            )
            3 -> WeatherCondition(
                code = 3,
                description = "Overcast",
                iconRes = R.drawable.ic_weather_cloudy
            )
            45 -> WeatherCondition(
                code = 45,
                description = "Fog",
                iconRes = R.drawable.ic_weather_fog
            )
            48 -> WeatherCondition(
                code = 48,
                description = "Depositing rime fog",
                iconRes = R.drawable.ic_weather_fog
            )
            51 -> WeatherCondition(
                code = 51,
                description = "Light drizzle",
                iconRes = R.drawable.ic_weather_rain
            )
            53 -> WeatherCondition(
                code = 53,
                description = "Moderate drizzle",
                iconRes = R.drawable.ic_weather_rain
            )
            55 -> WeatherCondition(
                code = 55,
                description = "Dense drizzle",
                iconRes = R.drawable.ic_weather_rain
            )
            56, 57 -> WeatherCondition(
                code = weatherCode,
                description = "Freezing drizzle",
                iconRes = R.drawable.ic_weather_rain
            )
            61 -> WeatherCondition(
                code = 61,
                description = "Slight rain",
                iconRes = R.drawable.ic_weather_rain
            )
            63 -> WeatherCondition(
                code = 63,
                description = "Moderate rain",
                iconRes = R.drawable.ic_weather_rain
            )
            65 -> WeatherCondition(
                code = 65,
                description = "Heavy rain",
                iconRes = R.drawable.ic_weather_rain
            )
            66, 67 -> WeatherCondition(
                code = weatherCode,
                description = "Freezing rain",
                iconRes = R.drawable.ic_weather_rain
            )
            71 -> WeatherCondition(
                code = 71,
                description = "Slight snow",
                iconRes = R.drawable.ic_weather_snow
            )
            73 -> WeatherCondition(
                code = 73,
                description = "Moderate snow",
                iconRes = R.drawable.ic_weather_snow
            )
            75 -> WeatherCondition(
                code = 75,
                description = "Heavy snow",
                iconRes = R.drawable.ic_weather_snow
            )
            77 -> WeatherCondition(
                code = 77,
                description = "Snow grains",
                iconRes = R.drawable.ic_weather_snow
            )
            80, 81, 82 -> WeatherCondition(
                code = weatherCode,
                description = "Rain showers",
                iconRes = R.drawable.ic_weather_rain
            )
            85, 86 -> WeatherCondition(
                code = weatherCode,
                description = "Snow showers",
                iconRes = R.drawable.ic_weather_snow
            )
            95 -> WeatherCondition(
                code = 95,
                description = "Thunderstorm",
                iconRes = R.drawable.ic_weather_thunderstorm
            )
            96, 99 -> WeatherCondition(
                code = weatherCode,
                description = "Thunderstorm with hail",
                iconRes = R.drawable.ic_weather_thunderstorm
            )
            else -> WeatherCondition(
                code = weatherCode,
                description = "Weather",
                iconRes = R.drawable.ic_weather_default
            )
        }
    }

    /**
     * Formats Celsius temperature as an integer with degree symbol, e.g. "22°".
     */
    fun formatTemperature(tempC: Double): String {
        return "${tempC.roundToInt()}°"
    }

    /**
     * Formats today's high and low temperatures, e.g. "H:31°  L:21°".
     */
    fun formatHighLow(maxC: Double, minC: Double): String {
        return "H:${maxC.roundToInt()}°  L:${minC.roundToInt()}°"
    }

    /**
     * Formats precipitation probability percentage, e.g. "40%".
     */
    fun formatPrecipitation(probabilityMax: Int): String {
        val clamped = probabilityMax.coerceIn(0, 100)
        return "$clamped%"
    }

    /**
     * Formats last-updated timestamp compactly for bedside viewing.
     */
    fun formatLastUpdated(lastUpdatedMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (lastUpdatedMillis <= 0L) return ""
        val time = Instant.ofEpochMilli(lastUpdatedMillis).atZone(zoneId).toLocalTime()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return "Updated ${formatter.format(time)}"
    }

    /**
     * Parses the Open-Meteo forecast JSON response into a [ParsedForecastData] bundle.
     * Throws [IllegalArgumentException] on invalid, malformed, or missing required fields.
     */
    fun parseForecastJson(jsonText: String): ParsedForecastData {
        val root = try {
            SimpleJsonParser(jsonText).parse() as? JsonVal.Obj
                ?: throw IllegalArgumentException("Root JSON is not an object")
        } catch (e: Exception) {
            throw IllegalArgumentException("Malformed JSON payload: ${e.message}", e)
        }

        val currentObj = root.map["current"] as? JsonVal.Obj
            ?: throw IllegalArgumentException("Missing 'current' object in forecast response")

        val tempC = (currentObj.map["temperature_2m"] as? JsonVal.Num)?.value
            ?: throw IllegalArgumentException("Missing or invalid 'current.temperature_2m'")
        if (tempC < -100.0 || tempC > 70.0) {
            throw IllegalArgumentException("Temperature out of valid physical range: $tempC")
        }

        val weatherCode = (currentObj.map["weather_code"] as? JsonVal.Num)?.value?.toInt()
            ?: throw IllegalArgumentException("Missing or invalid 'current.weather_code'")

        val isDayInt = (currentObj.map["is_day"] as? JsonVal.Num)?.value?.toInt() ?: 1
        val isDay = isDayInt == 1

        val dailyObj = root.map["daily"] as? JsonVal.Obj
            ?: throw IllegalArgumentException("Missing 'daily' object in forecast response")

        val maxTempList = dailyObj.map["temperature_2m_max"] as? JsonVal.Arr
            ?: throw IllegalArgumentException("Missing 'daily.temperature_2m_max'")
        val maxTempC = (maxTempList.list.firstOrNull() as? JsonVal.Num)?.value
            ?: throw IllegalArgumentException("Empty 'daily.temperature_2m_max'")

        val minTempList = dailyObj.map["temperature_2m_min"] as? JsonVal.Arr
            ?: throw IllegalArgumentException("Missing 'daily.temperature_2m_min'")
        val minTempC = (minTempList.list.firstOrNull() as? JsonVal.Num)?.value
            ?: throw IllegalArgumentException("Empty 'daily.temperature_2m_min'")

        val precipList = dailyObj.map["precipitation_probability_max"] as? JsonVal.Arr
        val precipProbability = (precipList?.list?.firstOrNull() as? JsonVal.Num)?.value?.toInt() ?: 0

        return ParsedForecastData(
            temperatureC = tempC,
            weatherCode = weatherCode,
            isDay = isDay,
            maxTempC = maxTempC,
            minTempC = minTempC,
            precipitationProbabilityMax = precipProbability.coerceIn(0, 100)
        )
    }

    data class ParsedForecastData(
        val temperatureC: Double,
        val weatherCode: Int,
        val isDay: Boolean,
        val maxTempC: Double,
        val minTempC: Double,
        val precipitationProbabilityMax: Int
    )

    /**
     * Lightweight pure Kotlin JSON AST and parser.
     */
    private sealed class JsonVal {
        data class Obj(val map: Map<String, JsonVal>) : JsonVal()
        data class Arr(val list: List<JsonVal>) : JsonVal()
        data class Str(val value: String) : JsonVal()
        data class Num(val value: Double) : JsonVal()
        data class Bool(val value: Boolean) : JsonVal()
        data object Null : JsonVal()
    }

    private class SimpleJsonParser(private val input: String) {
        private var index = 0

        fun parse(): JsonVal {
            val result = parseValue()
            skipWhitespace()
            return result
        }

        private fun skipWhitespace() {
            while (index < input.length && input[index].isWhitespace()) {
                index++
            }
        }

        private fun parseString(): String {
            index++ // skip opening quote
            val sb = java.lang.StringBuilder()
            while (index < input.length) {
                val c = input[index++]
                if (c == '"') return sb.toString()
                if (c == '\\' && index < input.length) {
                    val esc = input[index++]
                    when (esc) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (index + 4 <= input.length) {
                                val hex = input.substring(index, index + 4)
                                sb.append(hex.toInt(16).toChar())
                                index += 4
                            }
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            throw IllegalArgumentException("Unterminated string")
        }

        private fun parseNumber(): Double {
            val start = index
            if (index < input.length && input[index] == '-') index++
            while (index < input.length && (input[index].isDigit() || input[index] == '.' || input[index] == 'e' || input[index] == 'E' || input[index] == '+' || input[index] == '-')) {
                index++
            }
            val numStr = input.substring(start, index)
            return numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
        }

        private fun parseValue(): JsonVal {
            skipWhitespace()
            if (index >= input.length) throw IllegalArgumentException("Unexpected EOF")
            return when (val c = input[index]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonVal.Str(parseString())
                't', 'f' -> {
                    if (input.startsWith("true", index)) {
                        index += 4
                        JsonVal.Bool(true)
                    } else if (input.startsWith("false", index)) {
                        index += 5
                        JsonVal.Bool(false)
                    } else {
                        throw IllegalArgumentException("Invalid literal")
                    }
                }
                'n' -> {
                    if (input.startsWith("null", index)) {
                        index += 4
                        JsonVal.Null
                    } else {
                        throw IllegalArgumentException("Invalid literal")
                    }
                }
                '-', in '0'..'9' -> JsonVal.Num(parseNumber())
                else -> throw IllegalArgumentException("Unexpected char: $c at $index")
            }
        }

        private fun parseObject(): JsonVal.Obj {
            index++ // skip '{'
            val map = mutableMapOf<String, JsonVal>()
            skipWhitespace()
            if (index < input.length && input[index] == '}') {
                index++
                return JsonVal.Obj(map)
            }
            while (index < input.length) {
                skipWhitespace()
                if (input[index] != '"') throw IllegalArgumentException("Expected string key in object at $index")
                val key = parseString()
                skipWhitespace()
                if (index >= input.length || input[index] != ':') throw IllegalArgumentException("Expected ':' at $index")
                index++ // skip ':'
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (index < input.length && input[index] == ',') {
                    index++
                } else if (index < input.length && input[index] == '}') {
                    index++
                    return JsonVal.Obj(map)
                } else {
                    throw IllegalArgumentException("Expected ',' or '}' in object at $index")
                }
            }
            throw IllegalArgumentException("Unterminated object")
        }

        private fun parseArray(): JsonVal.Arr {
            index++ // skip '['
            val list = mutableListOf<JsonVal>()
            skipWhitespace()
            if (index < input.length && input[index] == ']') {
                index++
                return JsonVal.Arr(list)
            }
            while (index < input.length) {
                val value = parseValue()
                list.add(value)
                skipWhitespace()
                if (index < input.length && input[index] == ',') {
                    index++
                } else if (index < input.length && input[index] == ']') {
                    index++
                    return JsonVal.Arr(list)
                } else {
                    throw IllegalArgumentException("Expected ',' or ']' in array at $index")
                }
            }
            throw IllegalArgumentException("Unterminated array")
        }
    }
}
