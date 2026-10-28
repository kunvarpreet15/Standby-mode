package com.kunvarpreet.standbymode.ui.settings

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kunvarpreet.standbymode.core.charging.ChargingNotificationManager
import com.kunvarpreet.standbymode.core.media.MediaNotificationListenerService
import com.kunvarpreet.standbymode.core.media.MediaSessionPolicy
import com.kunvarpreet.standbymode.core.preferences.AccentColorOption
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.core.time.FormattedTime
import com.kunvarpreet.standbymode.ui.theme.StandByBackground
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * Screen D: SettingsScreen (With Live Clock Preview)
 * Complete responsive settings screen supporting landscape two-column split
 * and portrait stacked layouts. Immediately writes changes to DataStore.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: UserClockPreferences,
    formattedTime: FormattedTime,
    onClockFaceChanged: (ClockFaceType) -> Unit,
    on24HourChanged: (Boolean) -> Unit,
    onShowSecondsChanged: (Boolean) -> Unit,
    onShowDateChanged: (Boolean) -> Unit,
    onAccentColorChanged: (AccentColorOption) -> Unit,
    onNightModeChanged: (Boolean) -> Unit,
    onShowChargingNotificationChanged: (Boolean) -> Unit,
    onAutoActivateChanged: (Boolean) -> Unit = {},
    onBurnInProtectionChanged: (Boolean) -> Unit = {},
    onKeepScreenAwakeChanged: (Boolean) -> Unit = {},
    onShowBatteryStatusChanged: (Boolean) -> Unit = {},
    onShowMediaPlayerChanged: (Boolean) -> Unit = {},
    onShowCalendarAgendaChanged: (Boolean) -> Unit = {},
    onShowWeatherWidgetChanged: (Boolean) -> Unit = {},
    onTogglePhotoFrame: (Boolean) -> Unit = {},
    onLaunchPhotoPicker: () -> Unit = {},
    onRemoveAllPhotos: () -> Unit = {},
    onOpenFullscreenPhotoFrame: (() -> Unit)? = null,
    onRevisitOnboarding: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = preferences.accentColor.color

    var showPermissionExplanationDialog by remember { mutableStateOf(false) }
    var explanationDialogTitle by remember { mutableStateOf("") }
    var explanationDialogMessage by remember { mutableStateOf("") }

    var showCalendarPermissionExplanationDialog by remember { mutableStateOf(false) }
    var showCalendarPermissionDeniedFeedback by remember { mutableStateOf(false) }

    var showWeatherPermissionExplanationDialog by remember { mutableStateOf(false) }
    var showWeatherPermissionDeniedFeedback by remember { mutableStateOf(false) }

    var showRemoveAllPhotosConfirmationDialog by remember { mutableStateOf(false) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onShowCalendarAgendaChanged(true)
        } else {
            onShowCalendarAgendaChanged(false)
            showCalendarPermissionDeniedFeedback = true
        }
    }

    val onToggleCalendarAgenda: (Boolean) -> Unit = { shouldEnable ->
        if (shouldEnable) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                onShowCalendarAgendaChanged(true)
            } else {
                showCalendarPermissionExplanationDialog = true
            }
        } else {
            onShowCalendarAgendaChanged(false)
        }
    }

    val weatherPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onShowWeatherWidgetChanged(true)
        } else {
            onShowWeatherWidgetChanged(false)
            showWeatherPermissionDeniedFeedback = true
        }
    }

    val onToggleWeatherWidget: (Boolean) -> Unit = { shouldEnable ->
        if (shouldEnable) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                onShowWeatherWidgetChanged(true)
            } else {
                showWeatherPermissionExplanationDialog = true
            }
        } else {
            onShowWeatherWidgetChanged(false)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager?.areNotificationsEnabled() == false) {
                explanationDialogTitle = "Notifications Blocked"
                explanationDialogMessage = "Notifications are blocked in your device settings. Please enable notifications for StandBy Mode in Android Settings."
                showPermissionExplanationDialog = true
            } else {
                onShowChargingNotificationChanged(true)
                if (ChargingNotificationManager.isDeviceCharging(context)) {
                    ChargingNotificationManager.postChargingNotification(context)
                }
            }
        } else {
            explanationDialogTitle = "Notification Permission Required"
            explanationDialogMessage = "StandBy Mode requires notification permission to post a charging alert when your device is connected to power. Please grant notification permission to use this feature."
            showPermissionExplanationDialog = true
        }
    }

    val onToggleChargingNotification: (Boolean) -> Unit = { shouldEnable ->
        if (shouldEnable) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val systemAllowed = manager?.areNotificationsEnabled() ?: false

            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else if (!systemAllowed) {
                explanationDialogTitle = "Notifications Disabled"
                explanationDialogMessage = "Notifications are disabled in your system settings. Please enable notifications for StandBy Mode in Android Settings to use this feature."
                showPermissionExplanationDialog = true
            } else {
                onShowChargingNotificationChanged(true)
                if (ChargingNotificationManager.isDeviceCharging(context)) {
                    ChargingNotificationManager.postChargingNotification(context)
                }
            }
        } else {
            onShowChargingNotificationChanged(false)
            ChargingNotificationManager.cancelNotification(context)
        }
    }

    var showMediaPermissionExplanationDialog by remember { mutableStateOf(false) }
    var waitingForMediaPermissionReturn by remember { mutableStateOf(false) }
    var showMediaPermissionDeniedFeedback by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (waitingForMediaPermissionReturn) {
                    waitingForMediaPermissionReturn = false
                    if (MediaSessionPolicy.isNotificationListenerGranted(context)) {
                        onShowMediaPlayerChanged(true)
                    } else {
                        onShowMediaPlayerChanged(false)
                        showMediaPermissionDeniedFeedback = true
                    }
                } else if (preferences.showMediaPlayer) {
                    if (!MediaSessionPolicy.isNotificationListenerGranted(context)) {
                        onShowMediaPlayerChanged(false)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val onToggleMediaPlayer: (Boolean) -> Unit = { shouldEnable ->
        if (shouldEnable) {
            if (MediaSessionPolicy.isNotificationListenerGranted(context)) {
                onShowMediaPlayerChanged(true)
            } else {
                showMediaPermissionExplanationDialog = true
            }
        } else {
            onShowMediaPlayerChanged(false)
        }
    }

    if (showPermissionExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanationDialog = false },
            title = {
                Text(
                    text = explanationDialogTitle,
                    color = StandByTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Text(
                    text = explanationDialogMessage,
                    color = StandByTextMuted,
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionExplanationDialog = false }) {
                    Text(
                        text = "OK",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            containerColor = StandBySurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showMediaPermissionExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showMediaPermissionExplanationDialog = false },
            title = {
                Text(
                    text = "Media Access Required",
                    color = StandByTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Text(
                    text = "StandBy Mode displays track information and playback controls from compatible media apps on your bedside clock.\n\nTo discover active playback, Android requires Notification Listener access. This is a sensitive permission that must be granted in system Settings.\n\nPrivacy Notice: StandBy Mode uses this access strictly to read media session playback controls and metadata (track title, artist, album artwork). StandBy Mode does not inspect, read, or store any messages, notifications, or personal data.",
                    color = StandByTextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMediaPermissionExplanationDialog = false
                        waitingForMediaPermissionReturn = true
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            putExtra(
                                ":settings:fragment_args_key",
                                ComponentName(context, MediaNotificationListenerService::class.java).flattenToString()
                            )
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    }
                ) {
                    Text(
                        text = "Open Settings",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showMediaPermissionExplanationDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = StandByTextMuted,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            containerColor = StandBySurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showMediaPermissionDeniedFeedback) {
        AlertDialog(
            onDismissRequest = { showMediaPermissionDeniedFeedback = false },
            title = {
                Text(
                    text = "Media Access Not Granted",
                    color = StandByTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Text(
                    text = "Notification Listener access was not granted. The media player remains disabled until access is granted in Android Settings.",
                    color = StandByTextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                TextButton(onClick = { showMediaPermissionDeniedFeedback = false }) {
                    Text(
                        text = "OK",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            containerColor = StandBySurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showCalendarPermissionExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarPermissionExplanationDialog = false },
            title = {
                Text(
                    text = "Calendar Access Required",
                    color = StandByTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Text(
                    text = "StandBy Mode displays the current month and upcoming events from your calendar on the bedside display.\n\nPrivacy Notice: StandBy Mode only reads calendar information (dates and event titles/times) necessary to display your upcoming schedule. StandBy Mode never creates, edits, or deletes your events, and never collects attendee or location details.",
                    color = StandByTextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCalendarPermissionExplanationDialog = false
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    }
                ) {
                    Text(
                        text = "Grant Access",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarPermissionExplanationDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = StandByTextMuted,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            containerColor = StandBySurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showCalendarPermissionDeniedFeedback) {
        AlertDialog(
            onDismissRequest = { showCalendarPermissionDeniedFeedback = false },
            title = {
                Text(
                    text = "Calendar Access Not Granted",
                    color = StandByTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Text(
                    text = "Calendar permission was not granted. The Calendar & Agenda widget remains disabled until permission is granted.",
                    color = StandByTextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                TextButton(onClick = { showCalendarPermissionDeniedFeedback = false }) {
                    Text(
                        text = "OK",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            containerColor = StandBySurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showWeatherPermissionExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showWeatherPermissionExplanationDialog = false },
            title = {
                Text(
                    text = "Location Access Required",
                    color = StandByTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Text(
                    text = "The weather widget displays local weather and requires approximate location access and an internet connection.\n\nPrivacy Notice: StandBy Mode only accesses your approximate location while the app is actively in use to retrieve your local forecast. Precise or background location tracking is never requested or performed.",
                    color = StandByTextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWeatherPermissionExplanationDialog = false
                        weatherPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                ) {
                    Text(
                        text = "Continue",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeatherPermissionExplanationDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = StandByTextMuted,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            containerColor = StandBySurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showWeatherPermissionDeniedFeedback) {
        AlertDialog(
            onDismissRequest = { showWeatherPermissionDeniedFeedback = false },
            title = {
                Text(
                    text = "Location Access Not Granted",
                    color = StandByTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Text(
                    text = "Approximate location permission was not granted. The Weather widget remains disabled until permission is granted.",
                    color = StandByTextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                TextButton(onClick = { showWeatherPermissionDeniedFeedback = false }) {
                    Text(
                        text = "OK",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            containerColor = StandBySurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showRemoveAllPhotosConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveAllPhotosConfirmationDialog = false },
            title = {
                Text(
                    text = "Remove All Photos?",
                    color = StandByTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            },
            text = {
                Text(
                    text = "This will remove all selected photos from the Photo Frame widget and release associated permissions. You can select photos again at any time.",
                    color = StandByTextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveAllPhotosConfirmationDialog = false
                        onRemoveAllPhotos()
                    }
                ) {
                    Text(
                        text = "Remove",
                        color = androidx.compose.ui.graphics.Color(0xFFFF453A),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveAllPhotosConfirmationDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = StandByTextMuted,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            containerColor = StandBySurfaceCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StandByBackground)
            .safeDrawingPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Bar
            SettingsTopBar(
                onDismiss = onDismiss,
                accentColor = accentColor
            )

            HorizontalDivider(
                color = StandByBorder,
                thickness = 1.dp
            )

            // Responsive Layout Body
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                val isWideLayout = maxWidth >= 600.dp || maxWidth > maxHeight

                if (isWideLayout) {
                    // Wide / Landscape: Two-Column Split (Left: Preview, Right: Scrollable Settings)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Column: Live Clock Preview Card (approx 42% width)
                        Box(
                            modifier = Modifier
                                .weight(0.42f)
                                .padding(top = 12.dp)
                        ) {
                            ClockPreviewCard(
                                clockFace = preferences.clockFace,
                                formattedTime = formattedTime,
                                showSeconds = preferences.showSeconds && preferences.clockFace != ClockFaceType.RETRO_FLIP,
                                showDate = preferences.showDate,
                                accentColor = accentColor,
                                isNightMode = preferences.isNightModeEnabled
                            )
                        }

                        // Right Column: Vertically Scrollable Settings Controls
                        Column(
                            modifier = Modifier
                                .weight(0.58f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SettingsContent(
                                preferences = preferences,
                                accentColor = accentColor,
                                onClockFaceChanged = onClockFaceChanged,
                                on24HourChanged = on24HourChanged,
                                onShowSecondsChanged = onShowSecondsChanged,
                                onShowDateChanged = onShowDateChanged,
                                onAccentColorChanged = onAccentColorChanged,
                                onNightModeChanged = onNightModeChanged,
                                onToggleChargingNotification = onToggleChargingNotification,
                                onAutoActivateChanged = onAutoActivateChanged,
                                onBurnInProtectionChanged = onBurnInProtectionChanged,
                                onKeepScreenAwakeChanged = onKeepScreenAwakeChanged,
                                onShowBatteryStatusChanged = onShowBatteryStatusChanged,
                                onToggleMediaPlayer = onToggleMediaPlayer,
                                onToggleCalendarAgenda = onToggleCalendarAgenda,
                                onToggleWeatherWidget = onToggleWeatherWidget,
                                onTogglePhotoFrame = onTogglePhotoFrame,
                                onLaunchPhotoPicker = onLaunchPhotoPicker,
                                onRemoveAllPhotos = { showRemoveAllPhotosConfirmationDialog = true },
                                onOpenFullscreenPhotoFrame = onOpenFullscreenPhotoFrame,
                                onRevisitOnboarding = onRevisitOnboarding
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                } else {
                    // Narrow / Portrait: Single-Column Stacked Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Top Live Preview Card
                        ClockPreviewCard(
                            clockFace = preferences.clockFace,
                            formattedTime = formattedTime,
                            showSeconds = preferences.showSeconds && preferences.clockFace != ClockFaceType.RETRO_FLIP,
                            showDate = preferences.showDate,
                            accentColor = accentColor,
                            isNightMode = preferences.isNightModeEnabled
                        )

                        // Settings Sections
                        SettingsContent(
                            preferences = preferences,
                            accentColor = accentColor,
                            onClockFaceChanged = onClockFaceChanged,
                            on24HourChanged = on24HourChanged,
                            onShowSecondsChanged = onShowSecondsChanged,
                            onShowDateChanged = onShowDateChanged,
                            onAccentColorChanged = onAccentColorChanged,
                            onNightModeChanged = onNightModeChanged,
                            onToggleChargingNotification = onToggleChargingNotification,
                            onAutoActivateChanged = onAutoActivateChanged,
                            onBurnInProtectionChanged = onBurnInProtectionChanged,
                            onKeepScreenAwakeChanged = onKeepScreenAwakeChanged,
                            onShowBatteryStatusChanged = onShowBatteryStatusChanged,
                            onToggleMediaPlayer = onToggleMediaPlayer,
                            onToggleCalendarAgenda = onToggleCalendarAgenda,
                            onToggleWeatherWidget = onToggleWeatherWidget,
                            onTogglePhotoFrame = onTogglePhotoFrame,
                            onLaunchPhotoPicker = onLaunchPhotoPicker,
                            onRemoveAllPhotos = { showRemoveAllPhotosConfirmationDialog = true },
                            onOpenFullscreenPhotoFrame = onOpenFullscreenPhotoFrame,
                            onRevisitOnboarding = onRevisitOnboarding
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    onDismiss: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to clock",
                    tint = StandByTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Settings",
                color = StandByTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.padding(end = 4.dp)
        ) {
            Text(
                text = "Done",
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
private fun SettingsContent(
    preferences: UserClockPreferences,
    accentColor: androidx.compose.ui.graphics.Color,
    onClockFaceChanged: (ClockFaceType) -> Unit,
    on24HourChanged: (Boolean) -> Unit,
    onShowSecondsChanged: (Boolean) -> Unit,
    onShowDateChanged: (Boolean) -> Unit,
    onAccentColorChanged: (AccentColorOption) -> Unit,
    onNightModeChanged: (Boolean) -> Unit,
    onToggleChargingNotification: (Boolean) -> Unit,
    onAutoActivateChanged: (Boolean) -> Unit = {},
    onBurnInProtectionChanged: (Boolean) -> Unit,
    onKeepScreenAwakeChanged: (Boolean) -> Unit,
    onShowBatteryStatusChanged: (Boolean) -> Unit,
    onToggleMediaPlayer: (Boolean) -> Unit,
    onToggleCalendarAgenda: (Boolean) -> Unit,
    onToggleWeatherWidget: (Boolean) -> Unit = {},
    onTogglePhotoFrame: (Boolean) -> Unit = {},
    onLaunchPhotoPicker: () -> Unit = {},
    onRemoveAllPhotos: () -> Unit = {},
    onOpenFullscreenPhotoFrame: (() -> Unit)? = null,
    onRevisitOnboarding: () -> Unit = {}
) {
    // 1. Clock Face Selection
    SettingsSection(title = "Clock Face") {
        ClockFaceSelector(
            selectedFace = preferences.clockFace,
            accentColor = accentColor,
            onSelectFace = onClockFaceChanged
        )
    }

    // 2. Accent Color Swatches
    SettingsSection(title = "Accent Color") {
        AccentColorPicker(
            selectedColor = preferences.accentColor,
            onSelectColor = onAccentColorChanged
        )
    }

    // 3. Time & Date Options
    SettingsSection(title = "Time & Date Options") {
        SettingsToggleRow(
            title = "24-Hour Format",
            subtitle = "Display time as 00:00 to 23:59",
            checked = preferences.is24Hour,
            onCheckedChange = on24HourChanged,
            accentColor = accentColor
        )

        HorizontalDivider(
            color = StandByBorder.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        val isFlipClock = preferences.clockFace == ClockFaceType.RETRO_FLIP
        SettingsToggleRow(
            title = "Show Seconds",
            subtitle = if (isFlipClock) {
                "Flip clock displays hours and minutes only"
            } else {
                "Digital counter & analog sweeping second hand"
            },
            checked = if (isFlipClock) false else preferences.showSeconds,
            enabled = !isFlipClock,
            onCheckedChange = onShowSecondsChanged,
            accentColor = accentColor
        )

        HorizontalDivider(
            color = StandByBorder.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingsToggleRow(
            title = "Show Date",
            subtitle = "Display date capsule at top left",
            checked = preferences.showDate,
            onCheckedChange = onShowDateChanged,
            accentColor = accentColor
        )
    }

    // 4. Display Mode (Night Mode & Burn-In Protection & Keep Screen Awake & Battery Status)
    SettingsSection(title = "Display Mode") {
        SettingsToggleRow(
            title = "Night Mode",
            subtitle = "Switch clock display to red on black for low-light environments",
            checked = preferences.isNightModeEnabled,
            onCheckedChange = onNightModeChanged,
            accentColor = accentColor
        )

        HorizontalDivider(
            color = StandByBorder.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingsToggleRow(
            title = "OLED burn-in protection",
            subtitle = "Slightly shifts clock position at regular intervals to reduce localized display wear",
            checked = preferences.isBurnInProtectionEnabled,
            onCheckedChange = onBurnInProtectionChanged,
            accentColor = accentColor
        )

        HorizontalDivider(
            color = StandByBorder.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingsToggleRow(
            title = "Keep screen awake",
            subtitle = "Keeps the display awake while the clock screen is active. This may increase power consumption.",
            checked = preferences.keepScreenAwake,
            onCheckedChange = onKeepScreenAwakeChanged,
            accentColor = accentColor
        )

        HorizontalDivider(
            color = StandByBorder.copy(alpha = 0.5f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingsToggleRow(
            title = "Show battery status",
            subtitle = "Displays battery percentage and charging state on the clock screen",
            checked = preferences.showBatteryStatus,
            onCheckedChange = onShowBatteryStatusChanged,
            accentColor = accentColor
        )
    }

    // 5. Media Controls
    SettingsSection(title = "Media Controls") {
        SettingsToggleRow(
            title = "Show media player",
            subtitle = "Display track information and playback controls from compatible media apps",
            checked = preferences.showMediaPlayer,
            onCheckedChange = onToggleMediaPlayer,
            accentColor = accentColor
        )
    }

    // 6. Calendar & Agenda
    SettingsSection(title = "Calendar & Agenda") {
        SettingsToggleRow(
            title = "Show calendar agenda",
            subtitle = "Display current month and upcoming events from your calendar",
            checked = preferences.showCalendarAgenda,
            onCheckedChange = onToggleCalendarAgenda,
            accentColor = accentColor
        )
    }

    // 7. Weather & Forecast
    SettingsSection(title = "Weather & Forecast") {
        SettingsToggleRow(
            title = "Show weather",
            subtitle = "Display local weather and forecast using approximate location and internet",
            checked = preferences.showWeatherWidget,
            onCheckedChange = onToggleWeatherWidget,
            accentColor = accentColor
        )
    }

    // 8. Photo Frame
    SettingsSection(title = "Photo Frame") {
        SettingsToggleRow(
            title = "Show photo frame",
            subtitle = "Display personal photos as an ambient slideshow in widget stacks",
            checked = preferences.showPhotoFrame,
            onCheckedChange = onTogglePhotoFrame,
            accentColor = accentColor
        )

        if (preferences.showPhotoFrame) {
            HorizontalDivider(
                color = StandByBorder.copy(alpha = 0.5f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            PhotoFrameSettingsControls(
                photoCount = preferences.photoFrameUris.size,
                accentColor = accentColor,
                onChoosePhotos = onLaunchPhotoPicker,
                onRemoveAllPhotos = onRemoveAllPhotos,
                onOpenFullscreen = onOpenFullscreenPhotoFrame
            )
        }
    }

    // 9. Charging Awareness
    SettingsSection(title = "Charging Awareness") {
        SettingsToggleRow(
            title = "Show notification when charging",
            subtitle = "Display a notification to open StandBy Mode when power is connected",
            checked = preferences.showChargingNotification,
            onCheckedChange = onToggleChargingNotification,
            accentColor = accentColor
        )
        SettingsToggleRow(
            title = "Auto-activate in landscape while charging",
            subtitle = "Enter StandBy Mode when charging horizontally; dismiss when unplugged or upright",
            checked = preferences.autoActivateOnChargeAndLandscape,
            onCheckedChange = onAutoActivateChanged,
            accentColor = accentColor
        )
    }

    // 10. Setup & Permissions Guide
    SettingsSection(title = "Setup & Permissions Guide") {
        SettingsClickableRow(
            title = "Revisit Onboarding Walkthrough",
            subtitle = "Review charging setup, screen rules, and optional feature permissions",
            onClick = onRevisitOnboarding,
            accentColor = accentColor
        )
    }
}

