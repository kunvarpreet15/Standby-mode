package com.kunvarpreet.standbymode.core.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

/**
 * Immutable representation of the device's battery status.
 *
 * @param percentage The battery charge level in the range 0..100, or null if unavailable.
 * @param isCharging True if the device is currently charging or full while plugged in.
 * @param isAvailable True if valid battery percentage data could be retrieved.
 */
data class BatteryState(
    val percentage: Int? = null,
    val isCharging: Boolean = false,
    val isAvailable: Boolean = false
) {
    val formattedPercentage: String
        get() = if (percentage != null) "$percentage%" else ""
}

/**
 * Pure parser for interpreting system battery broadcasts.
 * Independent of Android UI to allow comprehensive, isolated unit testing.
 */
object BatteryStateParser {

    /**
     * Parses an Intent received from Intent.ACTION_BATTERY_CHANGED.
     */
    fun parse(intent: Intent?): BatteryState {
        if (intent == null) return BatteryState(isAvailable = false)

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        return parse(level = level, scale = scale, status = status, plugged = plugged)
    }

    /**
     * Overload for pure unit testing without android.content.Intent dependencies.
     *
     * @param level Raw level reported by BatteryManager.EXTRA_LEVEL.
     * @param scale Raw scale reported by BatteryManager.EXTRA_SCALE.
     * @param status Status reported by BatteryManager.EXTRA_STATUS.
     * @param plugged Plugged state reported by BatteryManager.EXTRA_PLUGGED (0 = unplugged).
     */
    fun parse(level: Int, scale: Int, status: Int, plugged: Int = 0): BatteryState {
        val percentage = if (level >= 0 && scale > 0) {
            val raw = ((level.toFloat() / scale.toFloat()) * 100f).roundToInt()
            raw.coerceIn(0, 100)
        } else {
            null
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                (status == BatteryManager.BATTERY_STATUS_FULL && plugged != 0)

        val isAvailable = percentage != null

        return BatteryState(
            percentage = percentage,
            isCharging = isCharging,
            isAvailable = isAvailable
        )
    }
}

/**
 * Lifecycle-aware battery state provider.
 *
 * Subscribes to Intent.ACTION_BATTERY_CHANGED only when [enabled], [isClockScreen],
 * and [isActivityForeground] are all true. Unregisters the receiver when any condition becomes false
 * or when the composable leaves the composition.
 */
@Composable
fun rememberBatteryState(
    enabled: Boolean,
    isClockScreen: Boolean,
    isActivityForeground: Boolean
): BatteryState {
    val context = LocalContext.current
    val shouldSubscribe = enabled && isClockScreen && isActivityForeground

    var batteryState by remember(enabled) {
        if (!enabled) {
            mutableStateOf(BatteryState(isAvailable = false))
        } else {
            val initialIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            mutableStateOf(BatteryStateParser.parse(initialIntent))
        }
    }

    DisposableEffect(shouldSubscribe, context) {
        if (!shouldSubscribe) {
            return@DisposableEffect onDispose {}
        }

        // Refresh latest sticky intent immediately upon entering active subscription
        val stickyIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        batteryState = BatteryStateParser.parse(stickyIntent)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                batteryState = BatteryStateParser.parse(intent)
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Safely ignore if already unregistered
            }
        }
    }

    return batteryState
}
