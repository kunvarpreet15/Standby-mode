package com.kunvarpreet.standbymode.core.onboarding

import com.kunvarpreet.standbymode.AppScreen
import com.kunvarpreet.standbymode.core.activation.AutoActivationPolicy
import com.kunvarpreet.standbymode.core.activation.DeviceOrientation
import com.kunvarpreet.standbymode.core.activation.LaunchSource
import com.kunvarpreet.standbymode.core.display.DisplaySpace
import com.kunvarpreet.standbymode.core.display.KeepScreenAwakePolicy
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests verifying the onboarding navigation policy, step ordering,
 * completion states, and integration with auto-activation and display policies (Phase 16).
 */
class OnboardingPolicyTest {

    // --- 1. First Launch vs Returning User ---

    @Test
    fun shouldShowOnboardingOnLaunch_forNewUser_returnsTrue() {
        val prefs = UserClockPreferences() // default: isOnboardingCompleted = false
        val shouldShow = OnboardingPolicy.shouldShowOnboardingOnLaunch(prefs.isOnboardingCompleted)
        assertTrue("Brand new installation must present the onboarding walkthrough", shouldShow)
    }

    @Test
    fun shouldShowOnboardingOnLaunch_forReturningUser_returnsFalse() {
        val prefs = UserClockPreferences(isOnboardingCompleted = true)
        val shouldShow = OnboardingPolicy.shouldShowOnboardingOnLaunch(prefs.isOnboardingCompleted)
        assertFalse("Returning user who completed onboarding must bypass directly to main screen", shouldShow)
    }

    // --- 2. Step Ordering & Progression ---

    @Test
    fun onboardingSteps_haveCorrectOrderingAndIndices() {
        assertEquals(0, OnboardingStep.WELCOME.stepIndex)
        assertEquals(1, OnboardingStep.CHARGING_ORIENTATION.stepIndex)
        assertEquals(2, OnboardingStep.DISPLAY_RULES.stepIndex)
        assertEquals(3, OnboardingStep.PERMISSIONS_INTEGRATIONS.stepIndex)
        assertEquals(4, OnboardingStep.READY.stepIndex)
        assertEquals(5, OnboardingStep.TOTAL_STEPS)
    }

    @Test
    fun getNextStep_advancesLinearlyThroughFlow() {
        val step0 = OnboardingStep.WELCOME
        val step1 = OnboardingPolicy.getNextStep(step0)
        assertEquals(OnboardingStep.CHARGING_ORIENTATION, step1)

        val step2 = OnboardingPolicy.getNextStep(step1!!)
        assertEquals(OnboardingStep.DISPLAY_RULES, step2)

        val step3 = OnboardingPolicy.getNextStep(step2!!)
        assertEquals(OnboardingStep.PERMISSIONS_INTEGRATIONS, step3)

        val step4 = OnboardingPolicy.getNextStep(step3!!)
        assertEquals(OnboardingStep.READY, step4)

        val step5 = OnboardingPolicy.getNextStep(step4!!)
        assertNull("Final step must return null for next step", step5)
    }

    @Test
    fun getPreviousStep_retreatsLinearlyThroughFlow() {
        val step4 = OnboardingStep.READY
        val step3 = OnboardingPolicy.getPreviousStep(step4)
        assertEquals(OnboardingStep.PERMISSIONS_INTEGRATIONS, step3)

        val step2 = OnboardingPolicy.getPreviousStep(step3!!)
        assertEquals(OnboardingStep.DISPLAY_RULES, step2)

        val step1 = OnboardingPolicy.getPreviousStep(step2!!)
        assertEquals(OnboardingStep.CHARGING_ORIENTATION, step1)

        val step0 = OnboardingPolicy.getPreviousStep(step1!!)
        assertEquals(OnboardingStep.WELCOME, step0)

        val stepPrev = OnboardingPolicy.getPreviousStep(step0!!)
        assertNull("Initial step must return null for previous step", stepPrev)
    }

    @Test
    fun canNavigateBack_returnsTrueOnlyWhenPastInitialStep() {
        assertFalse("Cannot navigate back on initial step", OnboardingPolicy.canNavigateBack(OnboardingStep.WELCOME))
        assertTrue(OnboardingPolicy.canNavigateBack(OnboardingStep.CHARGING_ORIENTATION))
        assertTrue(OnboardingPolicy.canNavigateBack(OnboardingStep.DISPLAY_RULES))
        assertTrue(OnboardingPolicy.canNavigateBack(OnboardingStep.PERMISSIONS_INTEGRATIONS))
        assertTrue(OnboardingPolicy.canNavigateBack(OnboardingStep.READY))
    }

    @Test
    fun isFinalStep_returnsTrueOnlyForReadyStep() {
        assertFalse(OnboardingPolicy.isFinalStep(OnboardingStep.WELCOME))
        assertFalse(OnboardingPolicy.isFinalStep(OnboardingStep.CHARGING_ORIENTATION))
        assertFalse(OnboardingPolicy.isFinalStep(OnboardingStep.DISPLAY_RULES))
        assertFalse(OnboardingPolicy.isFinalStep(OnboardingStep.PERMISSIONS_INTEGRATIONS))
        assertTrue(OnboardingPolicy.isFinalStep(OnboardingStep.READY))
    }

    // --- 3. Exit and Re-entry Destination Resolution ---

    @Test
    fun resolveExitScreen_whenFirstRun_returnsClock() {
        val destination = OnboardingPolicy.resolveExitScreen(wasPreviouslyCompleted = false)
        assertEquals("First-run completion must navigate into ambient CLOCK", AppScreen.CLOCK, destination)
    }

    @Test
    fun resolveExitScreen_whenRevisitedFromSettings_returnsSettings() {
        val destination = OnboardingPolicy.resolveExitScreen(wasPreviouslyCompleted = true)
        assertEquals("Revisiting onboarding from Settings must return back to SETTINGS", AppScreen.SETTINGS, destination)
    }

    // --- 4. Protection Against Auto-Activation Dismissal During Onboarding ---

    @Test
    fun shouldDismissAutoSession_isSuppressedDuringOnboarding() {
        // Even if session was auto-activated and device is unplugged or upright,
        // onboarding must never be abruptly dismissed/closed.
        val suppressedWhenUnplugged = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isCharging = false,
            orientation = DeviceOrientation.LANDSCAPE,
            currentScreen = AppScreen.ONBOARDING
        )
        assertFalse("Dismissal must be suppressed while user is viewing ONBOARDING", suppressedWhenUnplugged)

        val suppressedWhenPortrait = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isCharging = true,
            orientation = DeviceOrientation.PORTRAIT,
            currentScreen = AppScreen.ONBOARDING
        )
        assertFalse("Dismissal must be suppressed while user is viewing ONBOARDING", suppressedWhenPortrait)
    }

    // --- 5. Independence from Manual Launch & Existing Display Policies ---

    @Test
    fun manualLaunch_remainsIndependentOfOnboardingState() {
        val prefsCompleted = UserClockPreferences(isOnboardingCompleted = true)
        val shouldDismissManual = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.MANUAL,
            isCharging = false,
            orientation = DeviceOrientation.PORTRAIT,
            currentScreen = AppScreen.CLOCK
        )
        assertFalse("Manual session must not be dismissed regardless of onboarding state", shouldDismissManual)
    }

    @Test
    fun displaySpaces_remainPreservedAfterOnboardingCompletion() {
        val defaultSpace = DisplaySpace.fromPageIndex(DisplaySpace.DEFAULT_PAGE_INDEX)
        assertEquals(DisplaySpace.DUO_LAYOUT, defaultSpace)
        assertEquals(0, DisplaySpace.DUO_LAYOUT.pageIndex)
        assertEquals(1, DisplaySpace.FULLSCREEN_CLOCK.pageIndex)
        assertEquals(2, DisplaySpace.PHOTO_FRAME.pageIndex)
    }

    @Test
    fun keepScreenAwake_preservesChargingAwareness() {
        // KeepScreenAwake continues to require isCharging == true on CLOCK screen
        assertTrue(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = true
            )
        )
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = false
            )
        )
        // On ONBOARDING screen, KeepScreenAwake is inactive
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.ONBOARDING,
                isActivityForeground = true,
                isCharging = true
            )
        )
    }
}
