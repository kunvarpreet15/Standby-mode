package com.kunvarpreet.standbymode.core.configuration

import com.kunvarpreet.standbymode.core.widget.DuoWidgetType
import java.util.UUID

/**
 * An individual widget instance within an ordered pane stack.
 *
 * @param instanceId Stable unique identifier for this widget instance.
 * @param widgetType Type of built-in or external widget.
 * @param clockAppearance Per-instance appearance configuration if this widget is a clock.
 * @param appWidgetId Optional ID for hosted third-party Android AppWidgets (null for built-in).
 * @param customSettings Key-value store for widget-specific configuration.
 */
data class WidgetInstanceConfig(
    val instanceId: String = UUID.randomUUID().toString(),
    val widgetType: DuoWidgetType,
    val clockAppearance: ClockAppearanceConfig? = if (widgetType == DuoWidgetType.CLOCK) ClockAppearanceConfig() else null,
    val appWidgetId: Int? = null,
    val customSettings: Map<String, String> = emptyMap()
)
