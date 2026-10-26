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
    ),
    WEATHER(
        id = "weather",
        displayName = "Weather",
        accessibilityName = "Weather and forecast widget"
    ),
    PHOTO_FRAME(
        id = "photo_frame",
        displayName = "Photo Frame",
        accessibilityName = "Photo frame widget"
    );

    companion object {
        fun fromId(id: String): DuoWidgetType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CLOCK
        }
    }
}
