package com.kunvarpreet.standbymode.ui.duo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.core.battery.BatteryState
import com.kunvarpreet.standbymode.core.media.MediaWidgetUiState
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.ClockThemePalette
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.core.time.FormattedTime
import com.kunvarpreet.standbymode.ui.clock.AnalogClockDialView
import com.kunvarpreet.standbymode.ui.clock.BatteryStatusPill
import com.kunvarpreet.standbymode.ui.clock.BoldDigitalClockView
import com.kunvarpreet.standbymode.ui.clock.DatePill
import com.kunvarpreet.standbymode.ui.clock.RetroFlipClockView
import com.kunvarpreet.standbymode.ui.media.MediaMiniWidget
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * Duo Layout Screen displaying two equally weighted panel areas:
 * - Left Panel: Active Clock face (Bold Digital, Analog, Retro Flip) with Date and Battery pills.
 * - Right Panel: Secondary widget content (MediaMiniWidget when enabled, or quiet empty state with Settings entry).
 *
 * Automatically adapts between side-by-side landscape layout and stacked vertical portrait fallback.
 */
@Composable
fun DuoLayoutScreen(
    preferences: UserClockPreferences,
    formattedTime: FormattedTime,
    batteryState: BatteryState,
    mediaWidgetUiState: MediaWidgetUiState,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaSeek: (Long) -> Unit,
    onOpenMediaPermissionSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ClockThemePalette.resolve(
        isNightMode = preferences.isNightModeEnabled,
        selectedAccent = preferences.accentColor
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Duo Layout with clock and secondary content panels"
            }
    ) {
        val isLandscape = maxWidth > maxHeight
        val dividerColor = if (preferences.isNightModeEnabled) {
            StandByNightSurface
        } else {
            StandByBorder.copy(alpha = 0.5f)
        }

        if (isLandscape) {
            // Landscape Mode: Side-by-side 50/50 dual pane layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Panel: Clock Visual Anchor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    DuoClockPane(
                        preferences = preferences,
                        formattedTime = formattedTime,
                        batteryState = batteryState,
                        palette = palette,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Restrained Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.65f)
                        .background(dividerColor)
                )

                // Right Panel: Secondary Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    DuoSecondaryPane(
                        preferences = preferences,
                        palette = palette,
                        mediaWidgetUiState = mediaWidgetUiState,
                        onMediaPlayPause = onMediaPlayPause,
                        onMediaPrevious = onMediaPrevious,
                        onMediaNext = onMediaNext,
                        onMediaSeek = onMediaSeek,
                        onOpenMediaPermissionSettings = onOpenMediaPermissionSettings,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Portrait Mode: Readable stacked fallback
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Panel: Clock Visual Anchor
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DuoClockPane(
                        preferences = preferences,
                        formattedTime = formattedTime,
                        batteryState = batteryState,
                        palette = palette,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Restrained Divider
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth(0.65f)
                        .background(dividerColor)
                )

                // Bottom Panel: Secondary Content
                Box(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DuoSecondaryPane(
                        preferences = preferences,
                        palette = palette,
                        mediaWidgetUiState = mediaWidgetUiState,
                        onMediaPlayPause = onMediaPlayPause,
                        onMediaPrevious = onMediaPrevious,
                        onMediaNext = onMediaNext,
                        onMediaSeek = onMediaSeek,
                        onOpenMediaPermissionSettings = onOpenMediaPermissionSettings,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Left/Top panel of the Duo Layout: displays the active persistent clock face
 * scaled appropriately for panel bounds, along with optional Date and Battery status pills.
 */
@Composable
fun DuoClockPane(
    preferences: UserClockPreferences,
    formattedTime: FormattedTime,
    batteryState: BatteryState,
    palette: ClockThemePalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .semantics {
                contentDescription = "Clock panel showing ${preferences.clockFace.displayName}"
            }
    ) {
        // Top-Start: Date Capsule and Battery Status Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 8.dp),
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

        // Center: Active Clock Face scaled to panel bounds
        val hasTopPills = (preferences.showDate && formattedTime.formattedDate.isNotBlank()) ||
                (preferences.showBatteryStatus && batteryState.isAvailable)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (hasTopPills) 28.dp else 0.dp),
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

/**
 * Right/Bottom panel of the Duo Layout: displays the secondary widget content.
 * When media playback is enabled: displays the MediaMiniWidget (active, paused, no media, or permission prompt).
 * When media playback is disabled: displays a quiet empty state with a Settings entry point.
 */
@Composable
fun DuoSecondaryPane(
    preferences: UserClockPreferences,
    palette: ClockThemePalette,
    mediaWidgetUiState: MediaWidgetUiState,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaSeek: (Long) -> Unit,
    onOpenMediaPermissionSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .semantics {
                contentDescription = if (preferences.showMediaPlayer) {
                    "Media widget panel"
                } else {
                    "Secondary panel empty state"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (preferences.showMediaPlayer) {
            MediaMiniWidget(
                uiState = mediaWidgetUiState,
                isNightMode = preferences.isNightModeEnabled,
                accentColor = palette.accentColor,
                onPlayPause = onMediaPlayPause,
                onPrevious = onMediaPrevious,
                onNext = onMediaNext,
                onSeek = onMediaSeek,
                onOpenPermissionSettings = onOpenMediaPermissionSettings,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            DuoDisabledPanelCard(
                isNightMode = preferences.isNightModeEnabled,
                accentColor = palette.accentColor,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

/**
 * Quiet, restrained secondary panel empty state shown when panel content (media) is disabled in Settings.
 */
@Composable
fun DuoDisabledPanelCard(
    isNightMode: Boolean,
    accentColor: Color,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = if (isNightMode) StandByNightSurface else StandBySurfaceCard
    val borderColor = if (isNightMode) StandByNightSurface else StandByBorder
    val primaryTextColor = if (isNightMode) StandByNightText else StandByTextPrimary
    val mutedTextColor = if (isNightMode) StandByNightTextMuted else StandByTextMuted
    val actionColor = if (isNightMode) StandByNightText else accentColor

    Surface(
        modifier = modifier
            .widthIn(min = 260.dp, max = 380.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SECONDARY PANEL",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = mutedTextColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No Widget Enabled",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryTextColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Turn on the Media Player in Settings to display playback controls here.",
                fontSize = 13.sp,
                color = mutedTextColor,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            TextButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Text(
                    text = "Open Settings",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = actionColor
                )
            }
        }
    }
}
