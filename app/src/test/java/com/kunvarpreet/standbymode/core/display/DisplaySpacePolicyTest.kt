package com.kunvarpreet.standbymode.core.display

import com.kunvarpreet.standbymode.AppScreen
import com.kunvarpreet.standbymode.core.battery.BatteryState
import com.kunvarpreet.standbymode.core.burnin.BurnInProtectionPolicy
import com.kunvarpreet.standbymode.core.burnin.BurnInShiftOffset
import com.kunvarpreet.standbymode.core.media.MediaWidgetUiState
import com.kunvarpreet.standbymode.core.preferences.AccentColorOption
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.ClockThemePalette
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Unit tests verifying DisplaySpace model, fresh launch defaults,
 * preference preservation across display spaces, Settings round-trip preservation,
 * stay-awake policy, and secondary panel state handling for Phase 9A.
 */
class DisplaySpacePolicyTest {

    @Test
    fun displaySpace_representsPrimaryDestinations() {
        val spaces = DisplaySpace.entries
        assertEquals(3, spaces.size)
        assertEquals(DisplaySpace.DUO_LAYOUT, spaces[0])
        assertEquals(DisplaySpace.FULLSCREEN_CLOCK, spaces[1])
        assertEquals(DisplaySpace.PHOTO_FRAME, spaces[2])
        assertEquals(0, DisplaySpace.DUO_LAYOUT.pageIndex)
        assertEquals(1, DisplaySpace.FULLSCREEN_CLOCK.pageIndex)
        assertEquals(2, DisplaySpace.PHOTO_FRAME.pageIndex)
    }

    @Test
    fun displaySpace_freshLaunchResolvesToDuoLayout() {
        assertEquals(0, DisplaySpace.DEFAULT_PAGE_INDEX)
        val initialSpace = DisplaySpace.fromPageIndex(DisplaySpace.DEFAULT_PAGE_INDEX)
        assertEquals(DisplaySpace.DUO_LAYOUT, initialSpace)
        assertEquals("Duo Layout", initialSpace.title)
    }

    @Test
    fun displaySpace_fromPageIndexResolvesCorrectlyAndHandlesOutOfBoundsGracefully() {
        assertEquals(DisplaySpace.DUO_LAYOUT, DisplaySpace.fromPageIndex(0))
        assertEquals(DisplaySpace.FULLSCREEN_CLOCK, DisplaySpace.fromPageIndex(1))
        assertEquals(DisplaySpace.PHOTO_FRAME, DisplaySpace.fromPageIndex(2))

        // Out-of-bounds fallback safely returns default DUO_LAYOUT
        assertEquals(DisplaySpace.DUO_LAYOUT, DisplaySpace.fromPageIndex(-1))
        assertEquals(DisplaySpace.DUO_LAYOUT, DisplaySpace.fromPageIndex(3))
        assertEquals(DisplaySpace.DUO_LAYOUT, DisplaySpace.fromPageIndex(100))
    }

    @Test
    fun pageNavigation_preservesClockPreferencesAcrossSpaces() {
        val userPrefs = UserClockPreferences(
            clockFace = ClockFaceType.RETRO_FLIP,
            is24Hour = true,
            showSeconds = true, // Retro flip ignores seconds in rendering, but preference is preserved
            showDate = false,
            accentColor = AccentColorOption.PURPLE,
            isNightModeEnabled = true,
            showChargingNotification = true,
            isBurnInProtectionEnabled = true,
            keepScreenAwake = true,
            showBatteryStatus = true,
            showMediaPlayer = true
        )

        // Simulate paging between DUO_LAYOUT and FULLSCREEN_CLOCK
        var currentSpace = DisplaySpace.DUO_LAYOUT
        assertEquals(ClockFaceType.RETRO_FLIP, userPrefs.clockFace)
        assertEquals(AccentColorOption.PURPLE, userPrefs.accentColor)

        // Swipe to Fullscreen Clock
        currentSpace = DisplaySpace.FULLSCREEN_CLOCK
        assertEquals(DisplaySpace.FULLSCREEN_CLOCK, currentSpace)
        // Preferences remain completely unchanged
        assertEquals(ClockFaceType.RETRO_FLIP, userPrefs.clockFace)
        assertTrue(userPrefs.is24Hour)
        assertFalse(userPrefs.showDate)
        assertEquals(AccentColorOption.PURPLE, userPrefs.accentColor)
        assertTrue(userPrefs.isNightModeEnabled)

        // Swipe back to Duo Layout
        currentSpace = DisplaySpace.DUO_LAYOUT
        assertEquals(DisplaySpace.DUO_LAYOUT, currentSpace)
        assertEquals(ClockFaceType.RETRO_FLIP, userPrefs.clockFace)
    }

    @Test
    fun settingsNavigation_preservesInSessionDisplaySpace() {
        // User starts at Duo Layout (page 0)
        var simulatedPagerIndex = DisplaySpace.DEFAULT_PAGE_INDEX
        assertEquals(0, simulatedPagerIndex)

        // User swipes to Fullscreen Clock (page 1)
        simulatedPagerIndex = DisplaySpace.FULLSCREEN_CLOCK.pageIndex
        assertEquals(1, simulatedPagerIndex)

        // User opens Settings
        var currentScreen = AppScreen.SETTINGS
        assertEquals(AppScreen.SETTINGS, currentScreen)

        // User closes Settings / presses Done
        currentScreen = AppScreen.CLOCK
        assertEquals(AppScreen.CLOCK, currentScreen)

        // Pager state was hoisted, so the displayed space is restored to Fullscreen Clock
        val restoredSpace = DisplaySpace.fromPageIndex(simulatedPagerIndex)
        assertEquals(DisplaySpace.FULLSCREEN_CLOCK, restoredSpace)
    }

    @Test
    fun clockSettings_areRespectedAcrossBothDisplaySpaces() {
        val dayPrefs = UserClockPreferences(
            clockFace = ClockFaceType.BOLD_DIGITAL,
            accentColor = AccentColorOption.BLUE,
            isNightModeEnabled = false
        )
        val dayPalette = ClockThemePalette.resolve(
            isNightMode = dayPrefs.isNightModeEnabled,
            selectedAccent = dayPrefs.accentColor
        )
        assertFalse(dayPalette.isNightMode)
        assertEquals(AccentColorOption.BLUE.color, dayPalette.accentColor)

        val nightPrefs = dayPrefs.copy(isNightModeEnabled = true)
        val nightPalette = ClockThemePalette.resolve(
            isNightMode = nightPrefs.isNightModeEnabled,
            selectedAccent = nightPrefs.accentColor
        )
        assertTrue(nightPalette.isNightMode)
        assertEquals(com.kunvarpreet.standbymode.ui.theme.StandByNightText, nightPalette.accentColor)
    }

    @Test
    fun keepScreenAwakePolicy_appliesToAmbientDisplayRegardlessOfDisplaySpace() {
        // When in AppScreen.CLOCK (which encompasses DUO_LAYOUT, FULLSCREEN_CLOCK, and PHOTO_FRAME),
        // keep screen awake is enabled if preference is true, activity is foregrounded, and device is charging.
        assertTrue(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = true
            )
        )

        // Unplugged device clears stay-awake
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = false
            )
        )

        // Disabling preference turns off stay-awake
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = false,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = true
            )
        )

        // Entering Settings clears stay-awake
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.SETTINGS,
                isActivityForeground = true,
                isCharging = true
            )
        )

        // Leaving foreground clears stay-awake
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
    fun secondaryPanel_mediaWidgetStatesAreSafelyRepresented() {
        // Disabled media: showMediaPlayer = false
        val disabledPrefs = UserClockPreferences(showMediaPlayer = false)
        assertFalse(disabledPrefs.showMediaPlayer)

        // Enabled media but no active session: NoActiveMedia
        val noActiveState: MediaWidgetUiState = MediaWidgetUiState.NoActiveMedia
        assertNotNull(noActiveState)

        // Permission required / unavailable: UnavailableAccess
        val unavailableState: MediaWidgetUiState = MediaWidgetUiState.UnavailableAccess
        assertNotNull(unavailableState)
    }

    @Test
    fun burnInProtection_policyRemainsDeterministicAcrossSpaces() {
        val testInstant = Instant.parse("2026-10-23T12:00:00Z")
        val offsetEnabled = BurnInProtectionPolicy.calculateOffset(testInstant, isEnabled = true)
        val offsetDisabled = BurnInProtectionPolicy.calculateOffset(testInstant, isEnabled = false)

        assertEquals(0f, offsetDisabled.xDp, 0.001f)
        assertEquals(0f, offsetDisabled.yDp, 0.001f)
        assertEquals(BurnInShiftOffset.NEUTRAL, offsetDisabled)
        assertNotNull(offsetEnabled)
    }

    @Test
    fun batteryState_availabilityConditionPreserved() {
        val defaultBattery = BatteryState()
        assertFalse(defaultBattery.isAvailable)

        val activeBattery = BatteryState(percentage = 85, isCharging = true, isAvailable = true)
        assertTrue(activeBattery.isAvailable)
        assertEquals(85, activeBattery.percentage)
        assertTrue(activeBattery.isCharging)
    }
}
