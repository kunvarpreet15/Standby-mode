package com.kunvarpreet.standbymode.core.charging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kunvarpreet.standbymode.core.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manifest-declared BroadcastReceiver for charging events.
 *
 * Receives Intent.ACTION_POWER_CONNECTED and Intent.ACTION_POWER_DISCONNECTED.
 * Uses goAsync() to read DataStore preferences asynchronously without blocking the main thread.
 * Does not force-launch MainActivity or hold long-running wakelocks.
 */
class ChargingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("ChargingReceiver", "Received broadcast action: $action")

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = PreferencesRepository(context.applicationContext)
                        val preferences = repository.preferencesFlow.first()
                        val isForeground = ChargingNotificationManager.isAppInForeground
                        Log.d("ChargingReceiver", "showChargingNotification=${preferences.showChargingNotification}, isForeground=$isForeground")

                        if (ChargingNotificationPolicy.shouldShowChargingNotification(
                                isFeatureEnabled = preferences.showChargingNotification,
                                isCharging = true,
                                isAppInForeground = isForeground
                            )
                        ) {
                            Log.d("ChargingReceiver", "Posting charging notification")
                            ChargingNotificationManager.postChargingNotification(context.applicationContext)
                        } else {
                            Log.d("ChargingReceiver", "Policy rejected showing charging notification")
                        }
                    } catch (e: Exception) {
                        Log.e("ChargingReceiver", "Error in ChargingReceiver coroutine", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d("ChargingReceiver", "Cancelling notification on disconnect")
                ChargingNotificationManager.cancelNotification(context.applicationContext)
            }
        }
    }
}
