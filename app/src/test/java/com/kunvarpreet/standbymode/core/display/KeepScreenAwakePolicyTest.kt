package com.kunvarpreet.standbymode.core.display

import com.kunvarpreet.standbymode.AppScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepScreenAwakePolicyTest {

    @Test
    fun shouldKeepScreenOn_whenPreferenceEnabled_clockScreen_andForegroundActivity_returnsTrue() {
        val result = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = true,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = true
        )
        assertTrue(result)
    }

    @Test
    fun shouldKeepScreenOn_whenPreferenceDisabled_returnsFalse() {
        val onClockForeground = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = false,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = true
        )
        assertFalse(onClockForeground)

        val onClockBackground = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = false,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = false
        )
        assertFalse(onClockBackground)

        val onSettingsForeground = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = false,
            currentScreen = AppScreen.SETTINGS,
            isActivityForeground = true
        )
        assertFalse(onSettingsForeground)
    }

    @Test
    fun shouldKeepScreenOn_whenSettingsScreen_returnsFalse() {
        val settingsForegroundPrefEnabled = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = true,
            currentScreen = AppScreen.SETTINGS,
            isActivityForeground = true
        )
        assertFalse(settingsForegroundPrefEnabled)

        val settingsForegroundPrefDisabled = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = false,
            currentScreen = AppScreen.SETTINGS,
            isActivityForeground = true
        )
        assertFalse(settingsForegroundPrefDisabled)

        val settingsBackground = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = true,
            currentScreen = AppScreen.SETTINGS,
            isActivityForeground = false
        )
        assertFalse(settingsBackground)
    }

    @Test
    fun shouldKeepScreenOn_whenActivityNotInForeground_returnsFalse() {
        val clockPrefEnabledNotForeground = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = true,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = false
        )
        assertFalse(clockPrefEnabledNotForeground)

        val clockPrefDisabledNotForeground = KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = false,
            currentScreen = AppScreen.CLOCK,
            isActivityForeground = false
        )
        assertFalse(clockPrefDisabledNotForeground)
    }

    @Test
    fun shouldKeepScreenOn_booleanOverload_behavesConsistently() {
        assertTrue(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                isClockScreen = true,
                isActivityForeground = true
            )
        )
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = false,
                isClockScreen = true,
                isActivityForeground = true
            )
        )
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                isClockScreen = false,
                isActivityForeground = true
            )
        )
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                isClockScreen = true,
                isActivityForeground = false
            )
        )
    }
}
