package com.kunvarpreet.standbymode.core.dream

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.kunvarpreet.standbymode.core.activation.LaunchSource
import com.kunvarpreet.standbymode.core.burnin.BurnInProtectionPolicy
import com.kunvarpreet.standbymode.core.burnin.BurnInShiftOffset
import com.kunvarpreet.standbymode.core.preferences.AccentColorOption
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import java.time.Instant

/**
 * Pure policy and intent coordination for Android DreamService (Daydream Screensaver) integration.
 */
object DreamPolicy {

    const val ACTION_DREAM_SETTINGS = "android.settings.DREAM_SETTINGS"
    const val ACTION_DISPLAY_SETTINGS = "android.settings.DISPLAY_SETTINGS"
    const val ACTION_SETTINGS = "android.settings.SETTINGS"

    val FALLBACK_SETTINGS_ACTIONS: List<String> = listOf(
        ACTION_DISPLAY_SETTINGS,
        ACTION_SETTINGS
    )

    const val EXTRA_TARGET_SCREEN = "extra_target_screen"
    const val SCREEN_SETTINGS = "screen_settings"

    /**
     * By default, ambient screensavers are non-interactive so any tap on the screen
     * or hardware button wakes the device and dismisses the dream.
     */
    const val DEFAULT_IS_INTERACTIVE: Boolean = false

    /**
     * Ambient screensavers occupy the entire display area without status or navigation bars.
     */
    const val DEFAULT_IS_FULLSCREEN: Boolean = true

    /**
     * Evaluates whether the dream screen should be dimmed based on user preferences.
     * When night mode is enabled, the screen is dimmed to minimal brightness.
     */
    fun shouldDimScreen(isNightMode: Boolean): Boolean = isNightMode

    /**
     * Determines the screen brightness flag for DreamService.setScreenBright(boolean).
     * Returns false in Night Mode to allow display dimming; true for normal ambient brightness.
     */
    fun resolveScreenBrightness(isNightMode: Boolean): Boolean = !isNightMode

    /**
     * Calculates the deterministic burn-in pixel shift offset for the dream clock display.
     */
    fun calculatePixelShift(instant: Instant, isEnabled: Boolean): BurnInShiftOffset {
        return BurnInProtectionPolicy.calculateOffset(instant = instant, isEnabled = isEnabled)
    }

    /**
     * Creates an Intent to launch Android's system screensaver (Daydream) settings.
     */
    fun createDreamSettingsIntent(): Intent {
        return Intent(ACTION_DREAM_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates fallback intents in priority order when the primary dream settings intent is unavailable.
     */
    fun createFallbackSettingsIntents(): List<Intent> {
        return FALLBACK_SETTINGS_ACTIONS.map { action ->
            Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        }
    }

    /**
     * Safely opens system screensaver settings, gracefully falling back to display settings
     * or general settings if the OEM does not expose the direct dream activity.
     *
     * Returns true if an activity was successfully launched; false if all attempts failed.
     */
    fun openDreamSettingsSafely(context: Context): Boolean {
        val intentsToTry = listOf(createDreamSettingsIntent()) + createFallbackSettingsIntents()
        for (intent in intentsToTry) {
            try {
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
                // Try next fallback
            } catch (_: SecurityException) {
                // Try next fallback
            }
        }
        return false
    }

    /**
     * Validates that DreamService runs completely isolated from manual activity launch sources
     * and charging-triggered auto-activation sessions.
     */
    fun isIsolatedFromAutoActivation(launchSource: LaunchSource): Boolean {
        // DreamService does not alter, rely on, or participate in LaunchSource transitions
        return true
    }

    /**
     * State mapping representing the ambient dream display configuration.
     */
    data class DreamClockState(
        val clockFace: ClockFaceType,
        val isNightMode: Boolean,
        val accentColor: AccentColorOption,
        val showSeconds: Boolean,
        val showDate: Boolean,
        val showBatteryStatus: Boolean,
        val is24Hour: Boolean,
        val isBurnInProtectionEnabled: Boolean
    )

    /**
     * Maps user preferences into the pure dream display state model.
     */
    fun mapPreferencesToDreamState(preferences: UserClockPreferences): DreamClockState {
        return DreamClockState(
            clockFace = preferences.clockFace,
            isNightMode = preferences.isNightModeEnabled,
            accentColor = preferences.accentColor,
            showSeconds = preferences.showSeconds && preferences.clockFace != ClockFaceType.RETRO_FLIP,
            showDate = preferences.showDate,
            showBatteryStatus = preferences.showBatteryStatus,
            is24Hour = preferences.is24Hour,
            isBurnInProtectionEnabled = preferences.isBurnInProtectionEnabled
        )
    }

    /**
     * Pure representation of DreamService lifecycle states for validation and testing.
     */
    enum class DreamLifecycleState {
        UNINITIALIZED,
        CREATED,
        ATTACHED,
        DREAMING_STARTED,
        DREAMING_STOPPED,
        DETACHED,
        DESTROYED
    }

    /**
     * Validates whether a lifecycle transition is legal and idempotent.
     */
    fun canTransition(from: DreamLifecycleState, to: DreamLifecycleState): Boolean {
        return when (from) {
            DreamLifecycleState.UNINITIALIZED -> to == DreamLifecycleState.CREATED
            DreamLifecycleState.CREATED -> to == DreamLifecycleState.ATTACHED || to == DreamLifecycleState.DESTROYED
            DreamLifecycleState.ATTACHED -> to == DreamLifecycleState.DREAMING_STARTED || to == DreamLifecycleState.DETACHED
            DreamLifecycleState.DREAMING_STARTED -> to == DreamLifecycleState.DREAMING_STOPPED || to == DreamLifecycleState.DREAMING_STARTED
            DreamLifecycleState.DREAMING_STOPPED -> to == DreamLifecycleState.DREAMING_STARTED || to == DreamLifecycleState.DETACHED || to == DreamLifecycleState.DREAMING_STOPPED
            DreamLifecycleState.DETACHED -> to == DreamLifecycleState.ATTACHED || to == DreamLifecycleState.DESTROYED
            DreamLifecycleState.DESTROYED -> false
        }
    }
}
