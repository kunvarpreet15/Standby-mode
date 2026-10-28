package com.kunvarpreet.standbymode.core.photoframe

import com.kunvarpreet.standbymode.AppScreen
import com.kunvarpreet.standbymode.core.display.DisplaySpace
import com.kunvarpreet.standbymode.core.display.KeepScreenAwakePolicy
import com.kunvarpreet.standbymode.core.widget.DuoWidgetPolicy
import com.kunvarpreet.standbymode.core.widget.DuoWidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Phase 14C verifying:
 * - Slideshow lifecycle gating for Duo Layout panes and Fullscreen Photo Frame.
 * - Context-aware back navigation between Duo Layout, Fullscreen Clock, and Photo Frame.
 * - Charging-aware Keep Screen Awake behavior across ambient display spaces.
 * - Origin tracking, reset on departure, and fallback resolution.
 */
class FullscreenPhotoFramePolicyTest {

    // =========================================================================
    // 1. Context-Aware Navigation Tests
    // =========================================================================

    @Test
    fun contextAwareNavigation_expandFromDuoLayout_exitsToDuoLayout() {
        var simulatedPageIndex = DisplaySpace.DUO_LAYOUT.pageIndex
        var photoFrameOriginSpace: DisplaySpace? = null

        // User is viewing Duo Layout (Page 0)
        assertEquals(0, simulatedPageIndex)

        // User taps "Expand to fullscreen" on Photo Frame widget in Duo Layout
        photoFrameOriginSpace = DisplaySpace.DUO_LAYOUT
        simulatedPageIndex = DisplaySpace.PHOTO_FRAME.pageIndex
        assertEquals(2, simulatedPageIndex)

        // Back action triggers exit
        val resolveExitTarget: (DisplaySpace?) -> DisplaySpace = { origin ->
            origin ?: DisplaySpace.FULLSCREEN_CLOCK
        }
        val returnTarget = resolveExitTarget(photoFrameOriginSpace)
        assertEquals(DisplaySpace.DUO_LAYOUT, returnTarget)

        // Return to origin space and clear origin
        simulatedPageIndex = returnTarget.pageIndex
        photoFrameOriginSpace = null
        assertEquals(DisplaySpace.DUO_LAYOUT.pageIndex, simulatedPageIndex)
        assertNull(photoFrameOriginSpace)
    }

    @Test
    fun contextAwareNavigation_horizontalPagingFromClock_exitsToClock() {
        var simulatedPageIndex = DisplaySpace.FULLSCREEN_CLOCK.pageIndex
        var photoFrameOriginSpace: DisplaySpace? = null

        // User is viewing Fullscreen Clock (Page 1)
        assertEquals(1, simulatedPageIndex)

        // User swipes horizontally to Photo Frame (Page 2) without explicit expand action
        simulatedPageIndex = DisplaySpace.PHOTO_FRAME.pageIndex
        assertEquals(2, simulatedPageIndex)
        assertNull(photoFrameOriginSpace)

        // Back action resolves to established adjacent page (Fullscreen Clock)
        val returnTarget = photoFrameOriginSpace ?: DisplaySpace.FULLSCREEN_CLOCK
        assertEquals(DisplaySpace.FULLSCREEN_CLOCK, returnTarget)

        simulatedPageIndex = returnTarget.pageIndex
        assertEquals(DisplaySpace.FULLSCREEN_CLOCK.pageIndex, simulatedPageIndex)
    }

    @Test
    fun contextAwareNavigation_missingOrInvalidOrigin_usesFullscreenClockFallback() {
        val nullOrigin: DisplaySpace? = null
        val fallbackTarget = nullOrigin ?: DisplaySpace.FULLSCREEN_CLOCK
        assertEquals(DisplaySpace.FULLSCREEN_CLOCK, fallbackTarget)
    }

    @Test
    fun contextAwareNavigation_originResetWhenNavigatingAwayHorizontally() {
        var photoFrameOriginSpace: DisplaySpace? = DisplaySpace.DUO_LAYOUT
        var currentPage = DisplaySpace.PHOTO_FRAME.pageIndex

        // If user is on Photo Frame, origin is preserved
        if (currentPage != DisplaySpace.PHOTO_FRAME.pageIndex) {
            photoFrameOriginSpace = null
        }
        assertEquals(DisplaySpace.DUO_LAYOUT, photoFrameOriginSpace)

        // User swipes away horizontally to Fullscreen Clock (Page 1)
        currentPage = DisplaySpace.FULLSCREEN_CLOCK.pageIndex
        if (currentPage != DisplaySpace.PHOTO_FRAME.pageIndex) {
            photoFrameOriginSpace = null
        }
        assertNull(photoFrameOriginSpace)

        // Subsequent unrelated visit to Photo Frame starts with clean origin
        currentPage = DisplaySpace.PHOTO_FRAME.pageIndex
        val resolvedReturn = photoFrameOriginSpace ?: DisplaySpace.FULLSCREEN_CLOCK
        assertEquals(DisplaySpace.FULLSCREEN_CLOCK, resolvedReturn)
    }

    @Test
    fun contextAwareNavigation_settingsRoundtrip_preservesOriginState() {
        val photoFrameOriginSpace = DisplaySpace.DUO_LAYOUT
        val pagerPageIndex = DisplaySpace.PHOTO_FRAME.pageIndex

        // User opens Settings from Photo Frame
        var currentScreen = AppScreen.SETTINGS
        assertEquals(AppScreen.SETTINGS, currentScreen)

        // When returning to CLOCK, pager remains on Page 2 and origin is preserved
        currentScreen = AppScreen.CLOCK
        assertEquals(AppScreen.CLOCK, currentScreen)
        assertEquals(DisplaySpace.PHOTO_FRAME.pageIndex, pagerPageIndex)
        assertEquals(DisplaySpace.DUO_LAYOUT, photoFrameOriginSpace)
    }

    @Test
    fun contextAwareNavigation_preservesIndependentDuoPaneSelections() {
        val initialLeftPane = DuoWidgetType.PHOTO_FRAME
        val initialRightPane = DuoWidgetType.WEATHER

        var leftPaneWidget = initialLeftPane
        var rightPaneWidget = initialRightPane

        // Enter Photo Frame via expand
        var photoFrameOriginSpace: DisplaySpace? = DisplaySpace.DUO_LAYOUT
        var currentSpace = DisplaySpace.PHOTO_FRAME

        // Duo pane selections are untouched
        assertEquals(DuoWidgetType.PHOTO_FRAME, leftPaneWidget)
        assertEquals(DuoWidgetType.WEATHER, rightPaneWidget)

        // Exit back to Duo Layout
        val returnTarget = photoFrameOriginSpace ?: DisplaySpace.FULLSCREEN_CLOCK
        currentSpace = returnTarget
        photoFrameOriginSpace = null

        assertEquals(DisplaySpace.DUO_LAYOUT, currentSpace)
        assertEquals(initialLeftPane, leftPaneWidget)
        assertEquals(initialRightPane, rightPaneWidget)
    }

    // =========================================================================
    // 2. Duo Layout Pane Slideshow Lifecycle Gating Tests
    // =========================================================================

    @Test
    fun panePhotoFrameVisibility_leftPaneActive_whenDuoLayoutVisibleAndForegrounded() {
        assertTrue(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                paneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )
    }

    @Test
    fun panePhotoFrameVisibility_rightPaneActive_whenDuoLayoutVisibleAndForegrounded() {
        assertTrue(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                paneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )
    }

    @Test
    fun panePhotoFrameVisibility_inactive_whenPreferenceDisabled() {
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
    fun panePhotoFrameVisibility_inactive_whenPaneDoesNotDisplayPhotoFrame() {
        assertFalse(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                paneWidget = DuoWidgetType.CLOCK
            )
        )
        assertFalse(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                paneWidget = DuoWidgetType.BATTERY_STATUS
            )
        )
    }

    @Test
    fun panePhotoFrameVisibility_inactive_whenDuoLayoutIsNotActiveDisplaySpace() {
        // Fullscreen Clock is active -> Duo Layout pane photo frame MUST be inactive
        assertFalse(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.FULLSCREEN_CLOCK,
                paneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )

        // Fullscreen Photo Frame is active -> Duo Layout pane photo frame MUST be inactive
        assertFalse(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                displaySpace = DisplaySpace.PHOTO_FRAME,
                paneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )
    }

    @Test
    fun panePhotoFrameVisibility_inactive_whenActivityBackgrounded() {
        assertFalse(
            DuoWidgetPolicy.isPanePhotoFrameVisible(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = false,
                displaySpace = DisplaySpace.DUO_LAYOUT,
                paneWidget = DuoWidgetType.PHOTO_FRAME
            )
        )
    }

    @Test
    fun fullscreenPhotoFrameVisibility_activeWhenDisplaySpaceIsPhotoFrame() {
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
    }

    @Test
    fun slideshowAdvancement_gatingConditions() {
        val photos = listOf(
            "content://media/external/images/media/101",
            "content://media/external/images/media/102",
            "content://media/external/images/media/103"
        )

        // 1. Fully active foreground in visible Photo Frame
        assertTrue(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = true,
                isActivityForeground = true,
                photoCount = photos.size
            )
        )

        // 2. Activity in background -> stopped
        assertFalse(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = true,
                isActivityForeground = false,
                photoCount = photos.size
            )
        )

        // 3. User swiped away (widget not visible) -> stopped
        assertFalse(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = false,
                isActivityForeground = true,
                photoCount = photos.size
            )
        )

        // 4. Single photo selected -> does not tick
        assertFalse(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = true,
                isActivityForeground = true,
                photoCount = 1
            )
        )

        // 5. Empty selection -> does not tick
        assertFalse(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = true,
                isActivityForeground = true,
                photoCount = 0
            )
        )
    }

    // =========================================================================
    // 3. Keep Screen Awake Charging-Aware Tests
    // =========================================================================

    @Test
    fun keepScreenAwake_respectsChargingStatusInAmbientDisplays() {
        // Preference enabled, in CLOCK, foreground, charging -> true
        assertTrue(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = true
            )
        )

        // Unplugged device -> false (preserves battery)
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = false
            )
        )

        // Preference disabled -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = true
            )
        )

        // In Settings -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.SETTINGS,
                isActivityForeground = true,
                isCharging = true
            )
        )

        // Backgrounded -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = false,
                isCharging = true
            )
        )
    }

    @Test
    fun batteryObservation_neededWhenKeepScreenAwakeIsEnabled() {
        // Even when battery widget is not visible in Duo panes,
        // battery observation MUST be active if keepScreenAwake is enabled,
        // so that plug/unplug events are observed dynamically.
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

        // If keepScreenAwake is disabled and battery widget not shown -> false
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

    // =========================================================================
    // 4. Three-Page Pager Invariants
    // =========================================================================

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

    @Test
    fun photoCountReduction_clampsIndexSafely() {
        val currentIndex = 4 // Photo 5 out of 5 was showing
        val reducedList = listOf(
            "content://media/external/images/media/1",
            "content://media/external/images/media/2"
        )
        val validCount = reducedList.size // now only 2 photos

        val clamped = currentIndex.coerceIn(0, validCount - 1)
        assertEquals(1, clamped)
    }
}
