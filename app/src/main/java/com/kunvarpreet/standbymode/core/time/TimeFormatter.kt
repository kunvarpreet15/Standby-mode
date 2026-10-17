package com.kunvarpreet.standbymode.core.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure formatting utility for converting instants into display-ready clock components.
 */
object TimeFormatter {

    fun format(
        instant: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
        is24Hour: Boolean = false,
        showSeconds: Boolean = false,
        zeroPad12Hour: Boolean = false
    ): FormattedTime {
        val zonedDateTime = instant.atZone(zoneId)
        val hour24 = zonedDateTime.hour
        val minute = zonedDateTime.minute
        val second = zonedDateTime.second

        val (hoursString, amPmString) = if (is24Hour) {
            String.format(locale, "%02d", hour24) to null
        } else {
            val hour12 = when (val h = hour24 % 12) {
                0 -> 12
                else -> h
            }
            val amPm = if (hour24 < 12) "AM" else "PM"
            val hStr = if (zeroPad12Hour) {
                String.format(locale, "%02d", hour12)
            } else {
                String.format(locale, "%d", hour12)
            }
            hStr to amPm
        }

        val minutesString = String.format(locale, "%02d", minute)
        val secondsString = if (showSeconds) {
            String.format(locale, "%02d", second)
        } else {
            null
        }

        val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", locale)
        val formattedDate = zonedDateTime.format(dateFormatter).uppercase(locale)

        return FormattedTime(
            hours = hoursString,
            minutes = minutesString,
            seconds = secondsString,
            amPm = amPmString,
            formattedDate = formattedDate
        )
    }
}
