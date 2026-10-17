package com.kunvarpreet.standbymode.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AnalogClockAnglesTest {

    private val delta = 0.001f

    @Test
    fun at_12_00_hour_and_minute_hands_point_straight_up() {
        val angles = AnalogClockCalculator.calculateAngles(hours = 12, minutes = 0, seconds = 0)
        assertEquals(0f, angles.hourAngle, delta)
        assertEquals(0f, angles.minuteAngle, delta)
    }

    @Test
    fun at_midnight_hour_and_minute_hands_point_straight_up() {
        val angles = AnalogClockCalculator.calculateAngles(hours = 0, minutes = 0, seconds = 0)
        assertEquals(0f, angles.hourAngle, delta)
        assertEquals(0f, angles.minuteAngle, delta)
    }

    @Test
    fun at_3_00_hour_points_to_3_and_minute_points_to_12() {
        val angles = AnalogClockCalculator.calculateAngles(hours = 3, minutes = 0, seconds = 0)
        assertEquals(90f, angles.hourAngle, delta)
        assertEquals(0f, angles.minuteAngle, delta)
    }

    @Test
    fun at_6_30_hour_lies_halfway_between_6_and_7_and_minute_points_to_6() {
        val angles = AnalogClockCalculator.calculateAngles(hours = 6, minutes = 30, seconds = 0)
        // 6 * 30 + (30/60) * 30 = 180 + 15 = 195°
        assertEquals(195f, angles.hourAngle, delta)
        // 30 * 6 = 180°
        assertEquals(180f, angles.minuteAngle, delta)
    }

    @Test
    fun at_9_15_hour_incorporates_quarter_hour_offset() {
        val angles = AnalogClockCalculator.calculateAngles(hours = 9, minutes = 15, seconds = 0)
        // 9 * 30 + (15/60) * 30 = 270 + 7.5 = 277.5°
        assertEquals(277.5f, angles.hourAngle, delta)
        // 15 * 6 = 90°
        assertEquals(90f, angles.minuteAngle, delta)
    }

    @Test
    fun seconds_hand_absent_when_disabled() {
        val angles = AnalogClockCalculator.calculateAngles(
            hours = 10,
            minutes = 10,
            seconds = 45,
            showSeconds = false
        )
        assertNull("Seconds hand must be absent when showSeconds is false", angles.secondAngle)
    }

    @Test
    fun seconds_hand_present_and_accurate_when_enabled() {
        val angles = AnalogClockCalculator.calculateAngles(
            hours = 10,
            minutes = 10,
            seconds = 45,
            showSeconds = true
        )
        assertNotNull(angles.secondAngle)
        assertEquals(270f, angles.secondAngle!!, delta)
    }

    @Test
    fun minute_and_hour_incorporate_seconds_progression() {
        val angles = AnalogClockCalculator.calculateAngles(
            hours = 12,
            minutes = 0,
            seconds = 30,
            showSeconds = true
        )
        // 30 seconds = 0.5 minute -> minuteAngle = 0.5 * 6° = 3°
        assertEquals(3f, angles.minuteAngle, delta)
        // 30 seconds into hour = 0.5/60 hour -> hourAngle = (0.5/60) * 30° = 0.25°
        assertEquals(0.25f, angles.hourAngle, delta)
    }
}
