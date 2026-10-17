package com.kunvarpreet.standbymode.core.time

/**
 * Immutable model containing the four individual digits for the Retro Flip Clock.
 * Format: [hourTens][hourUnits] : [minuteTens][minuteUnits]
 */
data class FlipClockDigits(
    val hourTens: Char,
    val hourUnits: Char,
    val minuteTens: Char,
    val minuteUnits: Char
)

/**
 * Pure extraction and formatting utility for Flip Clock digits.
 */
object FlipClockExtractor {

    /**
     * Extracts four individual digit characters from hours and minutes strings.
     * Guarantees two digits for hours and two digits for minutes.
     * Single-digit hours (e.g. "2" in 12-hour mode) are padded with '0' ("02").
     */
    fun extractDigits(hours: String, minutes: String): FlipClockDigits {
        val paddedHours = when {
            hours.length == 1 -> "0$hours"
            hours.length >= 2 -> hours.take(2)
            else -> "00"
        }

        val paddedMinutes = when {
            minutes.length == 1 -> "0$minutes"
            minutes.length >= 2 -> minutes.take(2)
            else -> "00"
        }

        return FlipClockDigits(
            hourTens = paddedHours[0],
            hourUnits = paddedHours[1],
            minuteTens = paddedMinutes[0],
            minuteUnits = paddedMinutes[1]
        )
    }

    /**
     * Returns a set of digit positions ('HT', 'HU', 'MT', 'MU') that changed between previous and current state.
     */
    fun findChangedDigits(previous: FlipClockDigits, current: FlipClockDigits): Set<String> {
        val changed = mutableSetOf<String>()
        if (previous.hourTens != current.hourTens) changed.add("HT")
        if (previous.hourUnits != current.hourUnits) changed.add("HU")
        if (previous.minuteTens != current.minuteTens) changed.add("MT")
        if (previous.minuteUnits != current.minuteUnits) changed.add("MU")
        return changed
    }
}
