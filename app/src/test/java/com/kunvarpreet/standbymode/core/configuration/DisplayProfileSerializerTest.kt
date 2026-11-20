package com.kunvarpreet.standbymode.core.configuration

import com.kunvarpreet.standbymode.core.preferences.AccentColorOption
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.core.widget.DuoWidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayProfileSerializerTest {

    @Test
    fun serializeAndDeserialize_preservesProfileStructure() {
        val original = DisplayProfile.createDefaultProfile()
        val json = DisplayProfileSerializer.serializeProfile(original)

        assertNotNull(json)
        assertTrue(json.contains("schemaVersion"))
        assertTrue(json.contains(DisplayProfile.DEFAULT_PROFILE_ID))

        val restored = DisplayProfileSerializer.deserializeProfile(json)
        assertEquals(original.id, restored.id)
        assertEquals(original.layoutMode, restored.layoutMode)
        assertEquals(2, restored.panes.size)

        val leftPane = restored.getPane(PaneConfig.PANE_LEFT)
        assertNotNull(leftPane)
        assertEquals(3, leftPane!!.widgets.size)
        assertEquals(DuoWidgetType.CLOCK, leftPane.widgets[0].widgetType)
        assertEquals(ClockFaceType.ANALOG, leftPane.widgets[0].clockAppearance?.clockFaceType)
    }

    @Test
    fun deserialize_nullOrEmptyInput_returnsDefaultProfile() {
        val fromNull = DisplayProfileSerializer.deserializeProfile(null)
        assertNotNull(fromNull)
        assertEquals(DisplayProfile.DEFAULT_PROFILE_ID, fromNull.id)

        val fromBlank = DisplayProfileSerializer.deserializeProfile("   ")
        assertNotNull(fromBlank)
        assertEquals(DisplayProfile.DEFAULT_PROFILE_ID, fromBlank.id)
    }

    @Test
    fun deserialize_corruptJson_returnsDefaultProfileGracefully() {
        val fromCorrupt = DisplayProfileSerializer.deserializeProfile("{ invalid json ...")
        assertNotNull(fromCorrupt)
        assertEquals(DisplayProfile.DEFAULT_PROFILE_ID, fromCorrupt.id)
    }

    @Test
    fun migrateFromLegacyPreferences_preservesUserCustomizations() {
        val legacy = UserClockPreferences(
            clockFace = ClockFaceType.RETRO_FLIP,
            accentColor = AccentColorOption.PURPLE,
            is24Hour = true,
            showSeconds = true,
            showDate = false,
            showCalendarAgenda = true,
            showWeatherWidget = true,
            showBatteryStatus = true,
            showMediaPlayer = true,
            showPhotoFrame = false
        )

        val migrated = DisplayProfileSerializer.migrateFromLegacyPreferences(legacy)

        assertNotNull(migrated)
        val leftPane = migrated.getPane(PaneConfig.PANE_LEFT)
        assertNotNull(leftPane)

        // Verify clock appearance migrated accurately
        val leftClock = leftPane!!.widgets.first { it.widgetType == DuoWidgetType.CLOCK }
        assertNotNull(leftClock.clockAppearance)
        assertEquals(ClockFaceType.RETRO_FLIP, leftClock.clockAppearance?.clockFaceType)
        assertEquals(AccentColorOption.PURPLE, leftClock.clockAppearance?.accentColor)
        assertTrue(leftClock.clockAppearance!!.use24HourFormat)
        assertTrue(leftClock.clockAppearance!!.showSeconds)
        assertFalse(leftClock.clockAppearance!!.showDateCapsule)

        // Verify enabled widgets migrated into stacks
        assertTrue(leftPane.widgets.any { it.widgetType == DuoWidgetType.CALENDAR })
        assertTrue(leftPane.widgets.any { it.widgetType == DuoWidgetType.WEATHER })
        assertTrue(leftPane.widgets.any { it.widgetType == DuoWidgetType.BATTERY_STATUS })
        assertTrue(leftPane.widgets.any { it.widgetType == DuoWidgetType.MEDIA })
        assertFalse(leftPane.widgets.any { it.widgetType == DuoWidgetType.PHOTO_FRAME })
    }

    @Test
    fun configurationIsolation_leftAndRightClockAppearancesAreIndependent() {
        val profile = DisplayProfile.createDefaultProfile()
        val leftPane = profile.getPane(PaneConfig.PANE_LEFT)!!
        val rightPane = profile.getPane(PaneConfig.PANE_RIGHT)!!

        // Left pane has an analog clock in orange
        val leftClock = leftPane.widgets.first { it.widgetType == DuoWidgetType.CLOCK }
        assertEquals(ClockFaceType.ANALOG, leftClock.clockAppearance?.clockFaceType)
        assertEquals(AccentColorOption.ORANGE, leftClock.clockAppearance?.accentColor)

        // Mutate left clock appearance to Green Retro Flip
        val modifiedAppearance = leftClock.clockAppearance!!.copy(
            clockFaceType = ClockFaceType.RETRO_FLIP,
            accentColor = AccentColorOption.GREEN
        )

        val updatedProfile = profile.updatePane(PaneConfig.PANE_LEFT) { p ->
            p.copy(
                widgets = p.widgets.map { w ->
                    if (w.instanceId == leftClock.instanceId) w.copy(clockAppearance = modifiedAppearance) else w
                }
            )
        }

        // Verify left clock changed
        val updatedLeftClock = updatedProfile.getPane(PaneConfig.PANE_LEFT)!!.widgets.first { it.instanceId == leftClock.instanceId }
        assertEquals(ClockFaceType.RETRO_FLIP, updatedLeftClock.clockAppearance?.clockFaceType)
        assertEquals(AccentColorOption.GREEN, updatedLeftClock.clockAppearance?.accentColor)

        // Verify right pane remains completely unaffected
        val updatedRightPane = updatedProfile.getPane(PaneConfig.PANE_RIGHT)!!
        assertEquals(rightPane.widgets.size, updatedRightPane.widgets.size)
        assertEquals(rightPane.selectedIndex, updatedRightPane.selectedIndex)
    }

    @Test
    fun paneConfig_withSafeIndex_clampsWithinBounds() {
        val widgets = listOf(
            WidgetInstanceConfig(instanceId = "w1", widgetType = DuoWidgetType.CLOCK),
            WidgetInstanceConfig(instanceId = "w2", widgetType = DuoWidgetType.CALENDAR)
        )
        val pane = PaneConfig(paneId = "test", widgets = widgets, selectedIndex = 0)

        // Clamping upper bound
        val clampedUpper = pane.withSafeIndex(99)
        assertEquals(1, clampedUpper.selectedIndex)

        // Clamping lower bound
        val clampedLower = pane.withSafeIndex(-5)
        assertEquals(0, clampedLower.selectedIndex)

        // Empty widgets case
        val emptyPane = PaneConfig(paneId = "empty", widgets = emptyList(), selectedIndex = 5)
        val clampedEmpty = emptyPane.withSafeIndex(3)
        assertEquals(0, clampedEmpty.selectedIndex)
    }
}
