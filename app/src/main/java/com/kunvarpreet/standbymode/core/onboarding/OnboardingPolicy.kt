package com.kunvarpreet.standbymode.core.onboarding

import com.kunvarpreet.standbymode.AppScreen

/**
 * Ordered steps comprising the StandByMode first-run walkthrough.
 */
enum class OnboardingStep(val stepIndex: Int, val header: String) {
    WELCOME(0, "Welcome to StandByMode"),
    CHARGING_ORIENTATION(1, "Charging & Orientation"),
    DISPLAY_RULES(2, "Screen & Night Mode"),
    PERMISSIONS_INTEGRATIONS(3, "Optional Features"),
    READY(4, "You're All Set");

    companion object {
        val TOTAL_STEPS = entries.size

        fun fromIndex(index: Int): OnboardingStep {
            return entries.firstOrNull { it.stepIndex == index } ?: WELCOME
        }
    }
}

/**
 * Pure policy logic for onboarding navigation, completion rules, and permission state handling.
 * Completely independent of the Android UI toolkit for robust, isolated unit testing.
 */
object OnboardingPolicy {

    /**
     * Determines whether the onboarding walkthrough should be shown on initial application startup.
     * Genuine new installations (completed = false) see onboarding; returning users bypass directly to CLOCK.
     */
    fun shouldShowOnboardingOnLaunch(isOnboardingCompleted: Boolean): Boolean {
        return !isOnboardingCompleted
    }

    /**
     * Returns the next step in the flow, or null if currently on the final step.
     */
    fun getNextStep(current: OnboardingStep): OnboardingStep? {
        val nextIndex = current.stepIndex + 1
        return if (nextIndex < OnboardingStep.TOTAL_STEPS) {
            OnboardingStep.fromIndex(nextIndex)
        } else {
            null
        }
    }

    /**
     * Returns the previous step in the flow, or null if currently on the initial step.
     */
    fun getPreviousStep(current: OnboardingStep): OnboardingStep? {
        val prevIndex = current.stepIndex - 1
        return if (prevIndex >= 0) {
            OnboardingStep.fromIndex(prevIndex)
        } else {
            null
        }
    }

    /**
     * Determines if a back navigation can be performed within the onboarding wizard.
     */
    fun canNavigateBack(current: OnboardingStep): Boolean {
        return current.stepIndex > 0
    }

    /**
     * Determines whether the current step is the final completion step.
     */
    fun isFinalStep(current: OnboardingStep): Boolean {
        return current == OnboardingStep.READY
    }

    /**
     * Resolves the target screen when exiting the walkthrough.
     * If the user previously completed onboarding and revisited from Settings,
     * they return to Settings; otherwise they proceed to the main ambient CLOCK.
     */
    fun resolveExitScreen(wasPreviouslyCompleted: Boolean): AppScreen {
        return if (wasPreviouslyCompleted) {
            AppScreen.SETTINGS
        } else {
            AppScreen.CLOCK
        }
    }
}
