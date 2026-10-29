package com.kunvarpreet.standbymode.ui.dream

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kunvarpreet.standbymode.core.battery.rememberBatteryState
import com.kunvarpreet.standbymode.core.burnin.BurnInProtectionPolicy
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.ClockThemePalette
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.core.time.rememberLiveTime
import com.kunvarpreet.standbymode.ui.clock.AnalogClockDialView
import com.kunvarpreet.standbymode.ui.clock.BatteryStatusPill
import com.kunvarpreet.standbymode.ui.clock.BoldDigitalClockView
import com.kunvarpreet.standbymode.ui.clock.DatePill
import com.kunvarpreet.standbymode.ui.clock.RetroFlipClockView
import com.kunvarpreet.standbymode.ui.theme.StandByBackground

/**
 * Dedicated ambient display composable for StandByDreamService.
 * Reuses core clock faces, date/battery pills, theme palette, and burn-in protection
 * within an uncluttered, distraction-free screensaver presentation.
 */
@Composable
fun DreamClockContent(
    preferences: UserClockPreferences,
    isDreaming: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = ClockThemePalette.resolve(
        isNightMode = preferences.isNightModeEnabled,
        selectedAccent = preferences.accentColor
    )

    val formattedTime by rememberLiveTime(
        showSeconds = preferences.showSeconds && preferences.clockFace != ClockFaceType.RETRO_FLIP,
        is24Hour = preferences.is24Hour
    )

    val batteryState = rememberBatteryState(
        enabled = preferences.showBatteryStatus,
        isClockScreen = true,
        isActivityForeground = isDreaming
    )

    // Calculate deterministic pixel shift offset
    val bucketIndex = formattedTime.timestamp.epochSecond / (BurnInProtectionPolicy.INTERVAL_MINUTES * 60L)
    val targetOffset = remember(bucketIndex, preferences.isBurnInProtectionEnabled) {
        BurnInProtectionPolicy.calculateOffset(
            instant = formattedTime.timestamp,
            isEnabled = preferences.isBurnInProtectionEnabled
        )
    }

    val animatedX by animateDpAsState(
        targetValue = targetOffset.xDp.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "dream_burn_in_pixel_shift_x"
    )
    val animatedY by animateDpAsState(
        targetValue = targetOffset.yDp.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "dream_burn_in_pixel_shift_y"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StandByBackground)
            .safeDrawingPadding()
            .semantics {
                contentDescription = "StandBy system screensaver dream display"
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = animatedX.roundToPx(),
                        y = animatedY.roundToPx()
                    )
                }
        ) {
            // Top-Left: Date Capsule and Battery Status Indicator
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (preferences.showDate && formattedTime.formattedDate.isNotBlank()) {
                    DatePill(
                        dateText = formattedTime.formattedDate,
                        isNightMode = preferences.isNightModeEnabled
                    )
                }

                if (preferences.showBatteryStatus && batteryState.isAvailable) {
                    BatteryStatusPill(
                        batteryState = batteryState,
                        accentColor = palette.accentColor,
                        isNightMode = preferences.isNightModeEnabled
                    )
                }
            }

            // Center: Active Clock Face
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (preferences.clockFace) {
                    ClockFaceType.BOLD_DIGITAL -> {
                        BoldDigitalClockView(
                            hours = formattedTime.hours,
                            minutes = formattedTime.minutes,
                            seconds = if (preferences.showSeconds) formattedTime.seconds else null,
                            amPm = formattedTime.amPm,
                            accentColor = palette.accentColor,
                            isNightMode = preferences.isNightModeEnabled,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ClockFaceType.ANALOG -> {
                        val hourInt = formattedTime.hours.toIntOrNull() ?: 0
                        val minInt = formattedTime.minutes.toIntOrNull() ?: 0
                        val secInt = formattedTime.seconds?.toIntOrNull()

                        AnalogClockDialView(
                            hours = hourInt,
                            minutes = minInt,
                            seconds = secInt,
                            showSeconds = preferences.showSeconds,
                            accentColor = palette.accentColor,
                            isNightMode = preferences.isNightModeEnabled,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ClockFaceType.RETRO_FLIP -> {
                        RetroFlipClockView(
                            hours = formattedTime.hours,
                            minutes = formattedTime.minutes,
                            isNightMode = preferences.isNightModeEnabled,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
