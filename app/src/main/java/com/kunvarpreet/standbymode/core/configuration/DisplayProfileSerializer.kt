package com.kunvarpreet.standbymode.core.configuration

import com.kunvarpreet.standbymode.core.preferences.AccentColorOption
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.core.widget.DuoWidgetType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust JSON serialization and deserialization for [DisplayProfile] and its child configurations.
 * Uses standard org.json primitives with zero external dependency overhead.
 */
object DisplayProfileSerializer {

    const val CURRENT_SCHEMA_VERSION = 2

    fun serializeProfile(profile: DisplayProfile): String {
        val root = JSONObject()
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("id", profile.id)
        root.put("name", profile.name)
        root.put("layoutMode", profile.layoutMode)
        root.put("isDefault", profile.isDefault)

        val panesObj = JSONObject()
        for ((paneId, paneConfig) in profile.panes) {
            val paneJson = JSONObject()
            paneJson.put("paneId", paneConfig.paneId)
            paneJson.put("selectedIndex", paneConfig.selectedIndex)

            val widgetsArray = JSONArray()
            for (widget in paneConfig.widgets) {
                val widgetJson = JSONObject()
                widgetJson.put("instanceId", widget.instanceId)
                widgetJson.put("widgetType", widget.widgetType.name)
                if (widget.appWidgetId != null) {
                    widgetJson.put("appWidgetId", widget.appWidgetId)
                }

                if (widget.clockAppearance != null) {
                    val clockJson = JSONObject()
                    clockJson.put("clockFaceType", widget.clockAppearance.clockFaceType.name)
                    clockJson.put("accentColor", widget.clockAppearance.accentColor.name)
                    clockJson.put("fontFamily", widget.clockAppearance.fontFamily)
                    clockJson.put("showSeconds", widget.clockAppearance.showSeconds)
                    clockJson.put("showDateCapsule", widget.clockAppearance.showDateCapsule)
                    clockJson.put("use24HourFormat", widget.clockAppearance.use24HourFormat)
                    widgetJson.put("clockAppearance", clockJson)
                }

                if (widget.customSettings.isNotEmpty()) {
                    val settingsObj = JSONObject()
                    for ((k, v) in widget.customSettings) {
                        settingsObj.put(k, v)
                    }
                    widgetJson.put("customSettings", settingsObj)
                }

                widgetsArray.put(widgetJson)
            }
            paneJson.put("widgets", widgetsArray)
            panesObj.put(paneId, paneJson)
        }
        root.put("panes", panesObj)

        return root.toString()
    }

    fun deserializeProfile(jsonString: String?): DisplayProfile {
        if (jsonString.isNullOrBlank()) {
            return DisplayProfile.createDefaultProfile()
        }

        return try {
            val root = JSONObject(jsonString)
            val id = root.optString("id", DisplayProfile.DEFAULT_PROFILE_ID)
            val name = root.optString("name", "Default Profile")
            val layoutMode = root.optString("layoutMode", DisplayProfile.LAYOUT_DUO)
            val isDefault = root.optBoolean("isDefault", true)

            val panesObj = root.optJSONObject("panes")
            val panesMap = mutableMapOf<String, PaneConfig>()

            if (panesObj != null) {
                val keys = panesObj.keys()
                while (keys.hasNext()) {
                    val paneId = keys.next()
                    val paneJson = panesObj.getJSONObject(paneId)
                    val selectedIndex = paneJson.optInt("selectedIndex", 0)
                    val widgetsArray = paneJson.optJSONArray("widgets")
                    val widgetsList = mutableListOf<WidgetInstanceConfig>()

                    if (widgetsArray != null) {
                        for (i in 0 until widgetsArray.length()) {
                            val widgetJson = widgetsArray.getJSONObject(i)
                            val instanceId = widgetJson.optString("instanceId", "widget_$i")
                            val typeName = widgetJson.optString("widgetType", DuoWidgetType.CLOCK.name)
                            val widgetType = try {
                                DuoWidgetType.valueOf(typeName)
                            } catch (e: Exception) {
                                DuoWidgetType.CLOCK
                            }
                            val appWidgetId = if (widgetJson.has("appWidgetId")) widgetJson.getInt("appWidgetId") else null

                            var clockAppearance: ClockAppearanceConfig? = null
                            if (widgetJson.has("clockAppearance")) {
                                val cJson = widgetJson.getJSONObject("clockAppearance")
                                clockAppearance = ClockAppearanceConfig(
                                    clockFaceType = ClockFaceType.fromName(cJson.optString("clockFaceType")),
                                    accentColor = AccentColorOption.fromName(cJson.optString("accentColor")),
                                    fontFamily = cJson.optString("fontFamily", ClockAppearanceConfig.DEFAULT_FONT),
                                    showSeconds = cJson.optBoolean("showSeconds", false),
                                    showDateCapsule = cJson.optBoolean("showDateCapsule", true),
                                    use24HourFormat = cJson.optBoolean("use24HourFormat", false)
                                )
                            } else if (widgetType == DuoWidgetType.CLOCK) {
                                clockAppearance = ClockAppearanceConfig()
                            }

                            val customSettings = mutableMapOf<String, String>()
                            val settingsObj = widgetJson.optJSONObject("customSettings")
                            if (settingsObj != null) {
                                val sKeys = settingsObj.keys()
                                while (sKeys.hasNext()) {
                                    val sk = sKeys.next()
                                    customSettings[sk] = settingsObj.getString(sk)
                                }
                            }

                            widgetsList.add(
                                WidgetInstanceConfig(
                                    instanceId = instanceId,
                                    widgetType = widgetType,
                                    clockAppearance = clockAppearance,
                                    appWidgetId = appWidgetId,
                                    customSettings = customSettings
                                )
                            )
                        }
                    }

                    panesMap[paneId] = PaneConfig(
                        paneId = paneId,
                        widgets = widgetsList,
                        selectedIndex = selectedIndex
                    )
                }
            }

            if (panesMap.isEmpty()) {
                DisplayProfile.createDefaultProfile()
            } else {
                DisplayProfile(
                    id = id,
                    name = name,
                    layoutMode = layoutMode,
                    panes = panesMap,
                    isDefault = isDefault
                )
            }
        } catch (e: Exception) {
            DisplayProfile.createDefaultProfile()
        }
    }

    /**
     * Migrates legacy flat [UserClockPreferences] (Schema Version 1) into a versioned [DisplayProfile] (Schema Version 2).
     * Preserves existing clock face, accent color, 24-hour setting, seconds toggle, date capsule,
     * and enabled widgets without any loss of user preferences.
     */
    fun migrateFromLegacyPreferences(legacy: UserClockPreferences): DisplayProfile {
        val migratedClockAppearance = ClockAppearanceConfig(
            clockFaceType = legacy.clockFaceType,
            accentColor = legacy.accentColor,
            fontFamily = ClockAppearanceConfig.DEFAULT_FONT,
            showSeconds = legacy.showSeconds,
            showDateCapsule = legacy.showDateCapsule,
            use24HourFormat = legacy.is24HourFormat
        )

        val leftWidgets = mutableListOf<WidgetInstanceConfig>()
        leftWidgets.add(
            WidgetInstanceConfig(
                instanceId = "migrated_left_clock",
                widgetType = DuoWidgetType.CLOCK,
                clockAppearance = migratedClockAppearance
            )
        )
        if (legacy.showCalendarAgenda) {
            leftWidgets.add(WidgetInstanceConfig(instanceId = "migrated_left_calendar", widgetType = DuoWidgetType.CALENDAR))
        }
        if (legacy.showWeatherWidget) {
            leftWidgets.add(WidgetInstanceConfig(instanceId = "migrated_left_weather", widgetType = DuoWidgetType.WEATHER))
        }
        if (legacy.showBatteryStatus) {
            leftWidgets.add(WidgetInstanceConfig(instanceId = "migrated_left_battery", widgetType = DuoWidgetType.BATTERY_STATUS))
        }
        if (legacy.showPhotoFrame) {
            leftWidgets.add(WidgetInstanceConfig(instanceId = "migrated_left_photo", widgetType = DuoWidgetType.PHOTO_FRAME))
        }
        if (legacy.showMediaPlayer) {
            leftWidgets.add(WidgetInstanceConfig(instanceId = "migrated_left_media", widgetType = DuoWidgetType.MEDIA))
        }

        val rightWidgets = mutableListOf<WidgetInstanceConfig>()
        if (legacy.showMediaPlayer) {
            rightWidgets.add(WidgetInstanceConfig(instanceId = "migrated_right_media", widgetType = DuoWidgetType.MEDIA))
        }
        if (legacy.showCalendarAgenda) {
            rightWidgets.add(WidgetInstanceConfig(instanceId = "migrated_right_calendar", widgetType = DuoWidgetType.CALENDAR))
        }
        if (legacy.showWeatherWidget) {
            rightWidgets.add(WidgetInstanceConfig(instanceId = "migrated_right_weather", widgetType = DuoWidgetType.WEATHER))
        }
        if (legacy.showBatteryStatus) {
            rightWidgets.add(WidgetInstanceConfig(instanceId = "migrated_right_battery", widgetType = DuoWidgetType.BATTERY_STATUS))
        }
        if (legacy.showPhotoFrame) {
            rightWidgets.add(WidgetInstanceConfig(instanceId = "migrated_right_photo", widgetType = DuoWidgetType.PHOTO_FRAME))
        }
        rightWidgets.add(
            WidgetInstanceConfig(
                instanceId = "migrated_right_clock",
                widgetType = DuoWidgetType.CLOCK,
                clockAppearance = migratedClockAppearance
            )
        )

        return DisplayProfile(
            id = DisplayProfile.DEFAULT_PROFILE_ID,
            name = "Migrated User Profile",
            layoutMode = DisplayProfile.LAYOUT_DUO,
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
