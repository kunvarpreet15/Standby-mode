package com.kunvarpreet.standbymode.core.display

import com.kunvarpreet.standbymode.AppScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepScreenAwakePolicyTest {

    @Test
    fun shouldKeepScreenOn_whenPreferenceEnabled_clockScreen_foreground_andCharging_returnsTrue() {
        val result = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = true,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = true,
            isCharging = true
        )
        assertTrue(result)
    }

    @Test
    fun shouldKeepScreenOn_whenDeviceUnplugged_returnsFalse() {
        // Even if preference is enabled, clock is foregrounded, but unplugged -> false
        val result = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = true,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = true,
            isCharging = false
        )
        assertFalse(result)
    }

    @Test
    fun shouldKeepScreenOn_whenPreferenceDisabled_returnsFalse() {
        val onClockForegroundCharging = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = false,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = true,
            isCharging = true
        )
        assertFalse(onClockForegroundCharging)

        val onClockBackgroundCharging = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = false,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = false,
            isCharging = true
        )
        assertFalse(onClockBackgroundCharging)

        val onSettingsForegroundCharging = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = false,
            currentScreen = AppScreen.SETTINGS,
            isActivityForeground = true,
            isCharging = true
        )
        assertFalse(onSettingsForegroundCharging)
    }

    @Test
    fun shouldKeepScreenOn_whenSettingsScreen_returnsFalse() {
        val settingsForegroundPrefEnabledCharging = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = true,
            currentScreen = AppScreen.SETTINGS,
            isActivityForeground = true,
            isCharging = true
        )
        assertFalse(settingsForegroundPrefEnabledCharging)

        val settingsForegroundPrefDisabledCharging = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = false,
            currentScreen = AppScreen.SETTINGS,
            isActivityForeground = true,
            isCharging = true
        )
        assertFalse(settingsForegroundPrefDisabledCharging)

        val settingsBackgroundCharging = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = true,
            currentScreen = AppScreen.SETTINGS,
            isActivityForeground = false,
            isCharging = true
        )
        assertFalse(settingsBackgroundCharging)
    }

    @Test
    fun shouldKeepScreenOn_whenActivityNotInForeground_returnsFalse() {
        val clockPrefEnabledNotForegroundCharging = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = true,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = false,
            isCharging = true
        )
        assertFalse(clockPrefEnabledNotForegroundCharging)

        val clockPrefDisabledNotForegroundCharging = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = false,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = false,
            isCharging = true
        )
        assertFalse(clockPrefDisabledNotForegroundCharging)
    }

    @Test
    fun shouldKeepScreenOn_booleanOverload_behavesConsistently() {
        // All conditions satisfied
        assertTrue(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                isClockScreen = true,
                isActivityForeground = true,
                isCharging = true
            )
        )
        // Unplugged -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                isClockScreen = true,
                isActivityForeground = true,
                isCharging = false
            )
        )
        // Preference disabled -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = false,
                isClockScreen = true,
                isActivityForeground = true,
                isCharging = true
            )
        )
        // Not clock screen -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                isClockScreen = false,
                isActivityForeground = true,
                isCharging = true
            )
        )
        // Not foreground -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                isClockScreen = true,
                isActivityForeground = false,
                isCharging = true
            )
        )
    }

    @Test
    fun shouldKeepScreenOn_dynamicChargingStateTransitions_updatePolicyResult() {
        var isCharging = false

        // Device starts on battery -> false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = isCharging
            )
        )

        // User plugs in charger -> becomes true
        isCharging = true
        assertTrue(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = isCharging
            )
        )

        // User unplugs charger -> immediately becomes false
        isCharging = false
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = isCharging
            )
        )
    }
}
