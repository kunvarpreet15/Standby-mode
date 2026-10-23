package com.kunvarpreet.standbymode.core.widget

/**
 * Built-in widget types supported in the Duo Layout widget stacks.
 */
enum class DuoWidgetType(
    val id: String,
    val displayName: String,
    val accessibilityName: String
) {
    CLOCK(
        id = "clock",
        displayName = "Clock",
        accessibilityName = "Clock widget"
    ),
    BATTERY_STATUS(
        id = "battery",
        displayName = "Battery",
        accessibilityName = "Battery status widget"
    ),
    MEDIA(
        id = "media",
        displayName = "Media",
        accessibilityName = "Media player widget"
    ),
    CALENDAR(
        id = "calendar",
        displayName = "Calendar",
        accessibilityName = "Calendar and agenda widget"
    );

    companion object {
        fun fromId(id: String): DuoWidgetType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CLOCK
        }
    }
}
