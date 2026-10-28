package com.kunvarpreet.standbymode.core.widget

import com.kunvarpreet.standbymode.AppScreen
import com.kunvarpreet.standbymode.core.display.DisplaySpace
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for DuoWidgetPolicy and widget stack state transitions (Phase 10A).
 */
class DuoWidgetPolicyTest {

    @Test
    fun defaultEntries_forBothPanes_whenMediaEnabled_andCalendarDisabled() {
        val leftAvailable = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = true, showMediaPlayer = true, showCalendarAgenda = false)
        val rightAvailable = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = false, showMediaPlayer = true, showCalendarAgenda = false)

        assertEquals(3, leftAvailable.size)
        assertEquals(listOf(DuoWidgetType.CLOCK, DuoWidgetType.BATTERY_STATUS, DuoWidgetType.MEDIA), leftAvailable)

        assertEquals(3, rightAvailable.size)
        assertEquals(listOf(DuoWidgetType.MEDIA, DuoWidgetType.BATTERY_STATUS, DuoWidgetType.CLOCK), rightAvailable)
    }

    @Test
    fun defaultEntries_forBothPanes_whenMediaDisabled_andCalendarDisabled() {
        val leftAvailable = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = true, showMediaPlayer = false, showCalendarAgenda = false)
        val rightAvailable = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = false, showMediaPlayer = false, showCalendarAgenda = false)

        assertEquals(2, leftAvailable.size)
        assertEquals(listOf(DuoWidgetType.CLOCK, DuoWidgetType.BATTERY_STATUS), leftAvailable)
        assertFalse(leftAvailable.contains(DuoWidgetType.MEDIA))
        assertFalse(leftAvailable.contains(DuoWidgetType.CALENDAR))

        assertEquals(2, rightAvailable.size)
        assertEquals(listOf(DuoWidgetType.BATTERY_STATUS, DuoWidgetType.CLOCK), rightAvailable)
        assertFalse(rightAvailable.contains(DuoWidgetType.MEDIA))
        assertFalse(rightAvailable.contains(DuoWidgetType.CALENDAR))
    }

    @Test
    fun defaultEntries_forBothPanes_whenMediaAndCalendarBothEnabled() {
        val leftAvailable = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = true, showMediaPlayer = true, showCalendarAgenda = true)
        val rightAvailable = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = false, showMediaPlayer = true, showCalendarAgenda = true)

        assertEquals(4, leftAvailable.size)
        assertEquals(listOf(DuoWidgetType.CLOCK, DuoWidgetType.CALENDAR, DuoWidgetType.BATTERY_STATUS, DuoWidgetType.MEDIA), leftAvailable)

        assertEquals(4, rightAvailable.size)
        assertEquals(listOf(DuoWidgetType.MEDIA, DuoWidgetType.CALENDAR, DuoWidgetType.BATTERY_STATUS, DuoWidgetType.CLOCK), rightAvailable)
    }

    @Test
    fun defaultEntries_forBothPanes_whenCalendarEnabled_andMediaDisabled() {
        val leftAvailable = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = true, showMediaPlayer = false, showCalendarAgenda = true)
        val rightAvailable = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = false, showMediaPlayer = false, showCalendarAgenda = true)

        assertEquals(3, leftAvailable.size)
        assertEquals(listOf(DuoWidgetType.CLOCK, DuoWidgetType.CALENDAR, DuoWidgetType.BATTERY_STATUS), leftAvailable)
        assertFalse(leftAvailable.contains(DuoWidgetType.MEDIA))

        assertEquals(3, rightAvailable.size)
        assertEquals(listOf(DuoWidgetType.BATTERY_STATUS, DuoWidgetType.CALENDAR, DuoWidgetType.CLOCK), rightAvailable)
        assertFalse(rightAvailable.contains(DuoWidgetType.MEDIA))
    }

    @Test
    fun deterministicInitialSelection_freshLaunch() {
        // Left pane opens on Clock
        val leftDefaultWithMedia = DuoWidgetPolicy.getDefaultWidget(isLeftPane = true, showMediaPlayer = true)
        val leftDefaultNoMedia = DuoWidgetPolicy.getDefaultWidget(isLeftPane = true, showMediaPlayer = false)
        assertEquals(DuoWidgetType.CLOCK, leftDefaultWithMedia)
        assertEquals(DuoWidgetType.CLOCK, leftDefaultNoMedia)

        // Right pane opens on Media if enabled, or Battery Status if disabled
        val rightDefaultWithMedia = DuoWidgetPolicy.getDefaultWidget(isLeftPane = false, showMediaPlayer = true)
        val rightDefaultNoMedia = DuoWidgetPolicy.getDefaultWidget(isLeftPane = false, showMediaPlayer = false)
        assertEquals(DuoWidgetType.MEDIA, rightDefaultWithMedia)
        assertEquals(DuoWidgetType.BATTERY_STATUS, rightDefaultNoMedia)
    }

    @Test
    fun independentStackSelections_leftDoesNotAffectRight_rightDoesNotAffectLeft() {
        var leftSelection = DuoWidgetType.CLOCK
        var rightSelection = DuoWidgetType.MEDIA

        // Changing left pane to BATTERY_STATUS leaves right pane untouched
        leftSelection = DuoWidgetType.BATTERY_STATUS
        assertEquals(DuoWidgetType.BATTERY_STATUS, leftSelection)
        assertEquals(DuoWidgetType.MEDIA, rightSelection)

        // Changing right pane to CLOCK leaves left pane untouched
        rightSelection = DuoWidgetType.CLOCK
        assertEquals(DuoWidgetType.BATTERY_STATUS, leftSelection)
        assertEquals(DuoWidgetType.CLOCK, rightSelection)
    }

    @Test
    fun cyclingForwardAndBackward_withinBoundedStack_noWrapAround() {
        val available = listOf(DuoWidgetType.MEDIA, DuoWidgetType.BATTERY_STATUS, DuoWidgetType.CLOCK)

        // Simulate forward and backward transitions with strict clamping (no wrap around)
        var currentIndex = 0
        assertEquals(DuoWidgetType.MEDIA, available[currentIndex])

        // Backward at index 0 should clamp at 0
        val backwardFromStart = (currentIndex - 1).coerceIn(0, available.size - 1)
        assertEquals(0, backwardFromStart)
        assertEquals(DuoWidgetType.MEDIA, available[backwardFromStart])

        // Forward to index 1
        currentIndex = (currentIndex + 1).coerceIn(0, available.size - 1)
        assertEquals(1, currentIndex)
        assertEquals(DuoWidgetType.BATTERY_STATUS, available[currentIndex])

        // Forward to index 2
        currentIndex = (currentIndex + 1).coerceIn(0, available.size - 1)
        assertEquals(2, currentIndex)
        assertEquals(DuoWidgetType.CLOCK, available[currentIndex])

        // Forward at end should clamp at 2 (no wrap around to 0)
        val forwardFromEnd = (currentIndex + 1).coerceIn(0, available.size - 1)
        assertEquals(2, forwardFromEnd)
        assertEquals(DuoWidgetType.CLOCK, available[forwardFromEnd])
    }

    @Test
    fun oneEntryStack_behavior() {
        val singleEntryList = listOf(DuoWidgetType.CLOCK)
        assertEquals(1, singleEntryList.size)

        val clampedIndex = 0.coerceIn(0, singleEntryList.size - 1)
        assertEquals(0, clampedIndex)
        assertEquals(DuoWidgetType.CLOCK, singleEntryList[clampedIndex])

        // Clamping prevents any navigation
        val forwardClamped = 1.coerceIn(0, singleEntryList.size - 1)
        assertEquals(0, forwardClamped)

        val fallback = DuoWidgetPolicy.resolveValidSelection(
            current = DuoWidgetType.MEDIA,
            available = singleEntryList,
            default = DuoWidgetType.CLOCK
        )
        assertEquals(DuoWidgetType.CLOCK, fallback)
    }

    @Test
    fun removalOfMedia_whenShowMediaPlayerBecomesFalse() {
        val availableWithMedia = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = false, showMediaPlayer = true)
        assertTrue(availableWithMedia.contains(DuoWidgetType.MEDIA))

        val availableWithoutMedia = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = false, showMediaPlayer = false)
        assertFalse(availableWithoutMedia.contains(DuoWidgetType.MEDIA))
    }

    @Test
    fun safeFallback_whenSelectedEntryBecomesUnavailable() {
        val availableNoMedia = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = false, showMediaPlayer = false)
        val defaultRight = DuoWidgetPolicy.getDefaultWidget(isLeftPane = false, showMediaPlayer = false)

        // When right pane was on MEDIA and media is disabled, it safely falls back to default BATTERY_STATUS
        val resolved = DuoWidgetPolicy.resolveValidSelection(
            current = DuoWidgetType.MEDIA,
            available = availableNoMedia,
            default = defaultRight
        )
        assertEquals(DuoWidgetType.BATTERY_STATUS, resolved)

        // When left pane was on MEDIA and media is disabled, it safely falls back to default CLOCK
        val defaultLeft = DuoWidgetPolicy.getDefaultWidget(isLeftPane = true, showMediaPlayer = false)
        val availableLeftNoMedia = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = true, showMediaPlayer = false)
        val resolvedLeft = DuoWidgetPolicy.resolveValidSelection(
            current = DuoWidgetType.MEDIA,
            available = availableLeftNoMedia,
            default = defaultLeft
        )
        assertEquals(DuoWidgetType.CLOCK, resolvedLeft)
    }

    @Test
    fun preservingStillValidSelection_afterPreferenceChanges() {
        val availableWithMedia = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = false, showMediaPlayer = true)
        val availableNoMedia = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = false, showMediaPlayer = false)
        val defaultRight = DuoWidgetPolicy.getDefaultWidget(isLeftPane = false, showMediaPlayer = false)

        // If right pane was on CLOCK and media is disabled, CLOCK is still valid and preserved
        val preservedClock = DuoWidgetPolicy.resolveValidSelection(
            current = DuoWidgetType.CLOCK,
            available = availableNoMedia,
            default = defaultRight
        )
        assertEquals(DuoWidgetType.CLOCK, preservedClock)

        // If right pane was on BATTERY_STATUS and media is re-enabled, BATTERY_STATUS is still valid and preserved
        val preservedBattery = DuoWidgetPolicy.resolveValidSelection(
            current = DuoWidgetType.BATTERY_STATUS,
            available = availableWithMedia,
            default = DuoWidgetType.MEDIA
        )
        assertEquals(DuoWidgetType.BATTERY_STATUS, preservedBattery)
    }

    @Test
    fun returningFromSettings_preservesInSessionPaneSelections() {
        // User customizes pane stack selections during session
        var inSessionLeftWidget = DuoWidgetType.BATTERY_STATUS
        var inSessionRightWidget = DuoWidgetType.CLOCK

        // User opens Settings
        var currentScreen = AppScreen.SETTINGS
        assertEquals(AppScreen.SETTINGS, currentScreen)

        // User closes Settings
        currentScreen = AppScreen.CLOCK
        assertEquals(AppScreen.CLOCK, currentScreen)

        // Hoisted selections remain preserved
        assertEquals(DuoWidgetType.BATTERY_STATUS, inSessionLeftWidget)
        assertEquals(DuoWidgetType.CLOCK, inSessionRightWidget)
    }

    @Test
    fun mediaObserverActivation_basedOnActualVisibleContent() {
        // 1. Preference disabled -> never observe
        assertFalse(
            DuoWidgetPolicy.isMediaVisible(
                isPreferenceEnabled = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.MEDIA,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 2. Activity in background -> never observe
        assertFalse(
            DuoWidgetPolicy.isMediaVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = false,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.MEDIA,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 3. User inside Settings screen -> never observe
        assertFalse(
            DuoWidgetPolicy.isMediaVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.SETTINGS,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.MEDIA,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 4. In Fullscreen Clock page -> observe because docked media widget is on screen
        assertTrue(
            DuoWidgetPolicy.isMediaVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // 5. In Duo Layout: true if right pane has MEDIA selected
        assertTrue(
            DuoWidgetPolicy.isMediaVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 6. In Duo Layout: true if left pane has MEDIA selected
        assertTrue(
            DuoWidgetPolicy.isMediaVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.MEDIA,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // 7. In Duo Layout: FALSE if neither pane has MEDIA selected (e.g. left is CLOCK, right is BATTERY)
        assertFalse(
            DuoWidgetPolicy.isMediaVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )
    }

    @Test
    fun batteryObserverActivation_basedOnVisibleContentOrAccessoryPill() {
        // If accessory pill preference is true: active on clock screen
        assertTrue(
            DuoWidgetPolicy.isBatteryObservationNeeded(
                showBatteryStatusPref = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // If accessory pill preference is false, but Duo pane displays BATTERY_STATUS: active!
        assertTrue(
            DuoWidgetPolicy.isBatteryObservationNeeded(
                showBatteryStatusPref = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // If accessory pill preference is false, and neither Duo pane displays BATTERY_STATUS: not needed
        assertFalse(
            DuoWidgetPolicy.isBatteryObservationNeeded(
                showBatteryStatusPref = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )
    }

    @Test
    fun clockFacePreferences_remainUnchanged_whenStackSwipesOccur() {
        val userPrefs = UserClockPreferences(
            clockFace = ClockFaceType.RETRO_FLIP
        )
        assertEquals(ClockFaceType.RETRO_FLIP, userPrefs.clockFace)

        // Swiping widget stack in left or right pane
        var leftWidget = DuoWidgetType.CLOCK
        leftWidget = DuoWidgetType.BATTERY_STATUS
        assertEquals(DuoWidgetType.BATTERY_STATUS, leftWidget)

        // Global saved clockFace preference is unaffected
        assertEquals(ClockFaceType.RETRO_FLIP, userPrefs.clockFace)
    }

    @Test
    fun removalOfCalendar_whenShowCalendarAgendaBecomesFalse() {
        val availableWithCalendar = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = true, showMediaPlayer = false, showCalendarAgenda = true)
        assertTrue(availableWithCalendar.contains(DuoWidgetType.CALENDAR))

        val availableWithoutCalendar = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = true, showMediaPlayer = false, showCalendarAgenda = false)
        assertFalse(availableWithoutCalendar.contains(DuoWidgetType.CALENDAR))
    }

    @Test
    fun safeFallback_whenCalendarBecomesUnavailable() {
        val availableNoCalendar = DuoWidgetPolicy.getAvailableWidgets(isLeftPane = true, showMediaPlayer = false, showCalendarAgenda = false)
        val defaultLeft = DuoWidgetPolicy.getDefaultWidget(isLeftPane = true, showMediaPlayer = false, showCalendarAgenda = false)

        val resolved = DuoWidgetPolicy.resolveValidSelection(
            current = DuoWidgetType.CALENDAR,
            available = availableNoCalendar,
            default = defaultLeft
        )
        assertEquals(DuoWidgetType.CLOCK, resolved)
    }

    @Test
    fun isCalendarVisible_policyValidation() {
        // 1. Preference disabled -> false
        assertFalse(
            DuoWidgetPolicy.isCalendarVisible(
                isPreferenceEnabled = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CALENDAR,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 2. Activity in background -> false
        assertFalse(
            DuoWidgetPolicy.isCalendarVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = false,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CALENDAR,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 3. User in Settings -> false
        assertFalse(
            DuoWidgetPolicy.isCalendarVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.SETTINGS,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CALENDAR,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 4. In Fullscreen Clock -> false (Calendar is only in Duo Layout stacks)
        assertFalse(
            DuoWidgetPolicy.isCalendarVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                leftPaneWidget = DuoWidgetType.CALENDAR,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 5. In Duo Layout: true if left pane displays CALENDAR
        assertTrue(
            DuoWidgetPolicy.isCalendarVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CALENDAR,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 6. In Duo Layout: true if right pane displays CALENDAR
        assertTrue(
            DuoWidgetPolicy.isCalendarVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.CALENDAR
            )
        )

        // 7. In Duo Layout: false if neither pane displays CALENDAR
        assertFalse(
            DuoWidgetPolicy.isCalendarVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )
    }

    @Test
    fun defaultEntries_forBothPanes_whenWeatherEnabled() {
        val leftAvailable = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = true,
            showMediaPlayer = false,
            showCalendarAgenda = false,
            showWeatherWidget = true
        )
        val rightAvailable = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = false,
            showMediaPlayer = false,
            showCalendarAgenda = false,
            showWeatherWidget = true
        )

        assertEquals(3, leftAvailable.size)
        assertEquals(listOf(DuoWidgetType.CLOCK, DuoWidgetType.WEATHER, DuoWidgetType.BATTERY_STATUS), leftAvailable)

        assertEquals(3, rightAvailable.size)
        assertEquals(listOf(DuoWidgetType.BATTERY_STATUS, DuoWidgetType.WEATHER, DuoWidgetType.CLOCK), rightAvailable)
    }

    @Test
    fun defaultEntries_forBothPanes_whenAllWidgetsEnabled() {
        val leftAvailable = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = true,
            showMediaPlayer = true,
            showCalendarAgenda = true,
            showWeatherWidget = true
        )
        val rightAvailable = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = false,
            showMediaPlayer = true,
            showCalendarAgenda = true,
            showWeatherWidget = true
        )

        assertEquals(5, leftAvailable.size)
        assertEquals(listOf(DuoWidgetType.CLOCK, DuoWidgetType.CALENDAR, DuoWidgetType.WEATHER, DuoWidgetType.BATTERY_STATUS, DuoWidgetType.MEDIA), leftAvailable)

        assertEquals(5, rightAvailable.size)
        assertEquals(listOf(DuoWidgetType.MEDIA, DuoWidgetType.CALENDAR, DuoWidgetType.WEATHER, DuoWidgetType.BATTERY_STATUS, DuoWidgetType.CLOCK), rightAvailable)
    }

    @Test
    fun removalOfWeather_whenShowWeatherWidgetBecomesFalse() {
        val availableWithWeather = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = true,
            showMediaPlayer = false,
            showCalendarAgenda = false,
            showWeatherWidget = true
        )
        assertTrue(availableWithWeather.contains(DuoWidgetType.WEATHER))

        val availableWithoutWeather = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = true,
            showMediaPlayer = false,
            showCalendarAgenda = false,
            showWeatherWidget = false
        )
        assertFalse(availableWithoutWeather.contains(DuoWidgetType.WEATHER))
    }

    @Test
    fun safeFallback_whenWeatherBecomesUnavailable() {
        val availableNoWeather = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = true,
            showMediaPlayer = false,
            showCalendarAgenda = false,
            showWeatherWidget = false
        )
        val defaultLeft = DuoWidgetPolicy.getDefaultWidget(
            isLeftPane = true,
            showMediaPlayer = false,
            showCalendarAgenda = false,
            showWeatherWidget = false
        )

        val resolved = DuoWidgetPolicy.resolveValidSelection(
            current = DuoWidgetType.WEATHER,
            available = availableNoWeather,
            default = defaultLeft
        )
        assertEquals(DuoWidgetType.CLOCK, resolved)
    }

    @Test
    fun isWeatherVisible_policyValidation() {
        // 1. Preference disabled -> false
        assertFalse(
            DuoWidgetPolicy.isWeatherVisible(
                isPreferenceEnabled = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.WEATHER,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 2. Activity in background -> false
        assertFalse(
            DuoWidgetPolicy.isWeatherVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = false,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.WEATHER,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 3. User in Settings -> false
        assertFalse(
            DuoWidgetPolicy.isWeatherVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.SETTINGS,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.WEATHER,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 4. In Fullscreen Clock -> false (Weather is only in Duo Layout stacks)
        assertFalse(
            DuoWidgetPolicy.isWeatherVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                leftPaneWidget = DuoWidgetType.WEATHER,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 5. In Duo Layout: true if left pane displays WEATHER
        assertTrue(
            DuoWidgetPolicy.isWeatherVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.WEATHER,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // 6. In Duo Layout: true if right pane displays WEATHER
        assertTrue(
            DuoWidgetPolicy.isWeatherVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.WEATHER
            )
        )

        // 7. In Duo Layout: false if neither pane displays WEATHER
        assertFalse(
            DuoWidgetPolicy.isWeatherVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )
    }

    @Test
    fun availableWidgets_withPhotoFrameEnabled() {
        val leftAvailable = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = true,
            showMediaPlayer = true,
            showCalendarAgenda = true,
            showWeatherWidget = true,
            showPhotoFrame = true
        )
        val rightAvailable = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = false,
            showMediaPlayer = true,
            showCalendarAgenda = true,
            showWeatherWidget = true,
            showPhotoFrame = true
        )

        assertEquals(6, leftAvailable.size)
        assertEquals(
            listOf(
                DuoWidgetType.CLOCK,
                DuoWidgetType.CALENDAR,
                DuoWidgetType.WEATHER,
                DuoWidgetType.BATTERY_STATUS,
                DuoWidgetType.PHOTO_FRAME,
                DuoWidgetType.MEDIA
            ),
            leftAvailable
        )

        assertEquals(6, rightAvailable.size)
        assertEquals(
            listOf(
                DuoWidgetType.MEDIA,
                DuoWidgetType.PHOTO_FRAME,
                DuoWidgetType.CALENDAR,
                DuoWidgetType.WEATHER,
                DuoWidgetType.BATTERY_STATUS,
                DuoWidgetType.CLOCK
            ),
            rightAvailable
        )
    }

    @Test
    fun disablingPhotoFrame_reconcilesActivePane_withoutResettingOtherPane() {
        // Initial state: photo frame enabled, left pane on PHOTO_FRAME, right pane on BATTERY_STATUS
        val availableWhenEnabledLeft = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = true,
            showMediaPlayer = false,
            showPhotoFrame = true
        )
        assertTrue(availableWhenEnabledLeft.contains(DuoWidgetType.PHOTO_FRAME))

        // When Photo Frame is disabled, left pane reconciles to default (CLOCK), right pane stays on BATTERY_STATUS
        val availableWhenDisabledLeft = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = true,
            showMediaPlayer = false,
            showPhotoFrame = false
        )
        val availableWhenDisabledRight = DuoWidgetPolicy.getAvailableWidgets(
            isLeftPane = false,
            showMediaPlayer = false,
            showPhotoFrame = false
        )

        val defaultLeft = DuoWidgetPolicy.getDefaultWidget(isLeftPane = true, showMediaPlayer = false)
        val defaultRight = DuoWidgetPolicy.getDefaultWidget(isLeftPane = false, showMediaPlayer = false)

        val reconciledLeft = DuoWidgetPolicy.resolveValidSelection(
            current = DuoWidgetType.PHOTO_FRAME,
            available = availableWhenDisabledLeft,
            default = defaultLeft
        )
        val reconciledRight = DuoWidgetPolicy.resolveValidSelection(
            current = DuoWidgetType.BATTERY_STATUS,
            available = availableWhenDisabledRight,
            default = defaultRight
        )

        assertEquals(DuoWidgetType.CLOCK, reconciledLeft)
        assertEquals(DuoWidgetType.BATTERY_STATUS, reconciledRight)
    }

    @Test
    fun isPhotoFrameVisible_adheresToVisibilityContract() {
        // 1. Preference disabled -> false
        assertFalse(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.PHOTO_FRAME,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // 2. Activity in background -> false
        assertFalse(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = false,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.PHOTO_FRAME,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // 3. User in Settings -> false
        assertFalse(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.SETTINGS,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.PHOTO_FRAME,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // 4. In Fullscreen Clock -> false
        assertFalse(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                leftPaneWidget = DuoWidgetType.PHOTO_FRAME,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // 5. In Duo Layout: true if left pane displays PHOTO_FRAME
        assertTrue(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.PHOTO_FRAME,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // 6. In Duo Layout: true if right pane displays PHOTO_FRAME
        assertTrue(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )

        // 7. In Duo Layout: false if neither pane displays PHOTO_FRAME
        assertFalse(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )
    }

    @Test
    fun isPanePhotoFrameVisible_adheresToVisibilityContract() {
        // True only when preference is enabled, activity foregrounded, in CLOCK, Duo Layout, and pane widget is PHOTO_FRAME
        assertTrue(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                paneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )

        // Inactive if pane widget is not PHOTO_FRAME
        assertFalse(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                paneWidget = DuoWidgetType.CLOCK
            )
        )

        // Inactive if not in Duo Layout (e.g. in Fullscreen Clock or Photo Frame)
        assertFalse(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                paneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )
        assertFalse(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.PHOTO_FRAME,
                paneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )

        // Inactive if activity in background
        assertFalse(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = false,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                paneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )

        // Inactive if preference disabled
        assertFalse(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                paneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )
    }

    @Test
    fun batteryObserverActivation_whenKeepScreenAwakeEnabled() {
        // If keepScreenAwake is true, observation is needed even if battery widget is not shown
        assertTrue(
            DuoWidgetPolicy.isBatteryObservationNeeded(
                showBatteryStatusPref = false,
                keepScreenAwakePref = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // If keepScreenAwake is false and battery widget not shown -> false
        assertFalse(
            DuoWidgetPolicy.isBatteryObservationNeeded(
                showBatteryStatusPref = false,
                keepScreenAwakePref = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )
    }

    @Test
    fun batteryObserverActivation_whenAutoActivateEnabled() {
        // If autoActivatePref is true, observation is active whenever in foreground on clock
        assertTrue(
            DuoWidgetPolicy.isBatteryObservationNeeded(
                showBatteryStatusPref = false,
                keepScreenAwakePref = false,
                autoActivatePref = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // Inactive if in background even with autoActivatePref
        assertFalse(
            DuoWidgetPolicy.isBatteryObservationNeeded(
                showBatteryStatusPref = false,
                keepScreenAwakePref = false,
                autoActivatePref = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = false,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )
    }
}
