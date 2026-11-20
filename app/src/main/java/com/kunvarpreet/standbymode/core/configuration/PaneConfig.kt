package com.kunvarpreet.standbymode.core.configuration

/**
 * Configuration for a distinct display pane hosting an ordered stack of widgets.
 *
 * @param paneId Stable identifier for this pane (e.g., [PANE_LEFT], [PANE_RIGHT], [PANE_FULLSCREEN]).
 * @param widgets Ordered list of widget instances in this pane's stack.
 * @param selectedIndex The index of the currently visible/active widget in the stack.
 */
data class PaneConfig(
    val paneId: String,
    val widgets: List<WidgetInstanceConfig> = emptyList(),
    val selectedIndex: Int = 0
) {
    /**
     * Resolves the currently active widget instance, safely coercing bounds.
     */
    val activeWidget: WidgetInstanceConfig?
        get() = if (widgets.isEmpty()) null else widgets.getOrNull(selectedIndex.coerceIn(0, widgets.lastIndex))

    /**
     * Returns a copy with the selected index safely clamped.
     */
    fun withSafeIndex(newIndex: Int): PaneConfig {
        if (widgets.isEmpty()) return copy(selectedIndex = 0)
        return copy(selectedIndex = newIndex.coerceIn(0, widgets.lastIndex))
    }

    companion object {
        const val PANE_LEFT = "LEFT_PANE"
        const val PANE_RIGHT = "RIGHT_PANE"
        const val PANE_FULLSCREEN = "FULLSCREEN_PANE"
    }
}
