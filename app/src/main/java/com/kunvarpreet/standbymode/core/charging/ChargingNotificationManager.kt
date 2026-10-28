package com.kunvarpreet.standbymode.core.charging

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.kunvarpreet.standbymode.MainActivity
import com.kunvarpreet.standbymode.R
import com.kunvarpreet.standbymode.core.activation.AutoActivationPolicy

/**
 * Pure policy logic determining notification creation and cancellation.
 * Kept standalone and framework-independent for easy, robust unit testing.
 */
object ChargingNotificationPolicy {

    /**
     * Determines whether a charging notification should be shown.
     */
    fun shouldShowChargingNotification(
        isFeatureEnabled: Boolean,
        isCharging: Boolean,
        isAppInForeground: Boolean
    ): Boolean {
        return isFeatureEnabled && isCharging && !isAppInForeground
    }

    /**
     * Determines whether an active charging notification should be canceled.
     */
    fun shouldCancelChargingNotification(
        isFeatureEnabled: Boolean,
        isCharging: Boolean
    ): Boolean {
        return !isFeatureEnabled || !isCharging
    }
}

/**
 * Manages charging notification lifecycle, channels, and platform posting.
 */
object ChargingNotificationManager {

    const val CHANNEL_ID = "charging_notifications_quiet"
    const val LEGACY_CHANNEL_ID = "charging_notifications"
    const val NOTIFICATION_ID = 1001

    @Volatile
    var isAppInForeground: Boolean = false

    /**
     * Creates the dedicated non-disruptive notification channel for charging awareness.
     * Safe to call multiple times.
     *
     * Android does not allow changing the importance of an existing channel programmatically.
     * To migrate existing installations cleanly from the previous IMPORTANCE_DEFAULT channel,
     * we delete LEGACY_CHANNEL_ID and create CHANNEL_ID configured with IMPORTANCE_LOW.
     */
    fun ensureNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        try {
            val legacyChannel = manager.getNotificationChannel(LEGACY_CHANNEL_ID)
            if (legacyChannel != null) {
                manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            }
        } catch (_: Exception) {
            // Non-critical cleanup
        }

        val existingChannel = manager.getNotificationChannel(CHANNEL_ID)
        if (existingChannel == null) {
            val name = "Charging Notifications"
            val descriptionText = "Non-disruptive notifications to open StandBy Mode when device is connected to power"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if notification permission is granted and notifications are enabled globally.
     */
    fun canPostNotifications(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (manager == null) {
            android.util.Log.d("ChargingNotificationManager", "NotificationManager is null")
            return false
        }

        if (!manager.areNotificationsEnabled()) {
            android.util.Log.d("ChargingNotificationManager", "areNotificationsEnabled is false")
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("ChargingNotificationManager", "POST_NOTIFICATIONS permission not granted")
                return false
            }
        }

        val channel = manager.getNotificationChannel(CHANNEL_ID)
        if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
            android.util.Log.d("ChargingNotificationManager", "Channel importance is NONE")
            return false
        }

        return true
    }

    /**
     * Checks if the device is currently connected to power using sticky battery broadcast.
     */
    fun isDeviceCharging(context: Context): Boolean {
        val batteryStatus: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * Posts the StandBy Mode charging notification if permitted.
     */
    fun postChargingNotification(context: Context) {
        val allowed = canPostNotifications(context)
        android.util.Log.d("ChargingNotificationManager", "postChargingNotification called. allowed=$allowed")
        if (!allowed) {
            return
        }

        ensureNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AutoActivationPolicy.EXTRA_AUTO_ACTIVATED, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_standby_notification)
            .setContentTitle("StandBy Mode is ready")
            .setContentText("Tap to open your full-screen clock.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancels any active charging notification.
     */
    fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
    }
}
