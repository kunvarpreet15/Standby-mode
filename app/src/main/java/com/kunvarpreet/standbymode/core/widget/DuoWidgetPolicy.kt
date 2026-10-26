package com.kunvarpreet.standbymode.core.widget

import com.kunvarpreet.standbymode.AppScreen
import com.kunvarpreet.standbymode.core.display.DisplaySpace

/**
 * Pure policy rules governing Duo Layout widget stack availability,
 * deterministic initial selections, fallback recovery, and observer visibility decisions.
 */
object DuoWidgetPolicy {

    /**
     * Resolves the list of available widgets for a given pane based on whether media player, calendar, weather, and photo frame are enabled.
     * Index 0 always corresponds to the fresh-launch default for that pane.
     */
    fun getAvailableWidgets(
        isLeftPane: Boolean,
        showMediaPlayer: Boolean,
        showCalendarAgenda: Boolean = false,
        showWeatherWidget: Boolean = false,
        showPhotoFrame: Boolean = false
    ): List<DuoWidgetType> {
        return if (isLeftPane) {
            buildList {
                add(DuoWidgetType.CLOCK)
                if (showCalendarAgenda) add(DuoWidgetType.CALENDAR)
                if (showWeatherWidget) add(DuoWidgetType.WEATHER)
                add(DuoWidgetType.BATTERY_STATUS)
                if (showPhotoFrame) add(DuoWidgetType.PHOTO_FRAME)
                if (showMediaPlayer) add(DuoWidgetType.MEDIA)
            }
        } else {
            buildList {
                if (showMediaPlayer) {
                    add(DuoWidgetType.MEDIA)
                    if (showPhotoFrame) add(DuoWidgetType.PHOTO_FRAME)
                    if (showCalendarAgenda) add(DuoWidgetType.CALENDAR)
                    if (showWeatherWidget) add(DuoWidgetType.WEATHER)
                    add(DuoWidgetType.BATTERY_STATUS)
                    add(DuoWidgetType.CLOCK)
                } else {
                    if (showPhotoFrame) add(DuoWidgetType.PHOTO_FRAME)
                    add(DuoWidgetType.BATTERY_STATUS)
                    if (showCalendarAgenda) add(DuoWidgetType.CALENDAR)
                    if (showWeatherWidget) add(DuoWidgetType.WEATHER)
                    add(DuoWidgetType.CLOCK)
                }
            }
        }
    }

    /**
     * Resolves the deterministic fresh-install default widget for a given pane.
     */
    fun getDefaultWidget(
        isLeftPane: Boolean,
        showMediaPlayer: Boolean,
        showCalendarAgenda: Boolean = false,
        showWeatherWidget: Boolean = false,
        showPhotoFrame: Boolean = false
    ): DuoWidgetType {
        return if (isLeftPane) {
            DuoWidgetType.CLOCK
        } else {
            if (showMediaPlayer) DuoWidgetType.MEDIA else DuoWidgetType.BATTERY_STATUS
        }
    }

    /**
     * Ensures a pane's selected widget remains valid when available entries change.
     * Preserves current selection if still valid; otherwise falls back to pane default or first valid entry.
     */
    fun resolveValidSelection(
        current: DuoWidgetType,
        available: List<DuoWidgetType>,
        default: DuoWidgetType
    ): DuoWidgetType {
        if (available.isEmpty()) return DuoWidgetType.CLOCK
        if (available.contains(current)) return current
        if (available.contains(default)) return default
        return available.first()
    }

    /**
     * Evaluates whether the MediaMiniWidget is actively visible on the user's screen.
     * Media session observation is started only when this returns true.
     */
    fun isMediaVisible(
        isPreferenceEnabled: Boolean,
        currentScreen: AppScreen,
        isActivityForeground: Boolean,
        displaySpace: DisplaySpace,
        leftPaneWidget: DuoWidgetType,
        rightPaneWidget: DuoWidgetType
    ): Boolean {
        if (!isPreferenceEnabled || currentScreen != AppScreen.CLOCK || !isActivityForeground) {
            return false
        }
        return when (displaySpace) {
            DisplaySpace.FULLSCREEN_CLOCK -> true
            DisplaySpace.DUO_LAYOUT -> {
                leftPaneWidget == DuoWidgetType.MEDIA || rightPaneWidget == DuoWidgetType.MEDIA
            }
            DisplaySpace.PHOTO_FRAME -> false
        }
    }

    /**
     * Evaluates whether battery broadcast observation is needed.
     * True if the small battery pill is enabled or if either Duo pane is displaying the dedicated battery widget.
     */
    fun isBatteryObservationNeeded(
        showBatteryStatusPref: Boolean,
        currentScreen: AppScreen,
        isActivityForeground: Boolean,
        displaySpace: DisplaySpace,
        leftPaneWidget: DuoWidgetType,
        rightPaneWidget: DuoWidgetType
    ): Boolean {
        if (currentScreen != AppScreen.CLOCK || !isActivityForeground) {
            return false
        }
        if (showBatteryStatusPref) {
            return true
        }
        return displaySpace == DisplaySpace.DUO_LAYOUT && (
            leftPaneWidget == DuoWidgetType.BATTERY_STATUS ||
            rightPaneWidget == DuoWidgetType.BATTERY_STATUS
        )
    }

    /**
     * Evaluates whether the Calendar & Agenda widget is actively visible on the user's screen.
     * Content observer registration and queries are active only when this returns true.
     */
    fun isCalendarVisible(
        isPreferenceEnabled: Boolean,
        currentScreen: AppScreen,
        isActivityForeground: Boolean,
        displaySpace: DisplaySpace,
        leftPaneWidget: DuoWidgetType,
        rightPaneWidget: DuoWidgetType
    ): Boolean {
        if (!isPreferenceEnabled || currentScreen != AppScreen.CLOCK || !isActivityForeground) {
            return false
        }
        return displaySpace == DisplaySpace.DUO_LAYOUT && (
            leftPaneWidget == DuoWidgetType.CALENDAR || rightPaneWidget == DuoWidgetType.CALENDAR
        )
    }

    /**
     * Evaluates whether the Weather & Forecast widget is actively visible on the user's screen.
     * Location and forecast queries are active only when this returns true.
     */
    fun isWeatherVisible(
        isPreferenceEnabled: Boolean,
        currentScreen: AppScreen,
        isActivityForeground: Boolean,
        displaySpace: DisplaySpace,
        leftPaneWidget: DuoWidgetType,
        rightPaneWidget: DuoWidgetType
    ): Boolean {
        if (!isPreferenceEnabled || currentScreen != AppScreen.CLOCK || !isActivityForeground) {
            return false
        }
        return displaySpace == DisplaySpace.DUO_LAYOUT && (
            leftPaneWidget == DuoWidgetType.WEATHER || rightPaneWidget == DuoWidgetType.WEATHER
        )
    }

    /**
     * Evaluates whether the Photo Frame widget is actively visible on the user's screen.
     * Slideshow intervals and image decoding are active only when this returns true.
     */
    fun isPhotoFrameVisible(
        isPreferenceEnabled: Boolean,
        currentScreen: AppScreen,
        isActivityForeground: Boolean,
        displaySpace: DisplaySpace,
        leftPaneWidget: DuoWidgetType,
        rightPaneWidget: DuoWidgetType
    ): Boolean {
        if (!isPreferenceEnabled || !isActivityForeground) {
            return false
        }
        if (currentScreen == AppScreen.FULLSCREEN_PHOTO_FRAME) {
            return true
        }
        if (currentScreen != AppScreen.CLOCK) {
            return false
        }
        return when (displaySpace) {
            DisplaySpace.PHOTO_FRAME -> true
            DisplaySpace.DUO_LAYOUT -> {
                leftPaneWidget == DuoWidgetType.PHOTO_FRAME || rightPaneWidget == DuoWidgetType.PHOTO_FRAME
            }
            DisplaySpace.FULLSCREEN_CLOCK -> false
        }
    }
}
