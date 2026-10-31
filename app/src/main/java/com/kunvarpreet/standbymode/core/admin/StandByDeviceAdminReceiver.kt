package com.kunvarpreet.standbymode.core.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Dedicated DeviceAdminReceiver handling Device Administrator lifecycle events.
 * Strictly limited to the Force Lock policy required for DevicePolicyManager.lockNow().
 */
class StandByDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("DeviceAdminReceiver", "StandByMode Device Admin enabled by user.")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.i("DeviceAdminReceiver", "StandByMode Device Admin disabled by user.")
    }
}
