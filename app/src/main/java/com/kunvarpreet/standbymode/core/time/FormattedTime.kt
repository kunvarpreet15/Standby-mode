package com.kunvarpreet.standbymode.core.time

import java.time.Instant

/**
 * Immutable model representing formatted time components for clock display.
 */
data class FormattedTime(
    val hours: String,
    val minutes: String,
    val seconds: String?,
    val amPm: String?,
    val formattedDate: String,
    val timestamp: Instant = Instant.now()
)
