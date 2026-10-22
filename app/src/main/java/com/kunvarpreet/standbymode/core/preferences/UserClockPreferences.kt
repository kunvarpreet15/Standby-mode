package com.kunvarpreet.standbymode.core.preferences

import androidx.compose.ui.graphics.Color
import com.kunvarpreet.standbymode.ui.theme.StandByAccentBlue
import com.kunvarpreet.standbymode.ui.theme.StandByAccentGreen
import com.kunvarpreet.standbymode.ui.theme.StandByAccentOrange
import com.kunvarpreet.standbymode.ui.theme.StandByAccentPurple
import com.kunvarpreet.standbymode.ui.theme.StandByAccentWhite
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * Available clock face options.
 */
enum class ClockFaceType(val displayName: String) {
    BOLD_DIGITAL("Bold Digital"),
    ANALOG("Analog"),
    RETRO_FLIP("Retro Flip");

    companion object {
        fun fromName(name: String?): ClockFaceType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: BOLD_DIGITAL
        }
    }
}

/**
 * Curated signature accent color options.
 */
enum class AccentColorOption(
    val displayName: String,
    val color: Color,
    val hexCode: String
) {
    WHITE("White", StandByAccentWhite, "#FFFFFF"),
    ORANGE("Sunset Orange", StandByAccentOrange, "#FF6B35"),
    BLUE("Cyber Blue", StandByAccentBlue, "#00A8E8"),
    GREEN("Neon Green", StandByAccentGreen, "#76FF03"),
    PURPLE("Electric Purple", StandByAccentPurple, "#AF52DE");

    companion object {
        fun fromName(name: String?): AccentColorOption {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ORANGE
        }
    }
}

/**
 * Resolved color tokens for clock face and ambient UI rendering,
 * dynamically calculated from Night Mode state and active accent color.
 */
data class ClockThemePalette(
    val isNightMode: Boolean,
    val primaryText: Color,
    val mutedText: Color,
    val accentColor: Color,
    val surface: Color,
    val border: Color
) {
    companion object {
        fun resolve(isNightMode: Boolean, selectedAccent: AccentColorOption): ClockThemePalette {
            return if (isNightMode) {
                ClockThemePalette(
                    isNightMode = true,
                    primaryText = StandByNightText,
                    mutedText = StandByNightTextMuted,
                    accentColor = StandByNightText,
                    surface = StandByNightSurface,
                    border = StandByNightSurface
                )
            } else {
                ClockThemePalette(
                    isNightMode = false,
                    primaryText = StandByTextPrimary,
                    mutedText = StandByTextMuted,
                    accentColor = selectedAccent.color,
                    surface = StandBySurfaceCard,
                    border = StandByBorder
                )
            }
        }
    }
}

/**
 * Immutable user clock preferences model with safe, resilient defaults.
 */
data class UserClockPreferences(
    val clockFace: ClockFaceType = ClockFaceType.BOLD_DIGITAL,
    val is24Hour: Boolean = false,
    val showSeconds: Boolean = false,
    val showDate: Boolean = true,
    val accentColor: AccentColorOption = AccentColorOption.ORANGE,
    val isNightModeEnabled: Boolean = false,
    val showChargingNotification: Boolean = false,
    val isBurnInProtectionEnabled: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val showBatteryStatus: Boolean = false,
    val showMediaPlayer: Boolean = false
)
