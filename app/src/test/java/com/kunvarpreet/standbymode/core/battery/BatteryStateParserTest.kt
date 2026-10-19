package com.kunvarpreet.standbymode.core.battery

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryStateParserTest {

    @Test
    fun percentageCalculation_validData_computesAccuratePercentage() {
        val state85 = BatteryStateParser.parse(level = 85, scale = 100, status = BatteryManager.BATTERY_STATUS_DISCHARGING)
        assertTrue(state85.isAvailable)
        assertEquals(85, state85.percentage)
        assertEquals("85%", state85.formattedPercentage)

        val stateHalf = BatteryStateParser.parse(level = 1, scale = 2, status = BatteryManager.BATTERY_STATUS_DISCHARGING)
        assertTrue(stateHalf.isAvailable)
        assertEquals(50, stateHalf.percentage)

        val stateZero = BatteryStateParser.parse(level = 0, scale = 100, status = BatteryManager.BATTERY_STATUS_DISCHARGING)
        assertTrue(stateZero.isAvailable)
        assertEquals(0, stateZero.percentage)
    }

    @Test
    fun percentageCalculation_valuesAboveScale_areClampedTo100() {
        val stateOverflow = BatteryStateParser.parse(level = 105, scale = 100, status = BatteryManager.BATTERY_STATUS_FULL, plugged = 1)
        assertTrue(stateOverflow.isAvailable)
        assertEquals(100, stateOverflow.percentage)
        assertEquals("100%", stateOverflow.formattedPercentage)
    }

    @Test
    fun percentageCalculation_invalidOrUnavailableValues_returnUnavailable() {
        // Negative level
        val stateNegativeLevel = BatteryStateParser.parse(level = -1, scale = 100, status = BatteryManager.BATTERY_STATUS_UNKNOWN)
        assertFalse(stateNegativeLevel.isAvailable)
        assertNull(stateNegativeLevel.percentage)
        assertEquals("", stateNegativeLevel.formattedPercentage)

        // Zero scale
        val stateZeroScale = BatteryStateParser.parse(level = 50, scale = 0, status = BatteryManager.BATTERY_STATUS_UNKNOWN)
        assertFalse(stateZeroScale.isAvailable)
        assertNull(stateZeroScale.percentage)

        // Negative scale
        val stateNegativeScale = BatteryStateParser.parse(level = 50, scale = -1, status = BatteryManager.BATTERY_STATUS_UNKNOWN)
        assertFalse(stateNegativeScale.isAvailable)
        assertNull(stateNegativeScale.percentage)

        // Null intent
        val nullState = BatteryStateParser.parse(intent = null)
        assertFalse(nullState.isAvailable)
        assertNull(nullState.percentage)
        assertFalse(nullState.isCharging)
    }

    @Test
    fun chargingState_whenActivelyCharging_returnsChargingTrue() {
        // Charging via AC
        val stateAc = BatteryStateParser.parse(
            level = 42,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_AC
        )
        assertTrue(stateAc.isAvailable)
        assertTrue(stateAc.isCharging)

        // Charging via USB
        val stateUsb = BatteryStateParser.parse(
            level = 42,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_USB
        )
        assertTrue(stateUsb.isCharging)

        // Charging via Wireless
        val stateWireless = BatteryStateParser.parse(
            level = 42,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_WIRELESS
        )
        assertTrue(stateWireless.isCharging)
    }

    @Test
    fun chargingState_whenFullWhilePluggedIn_returnsChargingTrue() {
        val stateFullPlugged = BatteryStateParser.parse(
            level = 100,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_FULL,
            plugged = BatteryManager.BATTERY_PLUGGED_AC
        )
        assertTrue(stateFullPlugged.isAvailable)
        assertTrue(stateFullPlugged.isCharging)
    }

    @Test
    fun chargingState_whenFullWhileUnplugged_returnsChargingFalse() {
        val stateFullUnplugged = BatteryStateParser.parse(
            level = 100,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_FULL,
            plugged = 0
        )
        assertTrue(stateFullUnplugged.isAvailable)
        assertFalse(stateFullUnplugged.isCharging)
    }

    @Test
    fun chargingState_whenDischargingOrNotCharging_returnsChargingFalse() {
        // Normal discharging on battery
        val stateDischarging = BatteryStateParser.parse(
            level = 75,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_DISCHARGING,
            plugged = 0
        )
        assertTrue(stateDischarging.isAvailable)
        assertFalse(stateDischarging.isCharging)

        // Plugged in but not charging
        val stateNotCharging = BatteryStateParser.parse(
            level = 75,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_NOT_CHARGING,
            plugged = BatteryManager.BATTERY_PLUGGED_AC
        )
        assertTrue(stateNotCharging.isAvailable)
        assertFalse(stateNotCharging.isCharging)

        // Unknown status
        val stateUnknown = BatteryStateParser.parse(
            level = 75,
            scale = 100,
            status = BatteryManager.BATTERY_STATUS_UNKNOWN,
            plugged = 0
        )
        assertFalse(stateUnknown.isCharging)
    }
}
