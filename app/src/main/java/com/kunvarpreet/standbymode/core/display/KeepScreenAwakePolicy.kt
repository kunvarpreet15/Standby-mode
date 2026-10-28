package com.kunvarpreet.standbymode.core.display

import com.kunvarpreet.standbymode.AppScreen

/**
 * Pure policy for determining when the window-level stay-awake flag (FLAG_KEEP_SCREEN_ON)
 * should be active.
 *
 * Requirements:
 * 1. The user preference is enabled (keepScreenAwake).
 * 2. The active screen is an eligible ambient display screen (AppScreen.CLOCK).
 * 3. The Activity is in an active foreground lifecycle state.
 * 4. The device is connected to power / charging.
 */
object KeepScreenAwakePolicy {

    /**
     * Evaluates whether FLAG_KEEP_SCREEN_ON should be active on the Window.
     *
     * @param isPreferenceEnabled whether the user has enabled the keepScreenAwake preference.
     * @param currentScreen the currently displayed screen in the app.
     * @param isActivityForeground whether the activity is currently in the foreground (resumed/started).
     * @param isCharging whether the device is currently connected to power / charging.
     * @return true if and only if all four conditions are met.
     */
    fun shouldKeepScreenOn(
        isPreferenceEnabled: Boolean,
        currentScreen: AppScreen,
        isActivityForeground: Boolean,
        isCharging: Boolean
    ): Boolean {
        val isAmbientDisplay = currentScreen == AppScreen.CLOCK
        return isPreferenceEnabled && isAmbientDisplay && isActivityForeground && isCharging
    }

    /**
     * Overload accepting a boolean for whether the current screen is the clock/ambient screen.
     */
    fun shouldKeepScreenOn(
        isPreferenceEnabled: Boolean,
        isClockScreen: Boolean,
        isActivityForeground: Boolean,
        isCharging: Boolean
    ): Boolean {
        return isPreferenceEnabled && isClockScreen && isActivityForeground && isCharging
    }
}
