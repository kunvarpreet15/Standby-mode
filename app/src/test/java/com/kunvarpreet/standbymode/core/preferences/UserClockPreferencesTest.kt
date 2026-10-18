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

