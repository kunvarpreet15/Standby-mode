package com.kunvarpreet.standbymode.core.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Unit tests for pure CalendarLogic algorithms:
 * - Month-grid generation, weekday ordering, and padding
 * - Leap year calculations and month lengths
 * - Today highlight correctness
 * - Event occurrence mapping and multi-event day dots
 * - Next event selection (ongoing, upcoming, and ended events)
 * - Safe time formatting and boundary handling
 */
class CalendarLogicTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")

    @Test
    fun buildMonthData_respectsLocaleWeekdayOrdering_usSundayFirst() {
        val yearMonth = YearMonth.of(2024, 10) // Oct 2024 starts on Tuesday
        val today = LocalDate.of(2024, 10, 18)
        val monthData = CalendarLogic.buildMonthData(
            yearMonth = yearMonth,
            today = today,
            locale = Locale.US,
            eventDays = emptySet()
        )

        assertEquals(DayOfWeek.SUNDAY, monthData.firstDayOfWeek)
        assertEquals(7, monthData.weekdayLabels.size)
        // US Sunday-first starts with S
        assertEquals("S", monthData.weekdayLabels[0])
        assertEquals("M", monthData.weekdayLabels[1])
        assertEquals("T", monthData.weekdayLabels[2])
        assertEquals("W", monthData.weekdayLabels[3])
        assertEquals("T", monthData.weekdayLabels[4])
        assertEquals("F", monthData.weekdayLabels[5])
        assertEquals("S", monthData.weekdayLabels[6])

        // Oct 1 2024 was Tuesday -> lead days with Sunday start = 2 (Sun Sep 29, Mon Sep 30)
        assertEquals(2, monthData.days.takeWhile { !it.isCurrentMonth }.size)
        assertEquals(29, monthData.days[0].dayOfMonth)
        assertEquals(30, monthData.days[1].dayOfMonth)

        // Day index 2 is Oct 1
        val firstDay = monthData.days[2]
        assertEquals(1, firstDay.dayOfMonth)
        assertTrue(firstDay.isCurrentMonth)
    }

    @Test
    fun buildMonthData_respectsLocaleWeekdayOrdering_franceMondayFirst() {
        val yearMonth = YearMonth.of(2024, 10) // Oct 1, 2024 was Tuesday
        val today = LocalDate.of(2024, 10, 18)
        val monthData = CalendarLogic.buildMonthData(
            yearMonth = yearMonth,
            today = today,
            locale = Locale.FRANCE,
            eventDays = emptySet()
        )

        assertEquals(DayOfWeek.MONDAY, monthData.firstDayOfWeek)
        // Oct 1 was Tuesday -> lead days with Monday start = 1 (Mon Sep 30)
        assertEquals(1, monthData.days.takeWhile { !it.isCurrentMonth }.size)
        assertEquals(30, monthData.days[0].dayOfMonth)

        // Day index 1 is Oct 1
        assertEquals(1, monthData.days[1].dayOfMonth)
        assertTrue(monthData.days[1].isCurrentMonth)
    }

    @Test
    fun buildMonthData_handlesMonthLengthsAndLeapYearsCorrectly() {
        val leapFeb = YearMonth.of(2024, 2)
        val nonLeapFeb = YearMonth.of(2025, 2)
        val april = YearMonth.of(2024, 4)
        val january = YearMonth.of(2024, 1)

        val leapFebData = CalendarLogic.buildMonthData(leapFeb, LocalDate.of(2024, 2, 1), Locale.US, emptySet())
        val nonLeapFebData = CalendarLogic.buildMonthData(nonLeapFeb, LocalDate.of(2025, 2, 1), Locale.US, emptySet())
        val aprilData = CalendarLogic.buildMonthData(april, LocalDate.of(2024, 4, 1), Locale.US, emptySet())
        val janData = CalendarLogic.buildMonthData(january, LocalDate.of(2024, 1, 1), Locale.US, emptySet())

        assertEquals(29, leapFebData.days.count { it.isCurrentMonth })
        assertEquals(28, nonLeapFebData.days.count { it.isCurrentMonth })
        assertEquals(30, aprilData.days.count { it.isCurrentMonth })
        assertEquals(31, janData.days.count { it.isCurrentMonth })

        // Grid must always contain complete 7-day rows
        assertEquals(0, leapFebData.days.size % 7)
        assertEquals(0, nonLeapFebData.days.size % 7)
        assertEquals(0, aprilData.days.size % 7)
        assertEquals(0, janData.days.size % 7)
    }

    @Test
    fun buildMonthData_marksTodayAccurately() {
        val yearMonth = YearMonth.of(2024, 10)
        val today = LocalDate.of(2024, 10, 15)

        val monthData = CalendarLogic.buildMonthData(
            yearMonth = yearMonth,
            today = today,
            locale = Locale.US,
            eventDays = emptySet()
        )

        val todayItems = monthData.days.filter { it.isToday }
        assertEquals(1, todayItems.size)
        assertEquals(15, todayItems.first().dayOfMonth)
        assertTrue(todayItems.first().isCurrentMonth)

        // When today is in another month, no day in this month is marked as today
        val otherMonthToday = LocalDate.of(2024, 11, 15)
        val otherMonthData = CalendarLogic.buildMonthData(
            yearMonth = yearMonth,
            today = otherMonthToday,
            locale = Locale.US,
            eventDays = emptySet()
        )
        assertTrue(otherMonthData.days.none { it.isToday })
    }

    @Test
    fun calculateEventDays_identifiesDaysWithSingleAndMultipleEvents() {
        val yearMonth = YearMonth.of(2024, 10)

        // Event on Oct 5
        val event1 = CalendarEventItem(
            id = 1L,
            title = "Morning Sync",
            startMillis = ZonedDateTime.of(2024, 10, 5, 9, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            endMillis = ZonedDateTime.of(2024, 10, 5, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        )
        // Another event on Oct 5
        val event2 = CalendarEventItem(
            id = 2L,
            title = "Design Review",
            startMillis = ZonedDateTime.of(2024, 10, 5, 14, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            endMillis = ZonedDateTime.of(2024, 10, 5, 15, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        )
        // Multi-day event spanning Oct 10 to Oct 12
        val event3 = CalendarEventItem(
            id = 3L,
            title = "Conference",
            startMillis = ZonedDateTime.of(2024, 10, 10, 8, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            endMillis = ZonedDateTime.of(2024, 10, 12, 18, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        )
        // Event in November (should not be in Oct event days)
        val eventNov = CalendarEventItem(
            id = 4L,
            title = "Next Month",
            startMillis = ZonedDateTime.of(2024, 11, 1, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            endMillis = ZonedDateTime.of(2024, 11, 1, 11, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        )

        val eventDays = CalendarLogic.calculateEventDays(
            events = listOf(event1, event2, event3, eventNov),
            yearMonth = yearMonth,
            zoneId = zoneId
        )

        assertTrue(eventDays.contains(5))
        assertTrue(eventDays.contains(10))
        assertTrue(eventDays.contains(11))
        assertTrue(eventDays.contains(12))
        assertFalse(eventDays.contains(1))
        assertFalse(eventDays.contains(4))
        assertEquals(4, eventDays.size)
    }

    @Test
    fun selectNextEvent_discardsEndedEvents_andSelectsOngoingOrUpcoming() {
        val now = ZonedDateTime.of(2024, 10, 15, 12, 0, 0, 0, zoneId).toInstant().toEpochMilli()

        // 1. Ended event: 10:00 - 11:00
        val endedEvent = CalendarEventItem(
            id = 1L,
            title = "Past Meeting",
            startMillis = ZonedDateTime.of(2024, 10, 15, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            endMillis = ZonedDateTime.of(2024, 10, 15, 11, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        )

        // 2. Ongoing event: 11:30 - 12:30 (active during now = 12:00)
        val ongoingEvent = CalendarEventItem(
            id = 2L,
            title = "Lunch Meeting",
            startMillis = ZonedDateTime.of(2024, 10, 15, 11, 30, 0, 0, zoneId).toInstant().toEpochMilli(),
            endMillis = ZonedDateTime.of(2024, 10, 15, 12, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        )

        // 3. Upcoming event: 14:00 - 15:00
        val upcomingEvent = CalendarEventItem(
            id = 3L,
            title = "Team Standup",
            startMillis = ZonedDateTime.of(2024, 10, 15, 14, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            endMillis = ZonedDateTime.of(2024, 10, 15, 15, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        )

        // Ongoing event should take precedence
        val selectedOngoing = CalendarLogic.selectNextEvent(listOf(endedEvent, ongoingEvent, upcomingEvent), now)
        assertNotNull(selectedOngoing)
        assertEquals("Lunch Meeting", selectedOngoing?.title)
        assertTrue(selectedOngoing?.isOngoing == true)

        // When no ongoing event exists, upcoming event is selected
        val selectedUpcoming = CalendarLogic.selectNextEvent(listOf(endedEvent, upcomingEvent), now)
        assertNotNull(selectedUpcoming)
        assertEquals("Team Standup", selectedUpcoming?.title)
        assertFalse(selectedUpcoming?.isOngoing == true)

        // When only ended events exist, returns null
        val noUpcoming = CalendarLogic.selectNextEvent(listOf(endedEvent), now)
        assertNull(noUpcoming)

        // When list is empty, returns null
        assertNull(CalendarLogic.selectNextEvent(emptyList(), now))
    }

    @Test
    fun formatEventTime_formatsCorrectly_acrossDifferentScenarios() {
        val nowZdt = ZonedDateTime.of(2024, 10, 15, 10, 0, 0, 0, zoneId)
        val nowMillis = nowZdt.toInstant().toEpochMilli()

        // 1. Ongoing event
        val ongoingEvent = CalendarEventItem(
            id = 1L,
            title = "Active",
            startMillis = nowZdt.minusMinutes(30).toInstant().toEpochMilli(),
            endMillis = nowZdt.plusMinutes(30).toInstant().toEpochMilli(),
            isOngoing = true
        )
        assertEquals("In progress", CalendarLogic.formatEventTime(ongoingEvent, nowMillis, false, Locale.US, zoneId))

        // 2. All-day event
        val allDayEvent = CalendarEventItem(
            id = 2L,
            title = "Holiday",
            startMillis = nowZdt.toInstant().toEpochMilli(),
            endMillis = nowZdt.plusDays(1).toInstant().toEpochMilli(),
            isAllDay = true
        )
        assertEquals("All day", CalendarLogic.formatEventTime(allDayEvent, nowMillis, false, Locale.US, zoneId))

        // 3. Today timed event (14:30) in 12-hour and 24-hour formats
        val todayEvent = CalendarEventItem(
            id = 3L,
            title = "Doctor",
            startMillis = ZonedDateTime.of(2024, 10, 15, 14, 30, 0, 0, zoneId).toInstant().toEpochMilli(),
            endMillis = ZonedDateTime.of(2024, 10, 15, 15, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        )
        assertEquals("2:30 PM", CalendarLogic.formatEventTime(todayEvent, nowMillis, false, Locale.US, zoneId))
        assertEquals("14:30", CalendarLogic.formatEventTime(todayEvent, nowMillis, true, Locale.US, zoneId))

        // 4. Tomorrow timed event
        val tomorrowEvent = CalendarEventItem(
            id = 4L,
            title = "Flight",
            startMillis = ZonedDateTime.of(2024, 10, 16, 8, 15, 0, 0, zoneId).toInstant().toEpochMilli(),
            endMillis = ZonedDateTime.of(2024, 10, 16, 11, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        )
        assertEquals("Tomorrow, 8:15 AM", CalendarLogic.formatEventTime(tomorrowEvent, nowMillis, false, Locale.US, zoneId))

        // 5. Future day event (Oct 18 is Friday)
        val futureEvent = CalendarEventItem(
            id = 5L,
            title = "Weekend Trip",
            startMillis = ZonedDateTime.of(2024, 10, 18, 16, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            endMillis = ZonedDateTime.of(2024, 10, 18, 18, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        )
        assertEquals("Fri, 4:00 PM", CalendarLogic.formatEventTime(futureEvent, nowMillis, false, Locale.US, zoneId))
    }

    @Test
    fun calendarEventItem_handlesMissingTitleAndInvalidTimestampsSafely() {
        // Blank or null title fallback to "Untitled event"
        val itemWithBlankTitle = CalendarEventItem(
            id = 1L,
            title = "",
            startMillis = 1000L,
            endMillis = 2000L
        )
        assertEquals("Untitled event", itemWithBlankTitle.displayTitle)

        val itemWithWhitespaceTitle = CalendarEventItem(
            id = 2L,
            title = "   ",
            startMillis = 1000L,
            endMillis = 2000L
        )
        assertEquals("Untitled event", itemWithWhitespaceTitle.displayTitle)

        // Invalid timestamps (0 or negative) are safely skipped by calculation
        val invalidTimestampEvent = CalendarEventItem(
            id = 3L,
            title = "Invalid Time",
            startMillis = 0L,
            endMillis = 0L
        )
        val eventDays = CalendarLogic.calculateEventDays(
            events = listOf(invalidTimestampEvent),
            yearMonth = YearMonth.of(2024, 10),
            zoneId = zoneId
        )
        assertTrue(eventDays.isEmpty())

        val selected = CalendarLogic.selectNextEvent(listOf(invalidTimestampEvent), 1000L)
        assertNull(selected)
    }
}
