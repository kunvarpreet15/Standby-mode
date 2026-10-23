package com.kunvarpreet.standbymode.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.R
import com.kunvarpreet.standbymode.core.calendar.CalendarAgendaUiState
import com.kunvarpreet.standbymode.core.calendar.CalendarDayItem
import com.kunvarpreet.standbymode.core.calendar.CalendarEventItem
import com.kunvarpreet.standbymode.core.calendar.CalendarLogic
import com.kunvarpreet.standbymode.core.calendar.CalendarMonthData
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary
import java.time.ZoneId
import java.util.Locale

/**
 * Compact ambient Calendar & Agenda widget for Duo Layout widget stacks.
 * Displays a concise monthly calendar grid with today highlighted, event indicators,
 * and the next relevant agenda event.
 */
@Composable
fun CalendarMiniWidget(
    modifier: Modifier = Modifier,
    uiState: CalendarAgendaUiState,
    accentColor: Color,
    isNightMode: Boolean = false,
    is24Hour: Boolean = false,
    onOpenPermissionSettings: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val surfaceColor = if (isNightMode) StandByNightSurface else StandBySurfaceCard
    val borderColor = if (isNightMode) StandByNightSurface else StandByBorder
    val primaryTextColor = if (isNightMode) StandByNightText else StandByTextPrimary
    val mutedTextColor = if (isNightMode) StandByNightTextMuted else StandByTextMuted
    val activeAccentColor = if (isNightMode) StandByNightText else accentColor

    Surface(
        modifier = modifier
            .widthIn(min = 260.dp, max = 380.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                contentDescription = when (uiState) {
                    is CalendarAgendaUiState.Loaded -> "Calendar widget: ${uiState.monthData.monthLabel}, next event: ${uiState.nextEvent?.displayTitle ?: "None"}"
                    is CalendarAgendaUiState.Empty -> "Calendar widget: ${uiState.monthData.monthLabel}, no upcoming events"
                    is CalendarAgendaUiState.PermissionRequired -> "Calendar permission required"
                    is CalendarAgendaUiState.Loading -> "Loading calendar"
                    is CalendarAgendaUiState.Error -> "Calendar error: ${uiState.message}"
                }
            },
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState) {
                is CalendarAgendaUiState.Loading -> {
                    CalendarLoadingState(mutedTextColor = mutedTextColor)
                }

                is CalendarAgendaUiState.PermissionRequired -> {
                    CalendarPermissionState(
                        primaryTextColor = primaryTextColor,
                        mutedTextColor = mutedTextColor,
                        accentColor = activeAccentColor,
                        onOpenSettings = onOpenPermissionSettings
                    )
                }

                is CalendarAgendaUiState.Error -> {
                    CalendarErrorState(
                        message = uiState.message,
                        primaryTextColor = primaryTextColor,
                        mutedTextColor = mutedTextColor,
                        accentColor = activeAccentColor,
                        onRetry = onRetry
                    )
                }

                is CalendarAgendaUiState.Empty -> {
                    CalendarContent(
                        monthData = uiState.monthData,
                        nextEvent = null,
                        primaryTextColor = primaryTextColor,
                        mutedTextColor = mutedTextColor,
                        accentColor = activeAccentColor,
                        isNightMode = isNightMode,
                        is24Hour = is24Hour
                    )
                }

                is CalendarAgendaUiState.Loaded -> {
                    CalendarContent(
                        monthData = uiState.monthData,
                        nextEvent = uiState.nextEvent,
                        primaryTextColor = primaryTextColor,
                        mutedTextColor = mutedTextColor,
                        accentColor = activeAccentColor,
                        isNightMode = isNightMode,
                        is24Hour = is24Hour
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarContent(
    monthData: CalendarMonthData,
    nextEvent: CalendarEventItem?,
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
    isNightMode: Boolean,
    is24Hour: Boolean
) {
    // 1. Month / Year Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = monthData.monthLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.5.sp,
            color = accentColor
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 2. Weekday Column Headers (7 cols)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        monthData.weekdayLabels.forEach { label ->
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = mutedTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // 3. Month Grid Rows (weeks of 7 days)
    val weeks = monthData.days.chunked(7)
    weeks.forEach { week ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            week.forEach { day ->
                DayCell(
                    day = day,
                    primaryTextColor = primaryTextColor,
                    mutedTextColor = mutedTextColor,
                    accentColor = accentColor,
                    isNightMode = isNightMode,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 4. Subtle Divider
    HorizontalDivider(
        color = if (isNightMode) StandByNightSurface else StandByBorder.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 5. Next Event Agenda Section
    UpcomingEventSection(
        nextEvent = nextEvent,
        primaryTextColor = primaryTextColor,
        mutedTextColor = mutedTextColor,
        accentColor = accentColor,
        is24Hour = is24Hour
    )
}

@Composable
private fun DayCell(
    day: CalendarDayItem,
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
    isNightMode: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = when {
        day.isToday -> if (isNightMode) Color.Black else Color.Black
        day.isCurrentMonth -> primaryTextColor
        else -> mutedTextColor.copy(alpha = 0.35f)
    }

    Column(
        modifier = modifier.height(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (day.isToday) accentColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                fontSize = 10.sp,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }

        // Event Dot Indicator
        if (day.hasEvent && day.isCurrentMonth && !day.isToday) {
            Box(
                modifier = Modifier
                    .size(2.5.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        } else {
            Spacer(modifier = Modifier.height(2.5.dp))
        }
    }
}

@Composable
private fun UpcomingEventSection(
    nextEvent: CalendarEventItem?,
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
    is24Hour: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_calendar),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(14.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (nextEvent != null) {
            val nowMillis = System.currentTimeMillis()
            val timeString = CalendarLogic.formatEventTime(
                event = nextEvent,
                nowMillis = nowMillis,
                is24Hour = is24Hour,
                locale = Locale.getDefault(),
                zoneId = ZoneId.systemDefault()
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nextEvent.displayTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeString,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    color = if (nextEvent.isOngoing) accentColor else mutedTextColor
                )
            }
        } else {
            Text(
                text = "No upcoming events",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                color = mutedTextColor
            )
        }
    }
}

@Composable
private fun CalendarLoadingState(mutedTextColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_calendar),
            contentDescription = null,
            tint = mutedTextColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Loading calendar...",
            fontSize = 12.sp,
            color = mutedTextColor
        )
    }
}

@Composable
private fun CalendarPermissionState(
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_calendar),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Calendar Access Required",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryTextColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Grant calendar access to display dates and upcoming agenda events.",
            fontSize = 11.sp,
            color = mutedTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onOpenSettings) {
            Text(
                text = "Grant Access",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun CalendarErrorState(
    message: String,
    primaryTextColor: Color,
    mutedTextColor: Color,
    accentColor: Color,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unable to load calendar",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryTextColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            fontSize = 11.sp,
            color = mutedTextColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(
                text = "Retry",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
