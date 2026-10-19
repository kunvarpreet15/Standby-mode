package com.kunvarpreet.standbymode.core.charging

import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingNotificationLogicTest {

    @Test
    fun defaultPreferences_chargingNotificationIsDisabledByDefault() {
        val prefs = UserClockPreferences()
        assertFalse("Charging notification must be disabled by default", prefs.showChargingNotification)
    }

    @Test
    fun featureDisabled_powerConnected_doesNotRequestNotification() {
        val shouldShow = ChargingNotificationPolicy.shouldShowChargingNotification(
            isFeatureEnabled = false,
            isCharging = true,
            isAppInForeground = false
        )
        assertFalse("Disabled feature must not request notification", shouldShow)
    }

    @Test
    fun featureEnabled_powerConnected_inBackground_requestsNotification() {
        val shouldShow = ChargingNotificationPolicy.shouldShowChargingNotification(
            isFeatureEnabled = true,
            isCharging = true,
            isAppInForeground = false
        )
        assertTrue("Enabled feature while in background must request notification", shouldShow)
    }

    @Test
    fun featureEnabled_powerConnected_inForeground_doesNotDisruptUser() {
        val shouldShow = ChargingNotificationPolicy.shouldShowChargingNotification(
            isFeatureEnabled = true,
            isCharging = true,
            isAppInForeground = true
        )
        assertFalse("Should not post notification when app is already in foreground", shouldShow)
    }

    @Test
    fun featureEnabled_notCharging_doesNotRequestNotification() {
        val shouldShow = ChargingNotificationPolicy.shouldShowChargingNotification(
            isFeatureEnabled = true,
            isCharging = false,
            isAppInForeground = false
        )
        assertFalse("Should not post notification when device is not charging", shouldShow)
    }

    @Test
    fun powerDisconnected_cancelsNotification() {
        val shouldCancel = ChargingNotificationPolicy.shouldCancelChargingNotification(
            isFeatureEnabled = true,
            isCharging = false
        )
        assertTrue("Power disconnected must trigger cancellation", shouldCancel)
    }

    @Test
    fun featureDisabled_cancelsNotificationEvenIfCharging() {
        val shouldCancel = ChargingNotificationPolicy.shouldCancelChargingNotification(
            isFeatureEnabled = false,
            isCharging = true
        )
        assertTrue("Disabling feature must cancel active notification", shouldCancel)
    }

    @Test
    fun featureEnabled_stillCharging_doesNotCancelNotification() {
        val shouldCancel = ChargingNotificationPolicy.shouldCancelChargingNotification(
            isFeatureEnabled = true,
            isCharging = true
        )
        assertFalse("Active charging while feature enabled must retain notification", shouldCancel)
    }

    @Test
    fun notificationConstants_preventDuplicateNotifications() {
        // Notification ID is fixed to ensure successive charging events update/replace rather than stack
        assertEquals(1001, ChargingNotificationManager.NOTIFICATION_ID)
        assertEquals("charging_notifications", ChargingNotificationManager.CHANNEL_ID)
    }
}
