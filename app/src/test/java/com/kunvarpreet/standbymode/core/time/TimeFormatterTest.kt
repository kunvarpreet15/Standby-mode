package com.kunvarpreet.standbymode.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class TimeFormatterTest {

    private val zoneUtc = ZoneOffset.UTC
    private val localeUs = Locale.US

    private fun instantOf(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int) =
        LocalDateTime.of(year, month, day, hour, minute, second).toInstant(zoneUtc)

    @Test
    fun `test midnight in 24-hour format`() {
        val midnight = instantOf(2026, 10, 18, 0, 0, 0)
        val result = TimeFormatter.format(
            instant = midnight,
            zoneId = zoneUtc,
            locale = localeUs,
            is24Hour = true,
            showSeconds = false
        )

        assertEquals("00", result.hours)
        assertEquals("00", result.minutes)
        assertNull(result.seconds)
        assertNull(result.amPm)
        assertEquals("SUN, OCT 18", result.formattedDate)
    }

    @Test
    fun `test midnight in 12-hour format`() {
        val midnight = instantOf(2026, 10, 18, 0, 0, 0)
        val result = TimeFormatter.format(
            instant = midnight,
            zoneId = zoneUtc,
            locale = localeUs,
            is24Hour = false,
            showSeconds = false
        )

        assertEquals("12", result.hours)
        assertEquals("00", result.minutes)
        assertNull(result.seconds)
        assertEquals("AM", result.amPm)
    }

    @Test
    fun `test noon in 24-hour format`() {
        val noon = instantOf(2026, 10, 18, 12, 0, 0)
        val result = TimeFormatter.format(
            instant = noon,
            zoneId = zoneUtc,
            locale = localeUs,
            is24Hour = true,
            showSeconds = false
        )

        assertEquals("12", result.hours)
        assertEquals("00", result.minutes)
        assertNull(result.amPm)
    }

    @Test
    fun `test noon in 12-hour format`() {
        val noon = instantOf(2026, 10, 18, 12, 0, 0)
        val result = TimeFormatter.format(
            instant = noon,
            zoneId = zoneUtc,
            locale = localeUs,
            is24Hour = false,
            showSeconds = false
        )

        assertEquals("12", result.hours)
        assertEquals("00", result.minutes)
        assertEquals("PM", result.amPm)
    }

    @Test
    fun `test 1_05 AM in 12-hour format without zero padding`() {
        val time = instantOf(2026, 10, 18, 1, 5, 9)
        val result = TimeFormatter.format(
            instant = time,
            zoneId = zoneUtc,
            locale = localeUs,
            is24Hour = false,
            showSeconds = false,
            zeroPad12Hour = false
        )

        assertEquals("1", result.hours)
        assertEquals("05", result.minutes)
        assertEquals("AM", result.amPm)
    }

    @Test
    fun `test 1_05 AM in 12-hour format with zero padding`() {
        val time = instantOf(2026, 10, 18, 1, 5, 9)
        val result = TimeFormatter.format(
            instant = time,
            zoneId = zoneUtc,
            locale = localeUs,
            is24Hour = false,
            showSeconds = false,
            zeroPad12Hour = true
        )

        assertEquals("01", result.hours)
        assertEquals("05", result.minutes)
        assertEquals("AM", result.amPm)
    }

    @Test
    fun `test 11_59 PM in 12-hour format with seconds`() {
        val time = instantOf(2026, 10, 18, 23, 59, 45)
        val result = TimeFormatter.format(
            instant = time,
            zoneId = zoneUtc,
            locale = localeUs,
            is24Hour = false,
            showSeconds = true
        )

        assertEquals("11", result.hours)
        assertEquals("59", result.minutes)
        assertEquals("45", result.seconds)
        assertEquals("PM", result.amPm)
    }

    @Test
    fun `test 11_59 PM in 24-hour format with seconds`() {
        val time = instantOf(2026, 10, 18, 23, 59, 7)
        val result = TimeFormatter.format(
            instant = time,
            zoneId = zoneUtc,
            locale = localeUs,
            is24Hour = true,
            showSeconds = true
        )

        assertEquals("23", result.hours)
        assertEquals("59", result.minutes)
        assertEquals("07", result.seconds)
        assertNull(result.amPm)
    }

    @Test
    fun `test time across different timezones`() {
        val timeUtc = instantOf(2026, 10, 18, 10, 30, 0)
        val zoneTokyo = ZoneId.of("Asia/Tokyo") // UTC+9 -> 19:30
        val resultTokyo = TimeFormatter.format(
            instant = timeUtc,
            zoneId = zoneTokyo,
            locale = localeUs,
            is24Hour = true
        )

        assertEquals("19", resultTokyo.hours)
        assertEquals("30", resultTokyo.minutes)
    }
}
