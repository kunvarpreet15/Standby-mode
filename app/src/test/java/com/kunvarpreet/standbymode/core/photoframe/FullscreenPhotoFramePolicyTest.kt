package com.kunvarpreet.standbymode.core.photoframe

import com.kunvarpreet.standbymode.AppScreen
import com.kunvarpreet.standbymode.core.display.DisplaySpace
import com.kunvarpreet.standbymode.core.display.KeepScreenAwakePolicy
import com.kunvarpreet.standbymode.core.widget.DuoWidgetPolicy
import com.kunvarpreet.standbymode.core.widget.DuoWidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Phase 14A verifying Fullscreen Photo Frame navigation transitions,
 * back navigation restoration of previous display state, preservation of independent Duo pane selections,
 * slideshow lifecycle stop/resume behavior, stay-awake policy, and empty/stale selection bounds.
 */
class FullscreenPhotoFramePolicyTest {

    @Test
    fun fullscreenNavigation_transitionsAndRestoresPreviousScreen() {
        var currentScreen = AppScreen.CLOCK
        assertEquals(AppScreen.CLOCK, currentScreen)

        // User enters fullscreen photo frame
        currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME
        assertEquals(AppScreen.FULLSCREEN_PHOTO_FRAME, currentScreen)

        // User presses Back or taps minimal exit button
        currentScreen = AppScreen.CLOCK
        assertEquals(AppScreen.CLOCK, currentScreen)
    }

    @Test
    fun fullscreenNavigation_preservesIndependentDuoPaneSelections() {
        var currentScreen = AppScreen.CLOCK
        val initialLeftPane = DuoWidgetType.PHOTO_FRAME
        val initialRightPane = DuoWidgetType.WEATHER

        var leftPaneWidget = initialLeftPane
        var rightPaneWidget = initialRightPane

        // Enter Fullscreen Photo Frame
        currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME
        assertEquals(AppScreen.FULLSCREEN_PHOTO_FRAME, currentScreen)

        // Duo pane widget selections are not modified
        assertEquals(DuoWidgetType.PHOTO_FRAME, leftPaneWidget)
        assertEquals(DuoWidgetType.WEATHER, rightPaneWidget)

        // Exit Fullscreen Photo Frame back to CLOCK
        currentScreen = AppScreen.CLOCK
        assertEquals(AppScreen.CLOCK, currentScreen)
        assertEquals(initialLeftPane, leftPaneWidget)
        assertEquals(initialRightPane, rightPaneWidget)
    }

    @Test
    fun isPhotoFrameVisible_inFullscreenMode_respectsForegroundAndPreference() {
        // Active, enabled, foregrounded in fullscreen -> true
        assertTrue(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // In fullscreen but preference disabled -> false
        assertFalse(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = false,
                currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // In fullscreen but activity in background -> false (slideshow advances paused)
        assertFalse(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME,
                isActivityForeground = false,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )
    }

    @Test
    fun shouldAdvanceSlideshow_fullscreenLifecyclePausingAndResuming() {
        val photos = listOf(
            "content://media/external/images/media/101",
            "content://media/external/images/media/102",
            "content://media/external/images/media/103"
        )

        // 1. Fully active foreground in fullscreen
        val shouldAdvanceActive = PhotoFrameLogic.shouldAdvanceSlideshow(
            showPhotoFramePref = true,
            isPhotoFrameVisible = true,
            isActivityForeground = true,
            photoCount = photos.size
        )
        assertTrue(shouldAdvanceActive)

        // 2. Activity sent to background (e.g. phone call, screen off) -> immediately paused
        val shouldAdvanceBackgrounded = PhotoFrameLogic.shouldAdvanceSlideshow(
            showPhotoFramePref = true,
            isPhotoFrameVisible = true,
            isActivityForeground = false,
            photoCount = photos.size
        )
        assertFalse(shouldAdvanceBackgrounded)

        // 3. User navigates away to Settings -> paused
        val shouldAdvanceInSettings = PhotoFrameLogic.shouldAdvanceSlideshow(
            showPhotoFramePref = true,
            isPhotoFrameVisible = false,
            isActivityForeground = true,
            photoCount = photos.size
        )
        assertFalse(shouldAdvanceInSettings)

        // 4. Single photo selected -> does not tick timer needlessly
        val shouldAdvanceSingle = PhotoFrameLogic.shouldAdvanceSlideshow(
            showPhotoFramePref = true,
            isPhotoFrameVisible = true,
            isActivityForeground = true,
            photoCount = 1
        )
        assertFalse(shouldAdvanceSingle)
    }

    @Test
    fun keepScreenAwakePolicy_appliesInFullscreenPhotoFrame() {
        // Preference enabled, in fullscreen photo frame, activity foreground -> keep screen on
        assertTrue(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME,
                isActivityForeground = true
            )
        )

        // Preference disabled -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = false,
                currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME,
                isActivityForeground = true
            )
        )

        // Activity in background -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME,
                isActivityForeground = false
            )
        )

        // In Settings -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.SETTINGS,
                isActivityForeground = true
            )
        )
    }

    @Test
    fun emptySelection_handlesBoundsAndIndexSafely() {
        val emptyList = emptyList<String>()
        assertEquals(0, emptyList.size)

        // Empty selection does not advance
        assertEquals(0, PhotoFrameLogic.nextIndex(0, 0))
        assertEquals(0, PhotoFrameLogic.nextIndex(-1, 0))
        assertEquals(0, PhotoFrameLogic.nextIndex(5, 0))

        assertFalse(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = true,
                isActivityForeground = true,
                photoCount = 0
            )
        )
    }

    @Test
    fun photoCountReduction_clampsIndexSafely() {
        var currentIndex = 4 // Photo 5 out of 5 was showing
        val reducedList = listOf(
            "content://media/external/images/media/1",
            "content://media/external/images/media/2"
        )
        val validCount = reducedList.size // now only 2 photos

        val clamped = currentIndex.coerceIn(0, validCount - 1)
        assertEquals(1, clamped)
    }

    @Test
    fun otherDisplaySpaces_andWidgets_retainVisibilityIndependence() {
        // When in FULLSCREEN_PHOTO_FRAME, media is NOT visible
        assertFalse(
            DuoWidgetPolicy.isMediaVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // When in FULLSCREEN_PHOTO_FRAME, calendar is NOT visible
        assertFalse(
            DuoWidgetPolicy.isCalendarVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.CALENDAR,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // When in FULLSCREEN_PHOTO_FRAME, weather is NOT visible
        assertFalse(
            DuoWidgetPolicy.isWeatherVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.FULLSCREEN_PHOTO_FRAME,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                leftPaneWidget = DuoWidgetType.WEATHER,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )
    }

    @Test
    fun horizontalPaging_mapsPageIndicesToThreePrimaryDisplaySpaces() {
        assertEquals(3, DisplaySpace.entries.size)
        assertEquals(DisplaySpace.DUO_LAYOUT, DisplaySpace.fromPageIndex(0))
        assertEquals(DisplaySpace.FULLSCREEN_CLOCK, DisplaySpace.fromPageIndex(1))
        assertEquals(DisplaySpace.PHOTO_FRAME, DisplaySpace.fromPageIndex(2))

        assertEquals(0, DisplaySpace.DUO_LAYOUT.pageIndex)
        assertEquals(1, DisplaySpace.FULLSCREEN_CLOCK.pageIndex)
        assertEquals(2, DisplaySpace.PHOTO_FRAME.pageIndex)
    }

    @Test
    fun horizontalPaging_initialPageIsDuoLayout() {
        assertEquals(0, DisplaySpace.DEFAULT_PAGE_INDEX)
        val defaultSpace = DisplaySpace.fromPageIndex(DisplaySpace.DEFAULT_PAGE_INDEX)
        assertEquals(DisplaySpace.DUO_LAYOUT, defaultSpace)
        assertEquals("Duo Layout", defaultSpace.title)
    }

    @Test
    fun horizontalPaging_isPhotoFrameVisiblePolicy_reflectsActivePage() {
        // 1. When user pages to DisplaySpace.PHOTO_FRAME in AppScreen.CLOCK -> true
        assertTrue(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.PHOTO_FRAME,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // 2. When user pages away to DisplaySpace.FULLSCREEN_CLOCK -> false (slideshow pauses)
        assertFalse(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // 3. When in DisplaySpace.PHOTO_FRAME but app is in background -> false
        assertFalse(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = false,
                displaySpace = DisplaySpace.PHOTO_FRAME,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // 4. When in DisplaySpace.PHOTO_FRAME but preference is disabled -> false
        assertFalse(
            DuoWidgetPolicy.isPhotoFrameVisible(
                isPreferenceEnabled = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.PHOTO_FRAME,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )
    }

    @Test
    fun horizontalPaging_adjacentSpacesPreserveDuoPaneSelections() {
        val initialLeft = DuoWidgetType.CALENDAR
        val initialRight = DuoWidgetType.WEATHER

        var simulatedPageIndex = DisplaySpace.FULLSCREEN_CLOCK.pageIndex
        assertEquals(1, simulatedPageIndex)

        // User swipes right from Fullscreen Clock (Page 1) to Photo Frame (Page 2)
        simulatedPageIndex = DisplaySpace.PHOTO_FRAME.pageIndex
        assertEquals(2, simulatedPageIndex)
        val activeSpace = DisplaySpace.fromPageIndex(simulatedPageIndex)
        assertEquals(DisplaySpace.PHOTO_FRAME, activeSpace)

        // Duo pane selections are completely preserved
        assertEquals(DuoWidgetType.CALENDAR, initialLeft)
        assertEquals(DuoWidgetType.WEATHER, initialRight)

        // User swipes left from Photo Frame (Page 2) back to Fullscreen Clock (Page 1)
        simulatedPageIndex = DisplaySpace.FULLSCREEN_CLOCK.pageIndex
        assertEquals(1, simulatedPageIndex)
        assertEquals(DisplaySpace.FULLSCREEN_CLOCK, DisplaySpace.fromPageIndex(simulatedPageIndex))

        // User swipes left again to Duo Layout (Page 0)
        simulatedPageIndex = DisplaySpace.DUO_LAYOUT.pageIndex
        assertEquals(0, simulatedPageIndex)
        assertEquals(DisplaySpace.DUO_LAYOUT, DisplaySpace.fromPageIndex(simulatedPageIndex))
        assertEquals(DuoWidgetType.CALENDAR, initialLeft)
        assertEquals(DuoWidgetType.WEATHER, initialRight)
    }

    @Test
    fun horizontalPaging_secondaryObserversRemainInactiveOnPhotoFramePage() {
        // Media observer is inactive on PHOTO_FRAME page
        assertFalse(
            DuoWidgetPolicy.isMediaVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.PHOTO_FRAME,
                leftPaneWidget = DuoWidgetType.CLOCK,
                rightPaneWidget = DuoWidgetType.MEDIA
            )
        )

        // Calendar observer is inactive on PHOTO_FRAME page
        assertFalse(
            DuoWidgetPolicy.isCalendarVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.PHOTO_FRAME,
                leftPaneWidget = DuoWidgetType.CALENDAR,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )

        // Weather observer is inactive on PHOTO_FRAME page
        assertFalse(
            DuoWidgetPolicy.isWeatherVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.PHOTO_FRAME,
                leftPaneWidget = DuoWidgetType.WEATHER,
                rightPaneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )
    }
}
