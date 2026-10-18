package com.kunvarpreet.standbymode.core.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Creates a cold, lifecycle-safe Flow that emits the current Instant.
 *
 * Synchronizes to second boundaries when [showSeconds] is true,
 * or minute boundaries when [showSeconds] is false.
 * Also listens for system broadcasts (time, timezone, and tick changes)
 * to immediately reflect external system clock changes.
 */
fun timeFlow(
    context: Context,
    showSeconds: Boolean = false
): Flow<Instant> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            trySend(Instant.now())
        }
    }

    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_TIME_TICK)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }

    context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)

    // Initial emission on collection
    trySend(Instant.now())

    // Precision boundary-aligned ticker coroutine
    val tickerJob = launch {
        while (isActive) {
            val nowMillis = System.currentTimeMillis()
            val delayMillis = if (showSeconds) {
                (1000L - (nowMillis % 1000L)).coerceAtLeast(10L)
            } else {
                val millisIntoMinute = nowMillis % 60000L
                (60000L - millisIntoMinute + 10L).coerceAtLeast(100L)
            }
            delay(delayMillis)
            trySend(Instant.now())
        }
    }

    awaitClose {
        tickerJob.cancel()
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered
        }
    }
}

/**
 * Lifecycle-aware Composable producing live [FormattedTime].
 * Automatically stops polling and unregisters receivers when UI is in background.
 */
@Composable
fun rememberLiveTime(
    showSeconds: Boolean = false,
    is24Hour: Boolean = false,
    zeroPad12Hour: Boolean = false
): State<FormattedTime> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    return produceState(
        initialValue = TimeFormatter.format(
            instant = Instant.now(),
            is24Hour = is24Hour,
            showSeconds = showSeconds,
            zeroPad12Hour = zeroPad12Hour
        ),
        showSeconds,
        is24Hour,
        zeroPad12Hour,
        lifecycleOwner
    ) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            timeFlow(context, showSeconds).collect { instant ->
                this@produceState.value = TimeFormatter.format(
                    instant = instant,
                    is24Hour = is24Hour,
                    showSeconds = showSeconds,
                    zeroPad12Hour = zeroPad12Hour
                )
            }
        }
    }
}
