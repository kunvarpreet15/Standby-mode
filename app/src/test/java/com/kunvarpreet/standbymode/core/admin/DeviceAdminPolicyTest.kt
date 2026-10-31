package com.kunvarpreet.standbymode.core.admin

import androidx.core.view.WindowInsetsControllerCompat
import com.kunvarpreet.standbymode.AppScreen
import com.kunvarpreet.standbymode.core.activation.LaunchSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAdminPolicyTest {

    // --- 1. Lock Decision Evaluation Tests ---

    @Test
    fun evaluateLockDecision_whenPreferenceDisabled_returnsProceedWithoutLock() {
        val decision = DeviceAdminPolicy.evaluateLockDecision(
            isLockPreferenceEnabled = false,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isAdminActive = true,
            hasExecutedLock = false
        )
        assertEquals(LockActivationDecision.PROCEED_WITHOUT_LOCK, decision)
    }

    @Test
    fun evaluateLockDecision_whenManualLaunch_returnsProceedWithoutLock() {
        val decision = DeviceAdminPolicy.evaluateLockDecision(
            isLockPreferenceEnabled = true,
            launchSource = LaunchSource.MANUAL,
            isAdminActive = true,
            hasExecutedLock = false
        )
        assertEquals(LockActivationDecision.PROCEED_WITHOUT_LOCK, decision)
    }

    @Test
    fun evaluateLockDecision_whenAlreadyExecutedInCurrentSession_returnsProceedWithoutLock() {
        // Simulates recomposition or configuration change (e.g. rotation) after initial lock executed
        val decision = DeviceAdminPolicy.evaluateLockDecision(
            isLockPreferenceEnabled = true,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isAdminActive = true,
            hasExecutedLock = true
        )
        assertEquals(LockActivationDecision.PROCEED_WITHOUT_LOCK, decision)
    }

    @Test
    fun evaluateLockDecision_whenAutoActivated_andAdminActive_returnsExecuteLock() {
        val decision = DeviceAdminPolicy.evaluateLockDecision(
            isLockPreferenceEnabled = true,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isAdminActive = true,
            hasExecutedLock = false
        )
        assertEquals(LockActivationDecision.EXECUTE_LOCK, decision)
    }

    @Test
    fun evaluateLockDecision_whenAutoActivated_andAdminInactive_returnsShowAdminSetupRequired() {
        val decision = DeviceAdminPolicy.evaluateLockDecision(
            isLockPreferenceEnabled = true,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isAdminActive = false,
            hasExecutedLock = false
        )
        assertEquals(LockActivationDecision.SHOW_ADMIN_SETUP_REQUIRED, decision)
    }

    @Test
    fun evaluateLockDecision_newNotificationIntent_resetsExecutionFlagAllowingSubsequentLock() {
        // Initial tap: executes lock
        val initialDecision = DeviceAdminPolicy.evaluateLockDecision(
            isLockPreferenceEnabled = true,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isAdminActive = true,
            hasExecutedLock = false
        )
        assertEquals(LockActivationDecision.EXECUTE_LOCK, initialDecision)

        // Session continues (rotation/recomposition): suppressed
        val ongoingDecision = DeviceAdminPolicy.evaluateLockDecision(
            isLockPreferenceEnabled = true,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isAdminActive = true,
            hasExecutedLock = true
        )
        assertEquals(LockActivationDecision.PROCEED_WITHOUT_LOCK, ongoingDecision)

        // New notification intent arrives: flag resets to false
        val newIntentDecision = DeviceAdminPolicy.evaluateLockDecision(
            isLockPreferenceEnabled = true,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isAdminActive = true,
            hasExecutedLock = false
        )
        assertEquals(LockActivationDecision.EXECUTE_LOCK, newIntentDecision)
    }

    // --- 2. System Bars / Fullscreen Inset Visibility Tests ---

    @Test
    fun shouldHideSystemBars_whenAutoActivated_andClockScreen_returnsTrue() {
        val hideBars = DeviceAdminPolicy.shouldHideSystemBars(
            launchSource = LaunchSource.AUTO_ACTIVATED,
            currentScreen = AppScreen.CLOCK
        )
        assertTrue(hideBars)
    }

    @Test
    fun shouldHideSystemBars_whenAutoActivated_andSettingsScreen_returnsFalse() {
        val hideBars = DeviceAdminPolicy.shouldHideSystemBars(
            launchSource = LaunchSource.AUTO_ACTIVATED,
            currentScreen = AppScreen.SETTINGS
        )
        assertFalse("System bars must be restored when entering Settings", hideBars)
    }

    @Test
    fun shouldHideSystemBars_whenAutoActivated_andOnboardingScreen_returnsFalse() {
        val hideBars = DeviceAdminPolicy.shouldHideSystemBars(
            launchSource = LaunchSource.AUTO_ACTIVATED,
            currentScreen = AppScreen.ONBOARDING
        )
        assertFalse("System bars must be restored when entering Onboarding", hideBars)
    }

    @Test
    fun shouldHideSystemBars_whenManualLaunch_andClockScreen_returnsFalse() {
        val hideBars = DeviceAdminPolicy.shouldHideSystemBars(
            launchSource = LaunchSource.MANUAL,
            currentScreen = AppScreen.CLOCK
        )
        assertFalse("Manual launches preserve normal system bar visibility", hideBars)
    }

    @Test
    fun shouldHideSystemBars_whenManualLaunch_andSettingsScreen_returnsFalse() {
        val hideBars = DeviceAdminPolicy.shouldHideSystemBars(
            launchSource = LaunchSource.MANUAL,
            currentScreen = AppScreen.SETTINGS
        )
        assertFalse(hideBars)
    }

    @Test
    fun resolveSystemBarsBehavior_returnsTransientSwipeBehavior() {
        assertEquals(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            DeviceAdminPolicy.resolveSystemBarsBehavior()
        )
    }
}
