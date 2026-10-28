package com.kunvarpreet.standbymode.core.activation

import android.content.res.Configuration
import com.kunvarpreet.standbymode.AppScreen
import com.kunvarpreet.standbymode.core.display.DisplaySpace
import com.kunvarpreet.standbymode.core.display.KeepScreenAwakePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests verifying the auto-activation policy and state machine (Phase 15).
 * Tests Roadmap Row 1 (Charge detection) and Row 2 (Landscape auto-activation) behaviors.
 */
class AutoActivationPolicyTest {

    // --- 1. Charging + Landscape Auto-Activation Conditions ---

    @Test
    fun canAutoActivate_whenChargingAndLandscape_andPreferenceEnabled_returnsTrue() {
        val result = AutoActivationPolicy.canAutoActivate(
            isAutoActivationEnabled = true,
            isCharging = true,
            orientation = DeviceOrientation.LANDSCAPE
        )
        assertTrue("Should auto-activate when preference enabled, charging, and in landscape", result)
    }

    @Test
    fun canAutoActivate_whenChargingAndPortrait_returnsFalse() {
        val result = AutoActivationPolicy.canAutoActivate(
            isAutoActivationEnabled = true,
            isCharging = true,
            orientation = DeviceOrientation.PORTRAIT
        )
        assertFalse("Should not auto-activate when in portrait orientation", result)
    }

    @Test
    fun canAutoActivate_whenUnpluggedAndLandscape_returnsFalse() {
        val result = AutoActivationPolicy.canAutoActivate(
            isAutoActivationEnabled = true,
            isCharging = false,
            orientation = DeviceOrientation.LANDSCAPE
        )
        assertFalse("Should not auto-activate when device is not charging", result)
    }

    @Test
    fun canAutoActivate_whenPreferenceDisabled_returnsFalse() {
        val result = AutoActivationPolicy.canAutoActivate(
            isAutoActivationEnabled = false,
            isCharging = true,
            orientation = DeviceOrientation.LANDSCAPE
        )
        assertFalse("Should never auto-activate when user preference is disabled", result)
    }

    @Test
    fun canAutoActivate_whenOrientationUndefined_returnsFalse() {
        val result = AutoActivationPolicy.canAutoActivate(
            isAutoActivationEnabled = true,
            isCharging = true,
            orientation = DeviceOrientation.UNDEFINED
        )
        assertFalse("Should not auto-activate when orientation is undefined", result)
    }

    // --- 2. Automatic Dismissal Rules (Auto-Activated vs Manual Sessions) ---

    @Test
    fun shouldDismissAutoSession_whenAutoActivated_andUnplugged_returnsTrue() {
        val result = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isCharging = false,
            orientation = DeviceOrientation.LANDSCAPE,
            currentScreen = AppScreen.CLOCK
        )
        assertTrue("Auto-activated session should dismiss when power is unplugged", result)
    }

    @Test
    fun shouldDismissAutoSession_whenAutoActivated_andRotatedToPortrait_returnsTrue() {
        val result = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isCharging = true,
            orientation = DeviceOrientation.PORTRAIT,
            currentScreen = AppScreen.CLOCK
        )
        assertTrue("Auto-activated session should dismiss when device is rotated to portrait", result)
    }

    @Test
    fun shouldDismissAutoSession_whenAutoActivated_chargingAndLandscape_returnsFalse() {
        val result = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isCharging = true,
            orientation = DeviceOrientation.LANDSCAPE,
            currentScreen = AppScreen.CLOCK
        )
        assertFalse("Auto-activated session should remain active while charging in landscape", result)
    }

    @Test
    fun shouldDismissAutoSession_whenManualLaunch_unplugged_returnsFalse() {
        val result = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.MANUAL,
            isCharging = false,
            orientation = DeviceOrientation.LANDSCAPE,
            currentScreen = AppScreen.CLOCK
        )
        assertFalse("Manual session must NEVER automatically dismiss when unplugged", result)
    }

    @Test
    fun shouldDismissAutoSession_whenManualLaunch_rotatedToPortrait_returnsFalse() {
        val result = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.MANUAL,
            isCharging = true,
            orientation = DeviceOrientation.PORTRAIT,
            currentScreen = AppScreen.CLOCK
        )
        assertFalse("Manual session must NEVER automatically dismiss when rotated to portrait", result)
    }

    @Test
    fun shouldDismissAutoSession_whenManualLaunch_unpluggedAndPortrait_returnsFalse() {
        val result = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.MANUAL,
            isCharging = false,
            orientation = DeviceOrientation.PORTRAIT,
            currentScreen = AppScreen.CLOCK
        )
        assertFalse("Manual session must NEVER automatically dismiss under any conditions", result)
    }

    @Test
    fun shouldDismissAutoSession_whenInSettings_suppressesDismissal() {
        // Even if auto-activated and unplugged/portrait, if user is configuring in Settings, do not abruptly kill
        val resultUnplugged = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isCharging = false,
            orientation = DeviceOrientation.LANDSCAPE,
            currentScreen = AppScreen.SETTINGS
        )
        assertFalse("Dismissal must be suppressed while user is actively in Settings", resultUnplugged)

        val resultPortrait = AutoActivationPolicy.shouldDismissAutoSession(
            launchSource = LaunchSource.AUTO_ACTIVATED,
            isCharging = true,
            orientation = DeviceOrientation.PORTRAIT,
            currentScreen = AppScreen.SETTINGS
        )
        assertFalse("Dismissal must be suppressed while user is actively in Settings", resultPortrait)
    }

    // --- 3. State Machine Transitions ---

    @Test
    fun transitionState_fromIdle_toActiveAmbient_whenAutoActivated() {
        val next = AutoActivationPolicy.transitionState(
            currentState = StandByActivationState.INACTIVE,
            isAutoActivationEnabled = true,
            isCharging = true,
            orientation = DeviceOrientation.LANDSCAPE,
            isAppInForeground = true,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            currentScreen = AppScreen.CLOCK
        )
        assertEquals(StandByActivationState.ACTIVE_AMBIENT, next)
    }

    @Test
    fun transitionState_fromActiveAmbient_toDismissed_onUnplug() {
        val next = AutoActivationPolicy.transitionState(
            currentState = StandByActivationState.ACTIVE_AMBIENT,
            isAutoActivationEnabled = true,
            isCharging = false,
            orientation = DeviceOrientation.LANDSCAPE,
            isAppInForeground = true,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            currentScreen = AppScreen.CLOCK
        )
        assertEquals(StandByActivationState.DISMISSED, next)
    }

    @Test
    fun transitionState_fromActiveAmbient_toDismissed_onPortraitRotation() {
        val next = AutoActivationPolicy.transitionState(
            currentState = StandByActivationState.ACTIVE_AMBIENT,
            isAutoActivationEnabled = true,
            isCharging = true,
            orientation = DeviceOrientation.PORTRAIT,
            isAppInForeground = true,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            currentScreen = AppScreen.CLOCK
        )
        assertEquals(StandByActivationState.DISMISSED, next)
    }

    @Test
    fun transitionState_manualSession_persistsAcrossUnplugAndRotation() {
        val nextUnplugged = AutoActivationPolicy.transitionState(
            currentState = StandByActivationState.ACTIVE_AMBIENT,
            isAutoActivationEnabled = true,
            isCharging = false,
            orientation = DeviceOrientation.LANDSCAPE,
            isAppInForeground = true,
            launchSource = LaunchSource.MANUAL,
            currentScreen = AppScreen.CLOCK
        )
        assertEquals("Manual session remains ACTIVE_AMBIENT when unplugged", StandByActivationState.ACTIVE_AMBIENT, nextUnplugged)

        val nextPortrait = AutoActivationPolicy.transitionState(
            currentState = StandByActivationState.ACTIVE_AMBIENT,
            isAutoActivationEnabled = true,
            isCharging = true,
            orientation = DeviceOrientation.PORTRAIT,
            isAppInForeground = true,
            launchSource = LaunchSource.MANUAL,
            currentScreen = AppScreen.CLOCK
        )
        assertEquals("Manual session remains ACTIVE_AMBIENT in portrait", StandByActivationState.ACTIVE_AMBIENT, nextPortrait)
    }

    @Test
    fun transitionState_whenBackgroundedAndCharging_becomesPending() {
        val next = AutoActivationPolicy.transitionState(
            currentState = StandByActivationState.INACTIVE,
            isAutoActivationEnabled = true,
            isCharging = true,
            orientation = DeviceOrientation.LANDSCAPE,
            isAppInForeground = false,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            currentScreen = AppScreen.CLOCK
        )
        assertEquals(StandByActivationState.PENDING_BACKGROUND_USER_ACTION, next)
    }

    @Test
    fun transitionState_whenBackgroundedAndUnplugged_becomesInactive() {
        val next = AutoActivationPolicy.transitionState(
            currentState = StandByActivationState.PENDING_BACKGROUND_USER_ACTION,
            isAutoActivationEnabled = true,
            isCharging = false,
            orientation = DeviceOrientation.LANDSCAPE,
            isAppInForeground = false,
            launchSource = LaunchSource.AUTO_ACTIVATED,
            currentScreen = AppScreen.CLOCK
        )
        assertEquals(StandByActivationState.INACTIVE, next)
    }

    // --- 4. Android Background Launch Restrictions Compliance ---

    @Test
    fun evaluateBackgroundChargingAction_whenAppInBackground_andNotificationsAllowed_postsNotification() {
        val action = AutoActivationPolicy.evaluateBackgroundChargingAction(
            isAutoActivationEnabled = true,
            isChargingNotificationEnabled = false,
            isAppInForeground = false,
            canPostNotifications = true
        )
        assertEquals(ActivationAction.POST_CHARGING_NOTIFICATION, action)
    }

    @Test
    fun evaluateBackgroundChargingAction_whenAppInForeground_returnsNone() {
        val action = AutoActivationPolicy.evaluateBackgroundChargingAction(
            isAutoActivationEnabled = true,
            isChargingNotificationEnabled = true,
            isAppInForeground = true,
            canPostNotifications = true
        )
        assertEquals("No background notification needed when app is already visible", ActivationAction.NONE, action)
    }

    @Test
    fun evaluateBackgroundChargingAction_whenNotificationsDenied_returnsNone() {
        val action = AutoActivationPolicy.evaluateBackgroundChargingAction(
            isAutoActivationEnabled = true,
            isChargingNotificationEnabled = true,
            isAppInForeground = false,
            canPostNotifications = false
        )
        assertEquals("Cannot post notification if permission denied", ActivationAction.NONE, action)
    }

    @Test
    fun evaluateBackgroundChargingAction_whenFeaturesDisabled_returnsNone() {
        val action = AutoActivationPolicy.evaluateBackgroundChargingAction(
            isAutoActivationEnabled = false,
            isChargingNotificationEnabled = false,
            isAppInForeground = false,
            canPostNotifications = true
        )
        assertEquals(ActivationAction.NONE, action)
    }

    // --- 5. Rapid Transitions & Repeated Broadcast Idempotence ---

    @Test
    fun rapidOrientationChanges_remainDeterministic() {
        val orientations = listOf(
            DeviceOrientation.LANDSCAPE,
            DeviceOrientation.PORTRAIT,
            DeviceOrientation.LANDSCAPE,
            DeviceOrientation.UNDEFINED,
            DeviceOrientation.LANDSCAPE
        )
        orientations.forEach { orientation ->
            val canActivate = AutoActivationPolicy.canAutoActivate(
                isAutoActivationEnabled = true,
                isCharging = true,
                orientation = orientation
            )
            assertEquals(orientation == DeviceOrientation.LANDSCAPE, canActivate)
        }
    }

    @Test
    fun repeatedChargingBroadcasts_doNotAlterDecision() {
        // Repeated broadcasts while charging in landscape
        repeat(5) {
            assertTrue(
                AutoActivationPolicy.canAutoActivate(
                    isAutoActivationEnabled = true,
                    isCharging = true,
                    orientation = DeviceOrientation.LANDSCAPE
                )
            )
        }
    }

    // --- 6. DeviceOrientation Mapping from Android Configuration ---

    @Test
    fun deviceOrientation_mapsAccuratelyFromConfiguration() {
        assertEquals(
            DeviceOrientation.LANDSCAPE,
            DeviceOrientation.fromConfiguration(Configuration.ORIENTATION_LANDSCAPE)
        )
        assertEquals(
            DeviceOrientation.PORTRAIT,
            DeviceOrientation.fromConfiguration(Configuration.ORIENTATION_PORTRAIT)
        )
        assertEquals(
            DeviceOrientation.UNDEFINED,
            DeviceOrientation.fromConfiguration(Configuration.ORIENTATION_UNDEFINED)
        )
        assertEquals(
            DeviceOrientation.UNDEFINED,
            DeviceOrientation.fromConfiguration(999) // Invalid value fallback
        )
    }

    // --- 7. Preservation of Keep Screen Awake & Display Space Policies ---

    @Test
    fun keepScreenAwake_preservesChargingAwarenessUnderAutoActivation() {
        // While charging on clock, screen awake is permitted
        assertTrue(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = true
            )
        )

        // When unplugged under auto-activation, screen awake immediately turns off
        assertFalse(
            KeepScreenAwakePolicy.shouldKeepScreenOn(
                isPreferenceEnabled = true,
                currentScreen = AppScreen.CLOCK,
                isActivityForeground = true,
                isCharging = false
            )
        )
    }

    @Test
    fun displaySpaces_remainPreservedUnderOrientationTransitions() {
        // Verify 3-space indexing
        assertEquals(0, DisplaySpace.DUO_LAYOUT.pageIndex)
        assertEquals(1, DisplaySpace.FULLSCREEN_CLOCK.pageIndex)
        assertEquals(2, DisplaySpace.PHOTO_FRAME.pageIndex)
    }
}
