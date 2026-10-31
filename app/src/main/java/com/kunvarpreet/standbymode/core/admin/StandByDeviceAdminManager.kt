package com.kunvarpreet.standbymode.core.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kunvarpreet.standbymode.R

/**
 * System integration manager for Android Device Policy Manager operations.
 * Enforces least-privilege Force Lock requests without root or accessibility escalation.
 */
object StandByDeviceAdminManager {

    private const val TAG = "DeviceAdminManager"

    fun getAdminComponent(context: Context): ComponentName {
        return ComponentName(context, StandByDeviceAdminReceiver::class.java)
    }

    /**
     * Checks if StandByMode is currently an active Device Administrator.
     */
    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return try {
            dpm.isAdminActive(getAdminComponent(context))
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Device Admin active state", e)
            false
        }
    }

    /**
     * Creates an Intent to launch Android's official Device Admin activation UI.
     */
    fun createAddDeviceAdminIntent(context: Context): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponent(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.device_admin_explanation)
            )
        }
    }

    /**
     * Requests immediate device lock via DevicePolicyManager.lockNow().
     * Safe to call; handles SecurityException if permission was revoked.
     */
    fun lockDeviceNow(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        val component = getAdminComponent(context)

        if (!dpm.isAdminActive(component)) {
            Log.w(TAG, "Cannot lock device: StandByMode is not an active Device Admin.")
            return false
        }

        return try {
            dpm.lockNow()
            Log.i(TAG, "Device lock successfully requested via lockNow().")
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException executing lockNow(). Admin permission may have been revoked.", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error executing lockNow()", e)
            false
        }
    }

    /**
     * Safely deactivates StandByMode as a Device Administrator upon user request.
     */
    fun deactivateDeviceAdmin(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        val component = getAdminComponent(context)

        if (!dpm.isAdminActive(component)) {
            return true
        }

        return try {
            dpm.removeActiveAdmin(component)
            Log.i(TAG, "Device Admin successfully deactivated.")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deactivate Device Admin", e)
            false
        }
    }
}
