package com.kunvarpreet.standbymode.core.display

import com.kunvarpreet.standbymode.AppScreen

/* Pure policy for determining when the window-level stay-awake flag (FLAG_KEEP_SCREEN_ON)
   should be active.
   Requirements:
   1. The user preference is enabled.
   2. The active screen is the main clock screen (AppScreen.CLOCK).
   3. The Activity is in an active foreground lifecycle state.
*/
object KeepScreenAwakePolicy {

    /* Evaluates whether FLAG_KEEP_SCREEN_ON should be active on the Window.
        @param isPreferenceEnabled whether the user has enabled the keepScreenAwake preference.
        @param currentScreen the currently displayed screen in the app.
        @param isActivityForeground whether the activity is currently in the foreground (resumed/started).
        @return true if and only if all three conditions are met.
    */
    fun shouldKeepScreenOn(
        isPreferenceEnabled: Boolean,
        currentScreen: AppScreen,
        isActivityForeground: Boolean
    ): Boolean {
        return isPreferenceEnabled && currentScreen == AppScreen.CLOCK && isActivityForeground
    }

    // Overload accepting a boolean for whether the current screen is the clock screen.
    fun shouldKeepScreenOn(
        isPreferenceEnabled: Boolean,
        isClockScreen: Boolean,
        isActivityForeground: Boolean
    ): Boolean {
        return isPreferenceEnabled && isClockScreen && isActivityForeground
    }
}
