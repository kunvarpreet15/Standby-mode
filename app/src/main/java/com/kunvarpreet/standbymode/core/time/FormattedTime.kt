package com.kunvarpreet.standbymode.core.time

/**
 * Immutable model representing formatted time components for clock display.
 */
data class FormattedTime(
    val hours: String,
    val minutes: String,
    val seconds: String?,
    val amPm: String?,
    val formattedDate: String
)
