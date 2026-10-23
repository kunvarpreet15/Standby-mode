package com.kunvarpreet.standbymode.core.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Pure business logic for calendar month grid generation, event selection,
 * date marking, and time formatting.
 */
object CalendarLogic {

    /**
     * Builds a structured month grid respecting the user's locale (weekday ordering),
     * leap years, current day indicator, and event occurrence markers.
     */
    fun buildMonthData(
        yearMonth: YearMonth,
        today: LocalDate,
        locale: Locale,
        eventDays: Set<Int>
    ): CalendarMonthData {
        val weekFields = WeekFields.of(locale)
        val firstDayOfWeek = weekFields.firstDayOfWeek

        // Build localized 7-day weekday column headers
        val weekdayLabels = (0..6).map { dayOffset ->
            val dow = firstDayOfWeek.plus(dayOffset.toLong())
            val label = dow.getDisplayName(TextStyle.NARROW, locale)
            if (label.isBlank()) dow.getDisplayName(TextStyle.SHORT, locale).take(1) else label
        }

        val monthLabel = "${yearMonth.month.getDisplayName(TextStyle.FULL, locale).uppercase(locale)} ${yearMonth.year}"

        val firstOfMonthDow = yearMonth.atDay(1).dayOfWeek
        val leadDays = (firstOfMonthDow.value - firstDayOfWeek.value + 7) % 7

        val prevMonth = yearMonth.minusMonths(1)
        val prevMonthLength = prevMonth.lengthOfMonth()

        val days = mutableListOf<CalendarDayItem>()

        // 1. Leading padding days from previous month
        for (i in 0 until leadDays) {
            val dayNumber = prevMonthLength - leadDays + i + 1
            days.add(
                CalendarDayItem(
                    dayOfMonth = dayNumber,
                    isCurrentMonth = false,
                    isToday = false,
                    hasEvent = false
                )
            )
        }

        // 2. Current month days
        val monthLength = yearMonth.lengthOfMonth()
        for (dayNumber in 1..monthLength) {
            val isToday = (yearMonth.year == today.year &&
                yearMonth.month == today.month &&
                dayNumber == today.dayOfMonth)
            val hasEvent = eventDays.contains(dayNumber)

            days.add(
                CalendarDayItem(
                    dayOfMonth = dayNumber,
                    isCurrentMonth = true,
                    isToday = isToday,
                    hasEvent = hasEvent
                )
            )
        }

        // 3. Trailing padding days to fill the final week row
        val remainder = days.size % 7
        if (remainder != 0) {
            val trailDays = 7 - remainder
            for (i in 1..trailDays) {
                days.add(
                    CalendarDayItem(
                        dayOfMonth = i,
                        isCurrentMonth = false,
                        isToday = false,
                        hasEvent = false
                    )
                )
            }
        }

        return CalendarMonthData(
            yearMonth = yearMonth,
            firstDayOfWeek = firstDayOfWeek,
            days = days,
            monthLabel = monthLabel,
            weekdayLabels = weekdayLabels
        )
    }

    /**
     * Determines which days of the given month contain at least one event occurrence.
     */
    fun calculateEventDays(
        events: List<CalendarEventItem>,
        yearMonth: YearMonth,
        zoneId: ZoneId
    ): Set<Int> {
        val result = mutableSetOf<Int>()

        for (event in events) {
            if (event.startMillis <= 0L && event.endMillis <= 0L) continue

            val effectiveEnd = if (event.endMillis > event.startMillis) event.endMillis - 1L else event.startMillis
            val startDate = Instant.ofEpochMilli(event.startMillis).atZone(zoneId).toLocalDate()
            val endDate = Instant.ofEpochMilli(effectiveEnd).atZone(zoneId).toLocalDate()

            var curr = startDate
            while (!curr.isAfter(endDate)) {
                if (curr.year == yearMonth.year && curr.month == yearMonth.month) {
                    result.add(curr.dayOfMonth)
                }
                curr = curr.plusDays(1)
            }
        }

        return result
    }

    /**
     * Selects the next relevant event:
     * - Discards events that have already ended before nowMillis.
     * - Identifies ongoing events (startMillis <= nowMillis < endMillis).
     * - Selects the earliest relevant event (ongoing events take precedence, followed by upcoming events sorted by startMillis).
     */
    fun selectNextEvent(
        events: List<CalendarEventItem>,
        nowMillis: Long
    ): CalendarEventItem? {
        val activeOrUpcoming = events
            .filter { it.endMillis > nowMillis && it.startMillis > 0L }
            .map { event ->
                val isOngoing = event.startMillis <= nowMillis && event.endMillis > nowMillis
                event.copy(isOngoing = isOngoing)
            }
            .sortedWith(
                compareBy<CalendarEventItem> { !it.isOngoing }
                    .thenBy { it.startMillis }
                    .thenBy { it.endMillis }
            )

        return activeOrUpcoming.firstOrNull()
    }

    /**
     * Formats an event occurrence time for glanceable bedside display.
     */
    fun formatEventTime(
        event: CalendarEventItem,
        nowMillis: Long,
        is24Hour: Boolean,
        locale: Locale,
        zoneId: ZoneId
    ): String {
        if (event.isOngoing) {
            return "In progress"
        }
        if (event.isAllDay) {
            return "All day"
        }

        val eventZdt = Instant.ofEpochMilli(event.startMillis).atZone(zoneId)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val eventDate = eventZdt.toLocalDate()

        val timePattern = if (is24Hour) "HH:mm" else "h:mm a"
        val timeFormatter = DateTimeFormatter.ofPattern(timePattern, locale)
        val timeStr = eventZdt.format(timeFormatter)

        return when {
            eventDate == today -> timeStr
            eventDate == today.plusDays(1) -> "Tomorrow, $timeStr"
            else -> {
                val dayFormatter = DateTimeFormatter.ofPattern("EEE", locale)
                "${eventZdt.format(dayFormatter)}, $timeStr"
            }
        }
    }
}
