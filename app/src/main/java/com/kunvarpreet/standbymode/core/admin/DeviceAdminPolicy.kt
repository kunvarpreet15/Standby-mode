package com.kunvarpreet.standbymode.core.admin

import androidx.core.view.WindowInsetsControllerCompat
import com.kunvarpreet.standbymode.AppScreen
import com.kunvarpreet.standbymode.core.activation.LaunchSource

/**
 * Action decision evaluated upon entering an auto-activated StandByMode session.
 */
enum class LockActivationDecision {
    /** Execute DevicePolicyManager.lockNow() immediately. */
    EXECUTE_LOCK,

    /** Display user explanation dialog indicating Device Admin must be activated first. */
    SHOW_ADMIN_SETUP_REQUIRED,

    /** Proceed with normal ambient display without attempting a lock. */
    PROCEED_WITHOUT_LOCK
}

/**
 * Pure policy logic governing Device Admin lock requests and immersive fullscreen behavior.
 */
object DeviceAdminPolicy {

    /**
     * Evaluates whether a device lock or explanation dialog should be triggered
     * upon app entry / notification tap intent delivery.
     *
     * Rules:
     * 1. Only auto-activated sessions ([LaunchSource.AUTO_ACTIVATED]) are eligible. Manual sessions are unaffected.
     * 2. The user preference [isLockPreferenceEnabled] must be true.
     * 3. A lock request must not have already run for the current notification session ([hasExecutedLock] == false).
     * 4. If [isAdminActive] is true, returns [LockActivationDecision.EXECUTE_LOCK].
     * 5. If [isAdminActive] is false, returns [LockActivationDecision.SHOW_ADMIN_SETUP_REQUIRED].
     * 6. Otherwise returns [LockActivationDecision.PROCEED_WITHOUT_LOCK].
     */
    fun evaluateLockDecision(
        isLockPreferenceEnabled: Boolean,
        launchSource: LaunchSource,
        isAdminActive: Boolean,
        hasExecutedLock: Boolean
    ): LockActivationDecision {
        if (!isLockPreferenceEnabled || launchSource != LaunchSource.AUTO_ACTIVATED || hasExecutedLock) {
            return LockActivationDecision.PROCEED_WITHOUT_LOCK
        }
        return if (isAdminActive) {
            LockActivationDecision.EXECUTE_LOCK
        } else {
            LockActivationDecision.SHOW_ADMIN_SETUP_REQUIRED
        }
    }

    /**
     * Determines whether the app window should hide system bars (status & navigation).
     *
     * Rules:
     * - Fullscreen is enabled during auto-activated sessions while viewing [AppScreen.CLOCK].
     * - Fullscreen is dismissed (system bars restored) when viewing [AppScreen.SETTINGS] or [AppScreen.ONBOARDING].
     * - Manual sessions preserve standard system bar visibility.
     */
    fun shouldHideSystemBars(
        launchSource: LaunchSource,
        currentScreen: AppScreen
    ): Boolean {
        return launchSource == LaunchSource.AUTO_ACTIVATED && currentScreen == AppScreen.CLOCK
    }

    /**
     * Returns the transient system bars reveal behavior spec for WindowInsetsControllerCompat.
     */
    fun resolveSystemBarsBehavior(): Int {
        return WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
