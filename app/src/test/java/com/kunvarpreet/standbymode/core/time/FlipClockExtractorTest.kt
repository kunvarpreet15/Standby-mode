package com.kunvarpreet.standbymode.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipClockExtractorTest {

    @Test
    fun extractDigits_pads_single_digit_hours_with_zero() {
        val digits = FlipClockExtractor.extractDigits("2", "45")
        assertEquals('0', digits.hourTens)
        assertEquals('2', digits.hourUnits)
        assertEquals('4', digits.minuteTens)
        assertEquals('5', digits.minuteUnits)
    }

    @Test
    fun extractDigits_handles_two_digit_hours_and_minutes() {
        val digits = FlipClockExtractor.extractDigits("10", "42")
        assertEquals('1', digits.hourTens)
        assertEquals('0', digits.hourUnits)
        assertEquals('4', digits.minuteTens)
        assertEquals('2', digits.minuteUnits)
    }

    @Test
    fun extractDigits_handles_midnight_zeroes() {
        val digits = FlipClockExtractor.extractDigits("00", "00")
        assertEquals('0', digits.hourTens)
        assertEquals('0', digits.hourUnits)
        assertEquals('0', digits.minuteTens)
        assertEquals('0', digits.minuteUnits)
    }

    @Test
    fun extractDigits_handles_empty_inputs_safely() {
        val digits = FlipClockExtractor.extractDigits("", "")
        assertEquals('0', digits.hourTens)
        assertEquals('0', digits.hourUnits)
        assertEquals('0', digits.minuteTens)
        assertEquals('0', digits.minuteUnits)
    }

    @Test
    fun findChangedDigits_identifies_only_minute_units_change() {
        val prev = FlipClockExtractor.extractDigits("10", "42")
        val curr = FlipClockExtractor.extractDigits("10", "43")
        val changed = FlipClockExtractor.findChangedDigits(prev, curr)
        assertEquals(setOf("MU"), changed)
    }

    @Test
    fun findChangedDigits_identifies_minute_tens_and_units_change() {
        val prev = FlipClockExtractor.extractDigits("10", "49")
        val curr = FlipClockExtractor.extractDigits("10", "50")
        val changed = FlipClockExtractor.findChangedDigits(prev, curr)
        assertEquals(setOf("MT", "MU"), changed)
    }

    @Test
    fun findChangedDigits_identifies_hour_and_minute_change() {
        val prev = FlipClockExtractor.extractDigits("10", "59")
        val curr = FlipClockExtractor.extractDigits("11", "00")
        val changed = FlipClockExtractor.findChangedDigits(prev, curr)
        assertEquals(setOf("HU", "MT", "MU"), changed)
    }

    @Test
    fun findChangedDigits_returns_empty_when_no_change() {
        val prev = FlipClockExtractor.extractDigits("10", "42")
        val curr = FlipClockExtractor.extractDigits("10", "42")
        val changed = FlipClockExtractor.findChangedDigits(prev, curr)
        assertTrue(changed.isEmpty())
    }
}
