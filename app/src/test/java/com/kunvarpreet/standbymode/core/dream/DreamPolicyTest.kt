package com.kunvarpreet.standbymode.core.dream

import com.kunvarpreet.standbymode.core.activation.LaunchSource
import com.kunvarpreet.standbymode.core.burnin.BurnInProtectionPolicy
import com.kunvarpreet.standbymode.core.preferences.AccentColorOption
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Unit tests verifying DreamPolicy:
 * - Intent resolution and fallback order
 * - Preference mapping and display state derivation
 * - Screen brightness and Night Mode dimming resolution
 * - Burn-in protection offset delegation
 * - DreamService lifecycle state machine and idempotency
 * - Isolation from auto-activation and manual activity launch sources
 */
class DreamPolicyTest {

    @Test
    fun dreamSettingsActions_haveCorrectValues() {
        assertEquals("android.settings.DREAM_SETTINGS", DreamPolicy.ACTION_DREAM_SETTINGS)
        assertEquals("android.settings.DISPLAY_SETTINGS", DreamPolicy.ACTION_DISPLAY_SETTINGS)
        assertEquals("android.settings.SETTINGS", DreamPolicy.ACTION_SETTINGS)
        assertEquals("extra_target_screen", DreamPolicy.EXTRA_TARGET_SCREEN)
        assertEquals("screen_settings", DreamPolicy.SCREEN_SETTINGS)
    }

    @Test
    fun fallbackSettingsActions_prioritizesDisplaySettingsThenGeneralSettings() {
        val fallbacks = DreamPolicy.FALLBACK_SETTINGS_ACTIONS
        assertEquals(2, fallbacks.size)
        assertEquals(DreamPolicy.ACTION_DISPLAY_SETTINGS, fallbacks[0])
        assertEquals(DreamPolicy.ACTION_SETTINGS, fallbacks[1])
    }

    @Test
    fun screenBrightnessAndDimming_respectsNightMode() {
        // Normal mode: do not dim, screen bright is true
        assertFalse(DreamPolicy.shouldDimScreen(isNightMode = false))
        assertTrue(DreamPolicy.resolveScreenBrightness(isNightMode = false))

        // Night mode: dim screen, screen bright is false
        assertTrue(DreamPolicy.shouldDimScreen(isNightMode = true))
        assertFalse(DreamPolicy.resolveScreenBrightness(isNightMode = true))
    }

    @Test
    fun calculatePixelShift_matchesBurnInProtectionPolicy() {
        val instant = Instant.ofEpochSecond(1700000000L)

        // When disabled, offset is zero
        val disabledOffset = DreamPolicy.calculatePixelShift(instant, isEnabled = false)
        assertEquals(0f, disabledOffset.xDp, 0.001f)
        assertEquals(0f, disabledOffset.yDp, 0.001f)

        // When enabled, offset delegates to BurnInProtectionPolicy
        val enabledOffset = DreamPolicy.calculatePixelShift(instant, isEnabled = true)
        val expectedOffset = BurnInProtectionPolicy.calculateOffset(instant, isEnabled = true)
        assertEquals(expectedOffset.xDp, enabledOffset.xDp, 0.001f)
        assertEquals(expectedOffset.yDp, enabledOffset.yDp, 0.001f)
    }

    @Test
    fun mapPreferencesToDreamState_retainsUserPreferences() {
        val prefs = UserClockPreferences(
            clockFace = ClockFaceType.ANALOG,
            isNightModeEnabled = true,
            accentColor = AccentColorOption.ORANGE,
            showSeconds = true,
            showDate = true,
            showBatteryStatus = true,
            is24Hour = true,
            isBurnInProtectionEnabled = true
        )

        val dreamState = DreamPolicy.mapPreferencesToDreamState(prefs)
        assertEquals(ClockFaceType.ANALOG, dreamState.clockFace)
        assertTrue(dreamState.isNightMode)
        assertEquals(AccentColorOption.ORANGE, dreamState.accentColor)
        assertTrue(dreamState.showSeconds)
        assertTrue(dreamState.showDate)
        assertTrue(dreamState.showBatteryStatus)
        assertTrue(dreamState.is24Hour)
        assertTrue(dreamState.isBurnInProtectionEnabled)
    }

    @Test
    fun mapPreferencesToDreamState_suppressesSecondsForRetroFlipClock() {
        val prefs = UserClockPreferences(
            clockFace = ClockFaceType.RETRO_FLIP,
            showSeconds = true
        )

        val dreamState = DreamPolicy.mapPreferencesToDreamState(prefs)
        assertEquals(ClockFaceType.RETRO_FLIP, dreamState.clockFace)
        assertFalse(dreamState.showSeconds)
    }

    @Test
    fun lifecycleStateMachine_validatesLegalTransitions() {
        // Standard linear lifecycle
        assertTrue(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.UNINITIALIZED, DreamPolicy.DreamLifecycleState.CREATED))
        assertTrue(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.CREATED, DreamPolicy.DreamLifecycleState.ATTACHED))
        assertTrue(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.ATTACHED, DreamPolicy.DreamLifecycleState.DREAMING_STARTED))
        assertTrue(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.DREAMING_STARTED, DreamPolicy.DreamLifecycleState.DREAMING_STOPPED))
        assertTrue(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.DREAMING_STOPPED, DreamPolicy.DreamLifecycleState.DETACHED))
        assertTrue(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.DETACHED, DreamPolicy.DreamLifecycleState.DESTROYED))

        // Re-starting while attached or stopped
        assertTrue(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.DREAMING_STOPPED, DreamPolicy.DreamLifecycleState.DREAMING_STARTED))
        assertTrue(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.DETACHED, DreamPolicy.DreamLifecycleState.ATTACHED))

        // Idempotent duplicate notifications
        assertTrue(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.DREAMING_STARTED, DreamPolicy.DreamLifecycleState.DREAMING_STARTED))
        assertTrue(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.DREAMING_STOPPED, DreamPolicy.DreamLifecycleState.DREAMING_STOPPED))
    }

    @Test
    fun lifecycleStateMachine_rejectsIllegalTransitions() {
        // Cannot jump directly from UNINITIALIZED to DREAMING_STARTED
        assertFalse(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.UNINITIALIZED, DreamPolicy.DreamLifecycleState.DREAMING_STARTED))

        // Once destroyed, no further transitions allowed
        assertFalse(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.DESTROYED, DreamPolicy.DreamLifecycleState.CREATED))
        assertFalse(DreamPolicy.canTransition(DreamPolicy.DreamLifecycleState.DESTROYED, DreamPolicy.DreamLifecycleState.ATTACHED))
    }

    @Test
    fun dreamIsolation_isIndependentFromAutoActivationSession() {
        assertTrue(DreamPolicy.isIsolatedFromAutoActivation(LaunchSource.AUTO_ACTIVATED))
        assertTrue(DreamPolicy.isIsolatedFromAutoActivation(LaunchSource.MANUAL))
    }

    @Test
    fun defaultWindowConfiguration_isNonInteractiveAndFullscreen() {
        assertFalse(DreamPolicy.DEFAULT_IS_INTERACTIVE)
        assertTrue(DreamPolicy.DEFAULT_IS_FULLSCREEN)
    }
}
