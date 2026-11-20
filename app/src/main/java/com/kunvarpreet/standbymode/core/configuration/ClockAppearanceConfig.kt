package com.kunvarpreet.standbymode.core.configuration

import com.kunvarpreet.standbymode.core.preferences.AccentColorOption
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType

/**
 * Isolated appearance and typography configuration for an individual clock widget instance.
 * Decouples styling across distinct panes and display spaces so changes in one pane
 * do not overwrite other widgets.
 */
data class ClockAppearanceConfig(
    val clockFaceType: ClockFaceType = ClockFaceType.BOLD_DIGITAL,
    val accentColor: AccentColorOption = AccentColorOption.ORANGE,
    val fontFamily: String = DEFAULT_FONT,
    val showSeconds: Boolean = false,
    val showDateCapsule: Boolean = true,
    val use24HourFormat: Boolean = false
) {
    companion object {
        const val DEFAULT_FONT = "Default"
        const val SERIF_FONT = "Serif"
        const val MONOSPACE_FONT = "Monospace"
        const val CONDENSED_FONT = "Condensed"

        val SUPPORTED_FONTS = listOf(DEFAULT_FONT, SERIF_FONT, MONOSPACE_FONT, CONDENSED_FONT)
    }
}
