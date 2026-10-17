package com.kunvarpreet.standbymode.core.time

/**
 * Angles in degrees (0° = 12 o'clock / straight up, 90° = 3 o'clock, 180° = 6 o'clock, 270° = 9 o'clock).
 */
data class AnalogClockAngles(
    val hourAngle: Float,
    val minuteAngle: Float,
    val secondAngle: Float? = null
)

/**
 * Pure calculator for analog clock hand angles.
 */
object AnalogClockCalculator {

    /**
     * Calculates the hand angles for an analog clock.
     *
     * @param hours 0..23 or 1..12
     * @param minutes 0..59
     * @param seconds 0..59, or null if precision is minute-only
     * @param showSeconds whether the second hand should be included
     */
    fun calculateAngles(
        hours: Int,
        minutes: Int,
        seconds: Int? = null,
        showSeconds: Boolean = true
    ): AnalogClockAngles {
        val sec = seconds ?: 0
        val secFraction = sec / 60f
        val minuteFraction = (minutes + secFraction) / 60f

        val hourAngle = ((hours % 12) + minuteFraction) * 30f
        val minuteAngle = (minutes + secFraction) * 6f
        val secondAngle = if (showSeconds && seconds != null) (sec * 6f) % 360f else null

        return AnalogClockAngles(
            hourAngle = hourAngle % 360f,
            minuteAngle = minuteAngle % 360f,
            secondAngle = secondAngle
        )
    }
}
