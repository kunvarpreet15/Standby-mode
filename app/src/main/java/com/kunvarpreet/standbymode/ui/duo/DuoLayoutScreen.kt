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
import com.kunvarpreet.standbymode.core.calendar.CalendarAgendaUiState
import com.kunvarpreet.standbymode.core.media.MediaWidgetUiState
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.ClockThemePalette
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.core.time.FormattedTime
import com.kunvarpreet.standbymode.core.weather.WeatherUiState
import com.kunvarpreet.standbymode.core.widget.DuoWidgetPolicy
import com.kunvarpreet.standbymode.core.widget.DuoWidgetType
import com.kunvarpreet.standbymode.ui.battery.BatteryWidgetView
import com.kunvarpreet.standbymode.ui.calendar.CalendarMiniWidget
import com.kunvarpreet.standbymode.ui.clock.AnalogClockDialView
import com.kunvarpreet.standbymode.ui.clock.BatteryStatusPill
import com.kunvarpreet.standbymode.ui.clock.BoldDigitalClockView
import com.kunvarpreet.standbymode.ui.clock.DatePill
import com.kunvarpreet.standbymode.ui.clock.RetroFlipClockView
import com.kunvarpreet.standbymode.ui.media.MediaMiniWidget
import com.kunvarpreet.standbymode.ui.photoframe.PhotoFrameWidget
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary
import com.kunvarpreet.standbymode.ui.weather.WeatherMiniWidget

/**
 * Duo Layout Screen displaying two independently navigable vertical widget stacks:
 * - Left Pane Stack: Supports Clock, Calendar, Weather, Battery, and Media widgets (default: Clock).
 * - Right Pane Stack: Supports Media, Calendar, Weather, Battery, and Clock widgets (default: Media if enabled, else Battery).
 *
 * Automatically adapts between side-by-side landscape layout and stacked vertical portrait fallback.
 */
@Composable
fun DuoLayoutScreen(
    modifier: Modifier = Modifier,
    preferences: UserClockPreferences,
    formattedTime: FormattedTime,
    batteryState: BatteryState,
    mediaWidgetUiState: MediaWidgetUiState,
    calendarUiState: CalendarAgendaUiState = CalendarAgendaUiState.Loading,
    weatherUiState: WeatherUiState = WeatherUiState.Loading,
    leftPaneWidget: DuoWidgetType = DuoWidgetType.CLOCK,
    rightPaneWidget: DuoWidgetType = if (preferences.showMediaPlayer) DuoWidgetType.MEDIA else DuoWidgetType.BATTERY_STATUS,
    onLeftPaneWidgetChanged: (DuoWidgetType) -> Unit = {},
    onRightPaneWidgetChanged: (DuoWidgetType) -> Unit = {},
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaSeek: (Long) -> Unit,
    onOpenMediaPermissionSettings: () -> Unit,
    onOpenCalendarPermissionSettings: () -> Unit = {},
    onCalendarRetry: () -> Unit = {},
    onOpenWeatherPermissionSettings: () -> Unit = {},
    onWeatherRetry: () -> Unit = {},
    onLaunchPhotoPicker: () -> Unit = {},
    onOpenSettings: () -> Unit
) {
    val palette = ClockThemePalette.resolve(
        isNightMode = preferences.isNightModeEnabled,
        selectedAccent = preferences.accentColor
    )

    val availableLeftWidgets = DuoWidgetPolicy.getAvailableWidgets(
        isLeftPane = true,
        showMediaPlayer = preferences.showMediaPlayer,
        showCalendarAgenda = preferences.showCalendarAgenda,
        showWeatherWidget = preferences.showWeatherWidget,
        showPhotoFrame = preferences.showPhotoFrame
    )
    val availableRightWidgets = DuoWidgetPolicy.getAvailableWidgets(
        isLeftPane = false,
        showMediaPlayer = preferences.showMediaPlayer,
        showCalendarAgenda = preferences.showCalendarAgenda,
        showWeatherWidget = preferences.showWeatherWidget,
        showPhotoFrame = preferences.showPhotoFrame
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Duo Layout with independent vertical widget stacks"
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
                // Left Panel Widget Stack
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    DuoWidgetStackView(
                        availableWidgets = availableLeftWidgets,
                        selectedWidget = leftPaneWidget,
                        onWidgetSelected = onLeftPaneWidgetChanged,
                        paneLabel = "Left pane",
                        accentColor = palette.accentColor,
                        isNightMode = preferences.isNightModeEnabled,
                        indicatorAlignment = Alignment.CenterEnd,
                        modifier = Modifier.fillMaxSize()
                    ) { widgetType ->
                        DuoWidgetRenderer(
                            widgetType = widgetType,
                            preferences = preferences,
                            formattedTime = formattedTime,
                            batteryState = batteryState,
                            palette = palette,
                            mediaWidgetUiState = mediaWidgetUiState,
                            calendarUiState = calendarUiState,
                            weatherUiState = weatherUiState,
                            isSlideshowActive = leftPaneWidget == DuoWidgetType.PHOTO_FRAME && widgetType == DuoWidgetType.PHOTO_FRAME,
                            onMediaPlayPause = onMediaPlayPause,
                            onMediaPrevious = onMediaPrevious,
                            onMediaNext = onMediaNext,
                            onMediaSeek = onMediaSeek,
                            onOpenMediaPermissionSettings = onOpenMediaPermissionSettings,
                            onOpenCalendarPermissionSettings = onOpenCalendarPermissionSettings,
                            onCalendarRetry = onCalendarRetry,
                            onOpenWeatherPermissionSettings = onOpenWeatherPermissionSettings,
                            onWeatherRetry = onWeatherRetry,
                            onLaunchPhotoPicker = onLaunchPhotoPicker,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Restrained Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(0.65f)
                        .background(dividerColor)
                )

                // Right Panel Widget Stack
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    DuoWidgetStackView(
                        availableWidgets = availableRightWidgets,
                        selectedWidget = rightPaneWidget,
                        onWidgetSelected = onRightPaneWidgetChanged,
                        paneLabel = "Right pane",
                        accentColor = palette.accentColor,
                        isNightMode = preferences.isNightModeEnabled,
                        indicatorAlignment = Alignment.CenterEnd,
                        modifier = Modifier.fillMaxSize()
                    ) { widgetType ->
                        DuoWidgetRenderer(
                            widgetType = widgetType,
                            preferences = preferences,
                            formattedTime = formattedTime,
                            batteryState = batteryState,
                            palette = palette,
                            mediaWidgetUiState = mediaWidgetUiState,
                            calendarUiState = calendarUiState,
                            weatherUiState = weatherUiState,
                            isSlideshowActive = rightPaneWidget == DuoWidgetType.PHOTO_FRAME && widgetType == DuoWidgetType.PHOTO_FRAME,
                            onMediaPlayPause = onMediaPlayPause,
                            onMediaPrevious = onMediaPrevious,
                            onMediaNext = onMediaNext,
                            onMediaSeek = onMediaSeek,
                            onOpenMediaPermissionSettings = onOpenMediaPermissionSettings,
                            onOpenCalendarPermissionSettings = onOpenCalendarPermissionSettings,
                            onCalendarRetry = onCalendarRetry,
                            onOpenWeatherPermissionSettings = onOpenWeatherPermissionSettings,
                            onWeatherRetry = onWeatherRetry,
                            onLaunchPhotoPicker = onLaunchPhotoPicker,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        } else {
            // Portrait Mode: Readable stacked vertical fallback
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Panel Widget Stack
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DuoWidgetStackView(
                        availableWidgets = availableLeftWidgets,
                        selectedWidget = leftPaneWidget,
                        onWidgetSelected = onLeftPaneWidgetChanged,
                        paneLabel = "Top pane",
                        accentColor = palette.accentColor,
                        isNightMode = preferences.isNightModeEnabled,
                        indicatorAlignment = Alignment.CenterEnd,
                        modifier = Modifier.fillMaxSize()
                    ) { widgetType ->
                        DuoWidgetRenderer(
                            widgetType = widgetType,
                            preferences = preferences,
                            formattedTime = formattedTime,
                            batteryState = batteryState,
                            palette = palette,
                            mediaWidgetUiState = mediaWidgetUiState,
                            calendarUiState = calendarUiState,
                            weatherUiState = weatherUiState,
                            isSlideshowActive = leftPaneWidget == DuoWidgetType.PHOTO_FRAME && widgetType == DuoWidgetType.PHOTO_FRAME,
                            onMediaPlayPause = onMediaPlayPause,
                            onMediaPrevious = onMediaPrevious,
                            onMediaNext = onMediaNext,
                            onMediaSeek = onMediaSeek,
                            onOpenMediaPermissionSettings = onOpenMediaPermissionSettings,
                            onOpenCalendarPermissionSettings = onOpenCalendarPermissionSettings,
                            onCalendarRetry = onCalendarRetry,
                            onOpenWeatherPermissionSettings = onOpenWeatherPermissionSettings,
                            onWeatherRetry = onWeatherRetry,
                            onLaunchPhotoPicker = onLaunchPhotoPicker,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Restrained Divider
                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth(0.65f)
                        .background(dividerColor)
                )

                // Bottom Panel Widget Stack
                Box(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DuoWidgetStackView(
                        availableWidgets = availableRightWidgets,
                        selectedWidget = rightPaneWidget,
                        onWidgetSelected = onRightPaneWidgetChanged,
                        paneLabel = "Bottom pane",
                        accentColor = palette.accentColor,
                        isNightMode = preferences.isNightModeEnabled,
                        indicatorAlignment = Alignment.CenterEnd,
                        modifier = Modifier.fillMaxSize()
                    ) { widgetType ->
                        DuoWidgetRenderer(
                            widgetType = widgetType,
                            preferences = preferences,
                            formattedTime = formattedTime,
                            batteryState = batteryState,
                            palette = palette,
                            mediaWidgetUiState = mediaWidgetUiState,
                            calendarUiState = calendarUiState,
                            weatherUiState = weatherUiState,
                            isSlideshowActive = rightPaneWidget == DuoWidgetType.PHOTO_FRAME && widgetType == DuoWidgetType.PHOTO_FRAME,
                            onMediaPlayPause = onMediaPlayPause,
                            onMediaPrevious = onMediaPrevious,
                            onMediaNext = onMediaNext,
                            onMediaSeek = onMediaSeek,
                            onOpenMediaPermissionSettings = onOpenMediaPermissionSettings,
                            onOpenCalendarPermissionSettings = onOpenCalendarPermissionSettings,
                            onCalendarRetry = onCalendarRetry,
                            onOpenWeatherPermissionSettings = onOpenWeatherPermissionSettings,
                            onWeatherRetry = onWeatherRetry,
                            onLaunchPhotoPicker = onLaunchPhotoPicker,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable content renderer delegating to the corresponding built-in widget composable.
 */
@Composable
fun DuoWidgetRenderer(
    widgetType: DuoWidgetType,
    preferences: UserClockPreferences,
    formattedTime: FormattedTime,
    batteryState: BatteryState,
    palette: ClockThemePalette,
    mediaWidgetUiState: MediaWidgetUiState,
    calendarUiState: CalendarAgendaUiState = CalendarAgendaUiState.Loading,
    weatherUiState: WeatherUiState = WeatherUiState.Loading,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaSeek: (Long) -> Unit,
    onOpenMediaPermissionSettings: () -> Unit,
    onOpenCalendarPermissionSettings: () -> Unit = {},
    onCalendarRetry: () -> Unit = {},
    onOpenWeatherPermissionSettings: () -> Unit = {},
    onWeatherRetry: () -> Unit = {},
    isSlideshowActive: Boolean = false,
    onLaunchPhotoPicker: () -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (widgetType) {
        DuoWidgetType.CLOCK -> {
            DuoClockPane(
                preferences = preferences,
                formattedTime = formattedTime,
                batteryState = batteryState,
                palette = palette,
                modifier = modifier
            )
        }
        DuoWidgetType.BATTERY_STATUS -> {
            BatteryWidgetView(
                batteryState = batteryState,
                accentColor = palette.accentColor,
                isNightMode = preferences.isNightModeEnabled,
                modifier = modifier
            )
        }
        DuoWidgetType.MEDIA -> {
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
                modifier = modifier
            )
        }
        DuoWidgetType.CALENDAR -> {
            CalendarMiniWidget(
                uiState = calendarUiState,
                accentColor = palette.accentColor,
                isNightMode = preferences.isNightModeEnabled,
                is24Hour = preferences.is24Hour,
                onOpenPermissionSettings = onOpenCalendarPermissionSettings,
                onRetry = onCalendarRetry,
                modifier = modifier
            )
        }
        DuoWidgetType.WEATHER -> {
            WeatherMiniWidget(
                uiState = weatherUiState,
                accentColor = palette.accentColor,
                isNightMode = preferences.isNightModeEnabled,
                onOpenPermissionSettings = onOpenWeatherPermissionSettings,
                onRetry = onWeatherRetry,
                modifier = modifier
            )
        }
        DuoWidgetType.PHOTO_FRAME -> {
            PhotoFrameWidget(
                photoUris = preferences.photoFrameUris,
                isSlideshowActive = isSlideshowActive,
                isNightMode = preferences.isNightModeEnabled,
                accentColor = palette.accentColor,
                onChoosePhotos = onLaunchPhotoPicker,
                modifier = modifier
            )
        }
    }
}

/**
 * Clock panel for Duo Layout: displays the active persistent clock face
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
 * Media secondary panel: displays MediaMiniWidget when enabled or quiet empty state when disabled.
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
