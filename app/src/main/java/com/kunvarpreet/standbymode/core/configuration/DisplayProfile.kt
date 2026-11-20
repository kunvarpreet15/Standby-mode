package com.kunvarpreet.standbymode.core.configuration

/**
 * A complete display profile defining layout mode and per-pane widget configurations.
 * Supports multiple spaces (e.g. Bedside, Desk, Travel) with isolated widget stacks.
 */
data class DisplayProfile(
    val id: String = DEFAULT_PROFILE_ID,
    val name: String = "Default Profile",
    val layoutMode: String = LAYOUT_DUO,
    val panes: Map<String, PaneConfig> = emptyMap(),
    val isDefault: Boolean = true
) {
    fun getPane(paneId: String): PaneConfig? = panes[paneId]

    fun updatePane(paneId: String, transform: (PaneConfig) -> PaneConfig): DisplayProfile {
        val current = panes[paneId] ?: PaneConfig(paneId = paneId)
        val updated = transform(current)
        return copy(panes = panes + (paneId to updated))
    }

    companion object {
        const val DEFAULT_PROFILE_ID = "profile_default"
        const val LAYOUT_DUO = "DUO"
        const val LAYOUT_FULLSCREEN = "FULLSCREEN"
        const val LAYOUT_QUAD = "QUAD"

        /**
         * Builds a clean default profile with recommended initial widgets:
         * Left pane: Analog clock, Bold digital clock, Flip clock.
         * Right pane: Calendar agenda, Weather, Battery vitals, Media player.
         */
        fun createDefaultProfile(): DisplayProfile {
            val leftWidgets = listOf(
                WidgetInstanceConfig(
                    instanceId = "default_left_clock_analog",
                    widgetType = com.kunvarpreet.standbymode.core.widget.DuoWidgetType.CLOCK,
                    clockAppearance = ClockAppearanceConfig(
                        clockFaceType = com.kunvarpreet.standbymode.core.preferences.ClockFaceType.ANALOG,
                        accentColor = com.kunvarpreet.standbymode.core.preferences.AccentColorOption.ORANGE
                    )
                ),
                WidgetInstanceConfig(
                    instanceId = "default_left_clock_digital",
                    widgetType = com.kunvarpreet.standbymode.core.widget.DuoWidgetType.CLOCK,
                    clockAppearance = ClockAppearanceConfig(
                        clockFaceType = com.kunvarpreet.standbymode.core.preferences.ClockFaceType.BOLD_DIGITAL,
                        accentColor = com.kunvarpreet.standbymode.core.preferences.AccentColorOption.ORANGE
                    )
                ),
                WidgetInstanceConfig(
                    instanceId = "default_left_clock_flip",
                    widgetType = com.kunvarpreet.standbymode.core.widget.DuoWidgetType.CLOCK,
                    clockAppearance = ClockAppearanceConfig(
                        clockFaceType = com.kunvarpreet.standbymode.core.preferences.ClockFaceType.RETRO_FLIP,
                        accentColor = com.kunvarpreet.standbymode.core.preferences.AccentColorOption.ORANGE
                    )
                )
            )

            val rightWidgets = listOf(
                WidgetInstanceConfig(
                    instanceId = "default_right_calendar",
                    widgetType = com.kunvarpreet.standbymode.core.widget.DuoWidgetType.CALENDAR
                ),
                WidgetInstanceConfig(
                    instanceId = "default_right_weather",
                    widgetType = com.kunvarpreet.standbymode.core.widget.DuoWidgetType.WEATHER
                ),
                WidgetInstanceConfig(
                    instanceId = "default_right_battery",
                    widgetType = com.kunvarpreet.standbymode.core.widget.DuoWidgetType.BATTERY_STATUS
                ),
                WidgetInstanceConfig(
                    instanceId = "default_right_media",
                    widgetType = com.kunvarpreet.standbymode.core.widget.DuoWidgetType.MEDIA
                )
            )

            return DisplayProfile(
                id = DEFAULT_PROFILE_ID,
                name = "Bedside Default",
                layoutMode = LAYOUT_DUO,
                panes = mapOf(
                    PaneConfig.PANE_LEFT to PaneConfig(
                        paneId = PaneConfig.PANE_LEFT,
                        widgets = leftWidgets,
                        selectedIndex = 0
                    ),
                    PaneConfig.PANE_RIGHT to PaneConfig(
                        paneId = PaneConfig.PANE_RIGHT,
                        widgets = rightWidgets,
                        selectedIndex = 0
                    )
                ),
                isDefault = true
            )
        }
    }
}
