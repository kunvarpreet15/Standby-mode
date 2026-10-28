package com.kunvarpreet.standbymode.core.activation

import android.content.res.Configuration
import com.kunvarpreet.standbymode.AppScreen

/**
 * Screen orientation classified for StandByMode activation.
 */
enum class DeviceOrientation {
    PORTRAIT,
    LANDSCAPE,
    UNDEFINED;

    companion object {
        fun fromConfiguration(orientation: Int): DeviceOrientation {
            return when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> LANDSCAPE
                Configuration.ORIENTATION_PORTRAIT -> PORTRAIT
                else -> UNDEFINED
            }
        }
    }
}

/**
 * Origin of the current StandByMode session.
 */
enum class LaunchSource {
    /** User launched the app explicitly from the launcher or app drawer. */
    MANUAL,

    /** App was entered automatically via charging trigger or charging notification. */
    AUTO_ACTIVATED
}

/**
 * Discrete lifecycle and presentation states for the auto-activation engine.
 */
enum class StandByActivationState {
    /** App is not active or conditions for StandBy ambient mode are unmet. */
    INACTIVE,

    /** Device connected to power while app is backgrounded; awaiting user action via notification. */
    PENDING_BACKGROUND_USER_ACTION,

    /** Active ambient StandBy display mode (clock / widgets / photo frame). */
    ACTIVE_AMBIENT,

    /** Session dismissed due to unplug or rotation back to portrait. */
    DISMISSED
}

/**
 * Discrete actions recommended by the activation policy.
 */
enum class ActivationAction {
    NONE,
    ACTIVATE_AMBIENT,
    DISMISS_SESSION,
    POST_CHARGING_NOTIFICATION,
    CANCEL_CHARGING_NOTIFICATION
}

/**
 * Pure policy and state machine governing StandByMode charge detection and orientation-based activation.
 *
 * Implements Roadmap Row 1 (Auto-start on charge detection) and Row 2 (Landscape orientation auto-activation)
 * while strictly complying with Android 10+ (API 29+) Background Activity Launch (BAL) restrictions.
 */
object AutoActivationPolicy {

    const val EXTRA_AUTO_ACTIVATED = "com.kunvarpreet.standbymode.extra.AUTO_ACTIVATED"

    /**
     * Evaluates whether device conditions satisfy auto-activation into StandByMode:
     * - Feature preference is enabled
     * - Device is connected to power (isCharging = true)
     * - Device is in landscape orientation
     */
    fun canAutoActivate(
        isAutoActivationEnabled: Boolean,
        isCharging: Boolean,
        orientation: DeviceOrientation
    ): Boolean {
        return isAutoActivationEnabled && isCharging && orientation == DeviceOrientation.LANDSCAPE
    }

    /**
     * Determines whether an active session should be automatically dismissed.
     *
     * Rules:
     * 1. Only sessions started via [LaunchSource.AUTO_ACTIVATED] are eligible for automatic dismissal.
     *    Manual user sessions ([LaunchSource.MANUAL]) are NEVER automatically dismissed on unplug or rotation.
     * 2. If the user is currently in [AppScreen.SETTINGS], dismissal is suppressed so active configuration is not interrupted.
     * 3. An auto-activated session is dismissed if the device is unplugged (!isCharging) OR rotated to portrait.
     */
    fun shouldDismissAutoSession(
        launchSource: LaunchSource,
        isCharging: Boolean,
        orientation: DeviceOrientation,
        currentScreen: AppScreen = AppScreen.CLOCK
    ): Boolean {
        if (launchSource != LaunchSource.AUTO_ACTIVATED) {
            return false
        }
        if (currentScreen == AppScreen.SETTINGS) {
            return false
        }
        return !isCharging || orientation == DeviceOrientation.PORTRAIT
    }

    /**
     * Resolves the next [StandByActivationState] given current state and environment inputs.
     */
    fun transitionState(
        currentState: StandByActivationState,
        isAutoActivationEnabled: Boolean,
        isCharging: Boolean,
        orientation: DeviceOrientation,
        isAppInForeground: Boolean,
        launchSource: LaunchSource,
        currentScreen: AppScreen = AppScreen.CLOCK
    ): StandByActivationState {
        if (!isAutoActivationEnabled && launchSource != LaunchSource.AUTO_ACTIVATED) {
            return if (isAppInForeground && currentScreen == AppScreen.CLOCK) {
                StandByActivationState.ACTIVE_AMBIENT
            } else {
                StandByActivationState.INACTIVE
            }
        }

        if (!isAppInForeground) {
            return if (isCharging) {
                StandByActivationState.PENDING_BACKGROUND_USER_ACTION
            } else {
                StandByActivationState.INACTIVE
            }
        }

        // App is in foreground
        if (launchSource == LaunchSource.AUTO_ACTIVATED) {
            if (shouldDismissAutoSession(launchSource, isCharging, orientation, currentScreen)) {
                return StandByActivationState.DISMISSED
            }
            return StandByActivationState.ACTIVE_AMBIENT
        }

        // Manual launch in foreground
        return if (currentScreen == AppScreen.CLOCK) {
            StandByActivationState.ACTIVE_AMBIENT
        } else {
            StandByActivationState.INACTIVE
        }
    }

    /**
     * Evaluates background charging actions for [ChargingReceiver] when power is connected.
     *
     * Respects Android Background Activity Launch (BAL) restrictions by dispatching a notification
     * with a PendingIntent rather than attempting an illegal background startActivity().
     */
    fun evaluateBackgroundChargingAction(
        isAutoActivationEnabled: Boolean,
        isChargingNotificationEnabled: Boolean,
        isAppInForeground: Boolean,
        canPostNotifications: Boolean
    ): ActivationAction {
        if (isAppInForeground) {
            return ActivationAction.NONE
        }
        val shouldNotify = (isAutoActivationEnabled || isChargingNotificationEnabled) && canPostNotifications
        return if (shouldNotify) {
            ActivationAction.POST_CHARGING_NOTIFICATION
        } else {
            ActivationAction.NONE
        }
    }
}
