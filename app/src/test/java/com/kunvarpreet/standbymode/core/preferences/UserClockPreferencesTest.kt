package com.kunvarpreet.standbymode.core.preferences

import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary
import org.junit.Assert.assertEquals
import org.junit.Test

class UserClockPreferencesTest {

    @Test
    fun defaultPreferences_matches_mvp_specification() {
        val prefs = UserClockPreferences()
        assertEquals(ClockFaceType.BOLD_DIGITAL, prefs.clockFace)
        assertEquals(false, prefs.is24Hour)
        assertEquals(false, prefs.showSeconds)
        assertEquals(true, prefs.showDate)
        assertEquals(AccentColorOption.ORANGE, prefs.accentColor)
        assertEquals(false, prefs.isNightModeEnabled)
        assertEquals(false, prefs.showChargingNotification)
        assertEquals(true, prefs.isBurnInProtectionEnabled)
        assertEquals(true, prefs.keepScreenAwake)
        assertEquals(false, prefs.showBatteryStatus)
        assertEquals(false, prefs.showMediaPlayer)
        assertEquals(false, prefs.showCalendarAgenda)
        assertEquals(false, prefs.showWeatherWidget)
    }

    @Test
    fun showWeatherWidget_toggling_preservesOtherPreferences() {
        val initial = UserClockPreferences(
            clockFace = ClockFaceType.ANALOG,
            is24Hour = true,
            showSeconds = true,
            showDate = false,
            accentColor = AccentColorOption.PURPLE,
            isNightModeEnabled = true,
            showChargingNotification = true,
            isBurnInProtectionEnabled = false,
            keepScreenAwake = true,
            showBatteryStatus = true,
            showMediaPlayer = true,
            showCalendarAgenda = true,
            showWeatherWidget = false
        )
        val toggled = initial.copy(showWeatherWidget = true)

        assertEquals(true, toggled.showWeatherWidget)
        assertEquals(ClockFaceType.ANALOG, toggled.clockFace)
        assertEquals(true, toggled.is24Hour)
        assertEquals(true, toggled.showSeconds)
        assertEquals(false, toggled.showDate)
        assertEquals(AccentColorOption.PURPLE, toggled.accentColor)
        assertEquals(true, toggled.isNightModeEnabled)
        assertEquals(true, toggled.showChargingNotification)
        assertEquals(false, toggled.isBurnInProtectionEnabled)
        assertEquals(true, toggled.keepScreenAwake)
        assertEquals(true, toggled.showBatteryStatus)
        assertEquals(true, toggled.showMediaPlayer)
        assertEquals(true, toggled.showCalendarAgenda)
    }

    @Test
    fun showWeatherWidget_remainsCompletelyIndependentOfOtherFeatures() {
        val initial = UserClockPreferences()

        val weatherEnabled = initial.copy(showWeatherWidget = true)
        assertEquals(true, weatherEnabled.showWeatherWidget)
        assertEquals(false, weatherEnabled.showCalendarAgenda)
        assertEquals(false, weatherEnabled.showMediaPlayer)
        assertEquals(false, weatherEnabled.showBatteryStatus)
        assertEquals(false, weatherEnabled.showChargingNotification)
        assertEquals(false, weatherEnabled.isNightModeEnabled)
        assertEquals(true, weatherEnabled.keepScreenAwake)
        assertEquals(true, weatherEnabled.isBurnInProtectionEnabled)
    }

    @Test
    fun showCalendarAgenda_toggling_preservesOtherPreferences() {
        val initial = UserClockPreferences(
            clockFace = ClockFaceType.ANALOG,
            is24Hour = true,
            showSeconds = true,
            showDate = false,
            accentColor = AccentColorOption.PURPLE,
            isNightModeEnabled = true,
            showChargingNotification = true,
            isBurnInProtectionEnabled = false,
            keepScreenAwake = true,
            showBatteryStatus = true,
            showMediaPlayer = true,
            showCalendarAgenda = false
        )
        val toggled = initial.copy(showCalendarAgenda = true)

        assertEquals(true, toggled.showCalendarAgenda)
        assertEquals(ClockFaceType.ANALOG, toggled.clockFace)
        assertEquals(true, toggled.is24Hour)
        assertEquals(true, toggled.showSeconds)
        assertEquals(false, toggled.showDate)
        assertEquals(AccentColorOption.PURPLE, toggled.accentColor)
        assertEquals(true, toggled.isNightModeEnabled)
        assertEquals(true, toggled.showChargingNotification)
        assertEquals(false, toggled.isBurnInProtectionEnabled)
        assertEquals(true, toggled.keepScreenAwake)
        assertEquals(true, toggled.showBatteryStatus)
        assertEquals(true, toggled.showMediaPlayer)
    }

    @Test
    fun showCalendarAgenda_remainsCompletelyIndependentOfOtherFeatures() {
        val initial = UserClockPreferences()

        val calendarEnabled = initial.copy(showCalendarAgenda = true)
        assertEquals(true, calendarEnabled.showCalendarAgenda)
        assertEquals(false, calendarEnabled.showMediaPlayer)
        assertEquals(false, calendarEnabled.showBatteryStatus)
        assertEquals(false, calendarEnabled.showChargingNotification)
        assertEquals(false, calendarEnabled.isNightModeEnabled)
        assertEquals(true, calendarEnabled.keepScreenAwake)
        assertEquals(true, calendarEnabled.isBurnInProtectionEnabled)
    }

    @Test
    fun showMediaPlayer_toggling_preservesOtherPreferences() {
        val initial = UserClockPreferences(
            clockFace = ClockFaceType.ANALOG,
            is24Hour = true,
            showSeconds = true,
            showDate = false,
            accentColor = AccentColorOption.PURPLE,
            isNightModeEnabled = true,
            showChargingNotification = true,
            isBurnInProtectionEnabled = false,
            keepScreenAwake = true,
            showBatteryStatus = true,
            showMediaPlayer = false
        )
        val toggled = initial.copy(showMediaPlayer = true)

        assertEquals(true, toggled.showMediaPlayer)
        assertEquals(ClockFaceType.ANALOG, toggled.clockFace)
        assertEquals(true, toggled.is24Hour)
        assertEquals(true, toggled.showSeconds)
        assertEquals(false, toggled.showDate)
        assertEquals(AccentColorOption.PURPLE, toggled.accentColor)
        assertEquals(true, toggled.isNightModeEnabled)
        assertEquals(true, toggled.showChargingNotification)
        assertEquals(false, toggled.isBurnInProtectionEnabled)
        assertEquals(true, toggled.keepScreenAwake)
        assertEquals(true, toggled.showBatteryStatus)
    }

    @Test
    fun showMediaPlayer_remainsCompletelyIndependentOfOtherFeatures() {
        val initial = UserClockPreferences()

        val mediaEnabled = initial.copy(showMediaPlayer = true)
        assertEquals(true, mediaEnabled.showMediaPlayer)
        assertEquals(false, mediaEnabled.showBatteryStatus)
        assertEquals(false, mediaEnabled.showChargingNotification)
        assertEquals(false, mediaEnabled.isNightModeEnabled)
        assertEquals(true, mediaEnabled.keepScreenAwake)
        assertEquals(true, mediaEnabled.isBurnInProtectionEnabled)
    }

    @Test
    fun keepScreenAwake_toggling_preservesOtherPreferences() {
        val initial = UserClockPreferences(
            clockFace = ClockFaceType.ANALOG,
            is24Hour = true,
            showSeconds = true,
            showDate = false,
            accentColor = AccentColorOption.PURPLE,
            isNightModeEnabled = true,
            showChargingNotification = true,
            isBurnInProtectionEnabled = false,
            keepScreenAwake = true,
            showBatteryStatus = false
        )
        val toggled = initial.copy(keepScreenAwake = false)

        assertEquals(false, toggled.keepScreenAwake)
        assertEquals(ClockFaceType.ANALOG, toggled.clockFace)
        assertEquals(true, toggled.is24Hour)
        assertEquals(true, toggled.showSeconds)
        assertEquals(false, toggled.showDate)
        assertEquals(AccentColorOption.PURPLE, toggled.accentColor)
        assertEquals(true, toggled.isNightModeEnabled)
        assertEquals(true, toggled.showChargingNotification)
        assertEquals(false, toggled.isBurnInProtectionEnabled)
        assertEquals(false, toggled.showBatteryStatus)
    }

    @Test
    fun showBatteryStatus_toggling_preservesOtherPreferences() {
        val initial = UserClockPreferences(
            clockFace = ClockFaceType.RETRO_FLIP,
            is24Hour = true,
            showSeconds = false,
            showDate = true,
            accentColor = AccentColorOption.GREEN,
            isNightModeEnabled = true,
            showChargingNotification = true,
            isBurnInProtectionEnabled = true,
            keepScreenAwake = false,
            showBatteryStatus = false
        )
        val toggled = initial.copy(showBatteryStatus = true)

        assertEquals(true, toggled.showBatteryStatus)
        assertEquals(ClockFaceType.RETRO_FLIP, toggled.clockFace)
        assertEquals(true, toggled.is24Hour)
        assertEquals(false, toggled.showSeconds)
        assertEquals(true, toggled.showDate)
        assertEquals(AccentColorOption.GREEN, toggled.accentColor)
        assertEquals(true, toggled.isNightModeEnabled)
        assertEquals(true, toggled.showChargingNotification)
        assertEquals(true, toggled.isBurnInProtectionEnabled)
        assertEquals(false, toggled.keepScreenAwake)
    }

    @Test
    fun burnInProtection_toggling_preservesClockSettingsAndOtherPreferences() {
        val initial = UserClockPreferences(
            clockFace = ClockFaceType.ANALOG,
            is24Hour = true,
            showSeconds = true,
            showDate = false,
            accentColor = AccentColorOption.PURPLE,
            isNightModeEnabled = false,
            showChargingNotification = true,
            isBurnInProtectionEnabled = true,
            keepScreenAwake = false,
            showBatteryStatus = true
        )
        val toggled = initial.copy(isBurnInProtectionEnabled = false)

        assertEquals(false, toggled.isBurnInProtectionEnabled)
        assertEquals(ClockFaceType.ANALOG, toggled.clockFace)
        assertEquals(true, toggled.is24Hour)
        assertEquals(true, toggled.showSeconds)
        assertEquals(false, toggled.showDate)
        assertEquals(AccentColorOption.PURPLE, toggled.accentColor)
        assertEquals(false, toggled.isNightModeEnabled)
        assertEquals(true, toggled.showChargingNotification)
        assertEquals(false, toggled.keepScreenAwake)
        assertEquals(true, toggled.showBatteryStatus)
    }

    @Test
    fun batteryIndicatorAndChargingNotifications_remainCompletelyIndependent() {
        val initial = UserClockPreferences(
            showChargingNotification = false,
            showBatteryStatus = false
        )

        // Enabling battery indicator does not enable charging notification
        val batteryEnabled = initial.copy(showBatteryStatus = true)
        assertEquals(true, batteryEnabled.showBatteryStatus)
        assertEquals(false, batteryEnabled.showChargingNotification)

        // Enabling charging notification does not enable battery indicator
        val chargingEnabled = initial.copy(showChargingNotification = true)
        assertEquals(true, chargingEnabled.showChargingNotification)
        assertEquals(false, chargingEnabled.showBatteryStatus)
    }

    @Test
    fun keepScreenAwake_doesNotImplicitlyEnableChargingNotifications() {
        val initial = UserClockPreferences(
            keepScreenAwake = false,
            showChargingNotification = false
        )
        val toggled = initial.copy(keepScreenAwake = true)
        assertEquals(true, toggled.keepScreenAwake)
        assertEquals(false, toggled.showChargingNotification)
    }

    @Test
    fun clockFaceType_fromName_handles_valid_and_invalid_values() {
        assertEquals(ClockFaceType.BOLD_DIGITAL, ClockFaceType.fromName("BOLD_DIGITAL"))
        assertEquals(ClockFaceType.ANALOG, ClockFaceType.fromName("ANALOG"))
        assertEquals(ClockFaceType.RETRO_FLIP, ClockFaceType.fromName("RETRO_FLIP"))
        assertEquals(ClockFaceType.RETRO_FLIP, ClockFaceType.fromName("retro_flip"))

        // Malformed or unknown inputs default safely to BOLD_DIGITAL without throwing
        assertEquals(ClockFaceType.BOLD_DIGITAL, ClockFaceType.fromName(null))
        assertEquals(ClockFaceType.BOLD_DIGITAL, ClockFaceType.fromName(""))
        assertEquals(ClockFaceType.BOLD_DIGITAL, ClockFaceType.fromName("UNKNOWN_FACE"))
    }

    @Test
    fun accentColorOption_fromName_handles_valid_and_invalid_values() {
        assertEquals(AccentColorOption.WHITE, AccentColorOption.fromName("WHITE"))
        assertEquals(AccentColorOption.ORANGE, AccentColorOption.fromName("ORANGE"))
        assertEquals(AccentColorOption.BLUE, AccentColorOption.fromName("BLUE"))
        assertEquals(AccentColorOption.GREEN, AccentColorOption.fromName("GREEN"))
        assertEquals(AccentColorOption.PURPLE, AccentColorOption.fromName("PURPLE"))
        assertEquals(AccentColorOption.BLUE, AccentColorOption.fromName("blue"))

        // Malformed or unknown inputs default safely to ORANGE without throwing
        assertEquals(AccentColorOption.ORANGE, AccentColorOption.fromName(null))
        assertEquals(AccentColorOption.ORANGE, AccentColorOption.fromName(""))
        assertEquals(AccentColorOption.ORANGE, AccentColorOption.fromName("HOT_PINK"))
    }

    @Test
    fun nightMode_enableAndDisable_preservesSelectedAccentColor() {
        val initial = UserClockPreferences(accentColor = AccentColorOption.BLUE, isNightModeEnabled = false)
        val nightActive = initial.copy(isNightModeEnabled = true)
        // Saved accent color is untouched
        assertEquals(AccentColorOption.BLUE, nightActive.accentColor)
        assertEquals(true, nightActive.isNightModeEnabled)

        // Disabling restores active state while retaining original accent
        val nightDisabled = nightActive.copy(isNightModeEnabled = false)
        assertEquals(AccentColorOption.BLUE, nightDisabled.accentColor)
        assertEquals(false, nightDisabled.isNightModeEnabled)
    }

    @Test
    fun clockThemePalette_resolve_appliesMonochromaticRedInNightMode() {
        val palette = ClockThemePalette.resolve(isNightMode = true, selectedAccent = AccentColorOption.BLUE)
        assertEquals(true, palette.isNightMode)
        assertEquals(StandByNightText, palette.primaryText)
        assertEquals(StandByNightTextMuted, palette.mutedText)
        assertEquals(StandByNightText, palette.accentColor)
        assertEquals(StandByNightSurface, palette.surface)
        assertEquals(StandByNightSurface, palette.border)
    }

    @Test
    fun clockThemePalette_resolve_restoresNormalColorsAndUserAccentWhenDisabled() {
        val palette = ClockThemePalette.resolve(isNightMode = false, selectedAccent = AccentColorOption.BLUE)
        assertEquals(false, palette.isNightMode)
        assertEquals(StandByTextPrimary, palette.primaryText)
        assertEquals(StandByTextMuted, palette.mutedText)
        assertEquals(AccentColorOption.BLUE.color, palette.accentColor)
        assertEquals(StandBySurfaceCard, palette.surface)
        assertEquals(StandByBorder, palette.border)
    }

    @Test
    fun nightModeColorTokens_matchCanonicalHexValues() {
        assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), com.kunvarpreet.standbymode.ui.theme.StandByNightBackground)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF453A), StandByNightText)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFB33939), StandByNightTextMuted)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF1A0505), StandByNightSurface)
    }

    @Test
    fun normalAccentColorTokens_matchCanonicalHexValuesAndDefault() {
        // Assert canonical hex string and Color values for all five curated accent options
        assertEquals("#FFFFFF", AccentColorOption.WHITE.hexCode)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFFFFFF), AccentColorOption.WHITE.color)

        assertEquals("#FF6B35", AccentColorOption.ORANGE.hexCode)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF6B35), AccentColorOption.ORANGE.color)

        assertEquals("#00A8E8", AccentColorOption.BLUE.hexCode)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF00A8E8), AccentColorOption.BLUE.color)

        assertEquals("#76FF03", AccentColorOption.GREEN.hexCode)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF76FF03), AccentColorOption.GREEN.color)

        assertEquals("#AF52DE", AccentColorOption.PURPLE.hexCode)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFAF52DE), AccentColorOption.PURPLE.color)

        // Verify default preference resolves to Sunset Orange (#FF6B35)
        val defaultPrefs = UserClockPreferences()
        assertEquals(AccentColorOption.ORANGE, defaultPrefs.accentColor)
        assertEquals("Sunset Orange", defaultPrefs.accentColor.displayName)
        assertEquals("#FF6B35", defaultPrefs.accentColor.hexCode)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF6B35), defaultPrefs.accentColor.color)
    }
}

