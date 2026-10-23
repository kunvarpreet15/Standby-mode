package com.kunvarpreet.standbymode.core.calendar

import java.time.DayOfWeek
import java.time.YearMonth

/**
 * Access permission state for Android Calendar Provider.
 */
enum class CalendarAccessState {
    PERMISSION_GRANTED,
    PERMISSION_REQUIRED
}

/**
 * Immutable representation of a single calendar event occurrence.
 */
data class CalendarEventItem(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val isOngoing: Boolean = false,
    val isAllDay: Boolean = false
) {
    val displayTitle: String
        get() = if (title.isBlank()) "Untitled event" else title
}

/**
 * A single calendar cell in the monthly grid.
 */
data class CalendarDayItem(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasEvent: Boolean
)

/**
 * Structured data representing a complete month's calendar view.
 */
data class CalendarMonthData(
    val yearMonth: YearMonth,
    val firstDayOfWeek: DayOfWeek,
    val days: List<CalendarDayItem>,
    val monthLabel: String,
    val weekdayLabels: List<String>
)

/**
 * Distinct presentation states for CalendarMiniWidget.
 */
sealed interface CalendarAgendaUiState {
    data object Loading : CalendarAgendaUiState

    data class PermissionRequired(
        val isPermanentlyDenied: Boolean = false
    ) : CalendarAgendaUiState

    data class Empty(
        val monthData: CalendarMonthData
    ) : CalendarAgendaUiState

    data class Loaded(
        val monthData: CalendarMonthData,
        val nextEvent: CalendarEventItem?
    ) : CalendarAgendaUiState

    data class Error(
        val message: String
    ) : CalendarAgendaUiState
}
