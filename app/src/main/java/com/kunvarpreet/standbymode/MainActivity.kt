package com.kunvarpreet.standbymode

import android.app.Activity
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kunvarpreet.standbymode.core.battery.BatteryState
import com.kunvarpreet.standbymode.core.battery.rememberBatteryState
import com.kunvarpreet.standbymode.core.burnin.BurnInProtectionPolicy
import com.kunvarpreet.standbymode.core.calendar.CalendarAgendaUiState
import com.kunvarpreet.standbymode.core.calendar.CalendarRepository
import com.kunvarpreet.standbymode.core.charging.ChargingNotificationManager
import com.kunvarpreet.standbymode.core.display.DisplaySpace
import com.kunvarpreet.standbymode.core.display.KeepScreenAwakePolicy
import com.kunvarpreet.standbymode.core.media.MediaNotificationListenerService
import com.kunvarpreet.standbymode.core.media.MediaSessionRepository
import com.kunvarpreet.standbymode.core.media.MediaWidgetUiState
import com.kunvarpreet.standbymode.core.photoframe.AsyncPhotoLoader
import com.kunvarpreet.standbymode.core.photoframe.FaceCropAnalyzer
import com.kunvarpreet.standbymode.core.photoframe.PhotoFrameLogic
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.ClockThemePalette
import com.kunvarpreet.standbymode.core.preferences.PreferencesRepository
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.core.time.rememberLiveTime
import com.kunvarpreet.standbymode.core.weather.WeatherRepository
import com.kunvarpreet.standbymode.core.weather.WeatherUiState
import com.kunvarpreet.standbymode.core.widget.DuoWidgetPolicy
import com.kunvarpreet.standbymode.core.widget.DuoWidgetType
import com.kunvarpreet.standbymode.ui.clock.AnalogClockDialView
import com.kunvarpreet.standbymode.ui.clock.BatteryStatusPill
import com.kunvarpreet.standbymode.ui.clock.BoldDigitalClockView
import com.kunvarpreet.standbymode.ui.clock.DatePill
import com.kunvarpreet.standbymode.ui.clock.RetroFlipClockView
import com.kunvarpreet.standbymode.ui.display.PageDotsIndicator
import com.kunvarpreet.standbymode.ui.duo.DuoLayoutScreen
import com.kunvarpreet.standbymode.ui.media.MediaMiniWidget
import com.kunvarpreet.standbymode.ui.photoframe.FullscreenPhotoFrameScreen
import com.kunvarpreet.standbymode.ui.settings.SettingsButton
import com.kunvarpreet.standbymode.ui.settings.SettingsScreen
import com.kunvarpreet.standbymode.ui.theme.StandByBackground
import com.kunvarpreet.standbymode.ui.theme.StandByModeTheme
import kotlinx.coroutines.launch

/**
 * Root navigation destinations.
 */
enum class AppScreen {
    CLOCK,
    SETTINGS,
    FULLSCREEN_PHOTO_FRAME
}

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesRepository = PreferencesRepository(applicationContext)

        enableEdgeToEdge()
        setContent {
            StandByModeTheme {
                StandByAppShell(repository = preferencesRepository)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ChargingNotificationManager.isAppInForeground = true
        ChargingNotificationManager.cancelNotification(this)
    }

    override fun onStop() {
        super.onStop()
        ChargingNotificationManager.isAppInForeground = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        FaceCropAnalyzer.releaseDetector()
    }
}

/**
 * Production App Shell managing persistent preferences and screen navigation.
 */
@Composable
fun StandByAppShell(
    repository: PreferencesRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val preferences by repository.preferencesFlow.collectAsState(
        initial = UserClockPreferences()
    )

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.CLOCK) }

    // Hoist pager state across screen navigation so returning from Settings restores the viewed display space
    val pagerState = rememberPagerState(
        initialPage = DisplaySpace.DEFAULT_PAGE_INDEX
    ) {
        DisplaySpace.entries.size
    }

    val context = LocalContext.current
    val window = remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()?.window
    }

    val mediaSessionRepository = remember(context) {
        MediaSessionRepository(context.applicationContext)
    }

    val calendarRepository = remember(context) {
        CalendarRepository(context.applicationContext)
    }

    val weatherRepository = remember(context) {
        WeatherRepository(context.applicationContext)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isActivityForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isActivityForeground = true
                    mediaSessionRepository.refreshAccess()
                    calendarRepository.refresh()
                    if (weatherRepository.hasLocationPermission() && preferences.showWeatherWidget) {
                        weatherRepository.refresh()
                    }
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> {
                    isActivityForeground = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldKeepScreenOn = remember(preferences.keepScreenAwake, currentScreen, isActivityForeground) {
        KeepScreenAwakePolicy.shouldKeepScreenOn(
            isPreferenceEnabled = preferences.keepScreenAwake,
            currentScreen = currentScreen,
            isActivityForeground = isActivityForeground
        )
    }

    LaunchedEffect(shouldKeepScreenOn, window) {
        if (window != null) {
            if (shouldKeepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    DisposableEffect(window) {
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val defaultLeft = remember {
        DuoWidgetPolicy.getDefaultWidget(
            isLeftPane = true,
            showMediaPlayer = preferences.showMediaPlayer,
            showCalendarAgenda = preferences.showCalendarAgenda,
            showWeatherWidget = preferences.showWeatherWidget,
            showPhotoFrame = preferences.showPhotoFrame
        )
    }
    val defaultRight = remember {
        DuoWidgetPolicy.getDefaultWidget(
            isLeftPane = false,
            showMediaPlayer = preferences.showMediaPlayer,
            showCalendarAgenda = preferences.showCalendarAgenda,
            showWeatherWidget = preferences.showWeatherWidget,
            showPhotoFrame = preferences.showPhotoFrame
        )
    }

    var leftPaneWidget by rememberSaveable { mutableStateOf(defaultLeft) }
    var rightPaneWidget by rememberSaveable { mutableStateOf(defaultRight) }

    // Reconcile selections if preferences change (e.g. Media, Calendar, Weather, or Photo Frame toggled in Settings)
    val availableLeft = DuoWidgetPolicy.getAvailableWidgets(
        isLeftPane = true,
        showMediaPlayer = preferences.showMediaPlayer,
        showCalendarAgenda = preferences.showCalendarAgenda,
        showWeatherWidget = preferences.showWeatherWidget,
        showPhotoFrame = preferences.showPhotoFrame
    )
    val availableRight = DuoWidgetPolicy.getAvailableWidgets(
        isLeftPane = false,
        showMediaPlayer = preferences.showMediaPlayer,
        showCalendarAgenda = preferences.showCalendarAgenda,
        showWeatherWidget = preferences.showWeatherWidget,
        showPhotoFrame = preferences.showPhotoFrame
    )

    val reconciledLeft = DuoWidgetPolicy.resolveValidSelection(leftPaneWidget, availableLeft, defaultLeft)
    if (reconciledLeft != leftPaneWidget) {
        leftPaneWidget = reconciledLeft
    }

    val reconciledRight = DuoWidgetPolicy.resolveValidSelection(rightPaneWidget, availableRight, defaultRight)
    if (reconciledRight != rightPaneWidget) {
        rightPaneWidget = reconciledRight
    }

    // Live time engine is shared across screens without duplicating tickers
    val formattedTime by rememberLiveTime(
        showSeconds = preferences.showSeconds && preferences.clockFace != ClockFaceType.RETRO_FLIP,
        is24Hour = preferences.is24Hour
    )

    val currentDisplaySpace = DisplaySpace.fromPageIndex(pagerState.currentPage)

    val shouldObserveBattery = DuoWidgetPolicy.isBatteryObservationNeeded(
        showBatteryStatusPref = preferences.showBatteryStatus,
        currentScreen = currentScreen,
        isActivityForeground = isActivityForeground,
        displaySpace = currentDisplaySpace,
        leftPaneWidget = leftPaneWidget,
        rightPaneWidget = rightPaneWidget
    )

    val batteryState = rememberBatteryState(
        enabled = shouldObserveBattery,
        isClockScreen = currentScreen == AppScreen.CLOCK,
        isActivityForeground = isActivityForeground
    )

    val shouldObserveMedia = DuoWidgetPolicy.isMediaVisible(
        isPreferenceEnabled = preferences.showMediaPlayer,
        currentScreen = currentScreen,
        isActivityForeground = isActivityForeground,
        displaySpace = currentDisplaySpace,
        leftPaneWidget = leftPaneWidget,
        rightPaneWidget = rightPaneWidget
    )

    DisposableEffect(shouldObserveMedia, mediaSessionRepository) {
        if (shouldObserveMedia) {
            mediaSessionRepository.startObserving()
        } else {
            mediaSessionRepository.stopObserving()
        }
        onDispose {
            mediaSessionRepository.stopObserving()
        }
    }

    val mediaWidgetUiState by mediaSessionRepository.uiStateFlow.collectAsState()

    val shouldObserveCalendar = DuoWidgetPolicy.isCalendarVisible(
        isPreferenceEnabled = preferences.showCalendarAgenda,
        currentScreen = currentScreen,
        isActivityForeground = isActivityForeground,
        displaySpace = currentDisplaySpace,
        leftPaneWidget = leftPaneWidget,
        rightPaneWidget = rightPaneWidget
    )

    DisposableEffect(shouldObserveCalendar, calendarRepository) {
        if (shouldObserveCalendar) {
            calendarRepository.startObserving()
        } else {
            calendarRepository.stopObserving()
        }
        onDispose {
            calendarRepository.stopObserving()
        }
    }

    val calendarUiState by calendarRepository.uiState.collectAsState()

    val shouldObserveWeather = DuoWidgetPolicy.isWeatherVisible(
        isPreferenceEnabled = preferences.showWeatherWidget,
        currentScreen = currentScreen,
        isActivityForeground = isActivityForeground,
        displaySpace = currentDisplaySpace,
        leftPaneWidget = leftPaneWidget,
        rightPaneWidget = rightPaneWidget
    )

    DisposableEffect(shouldObserveWeather, weatherRepository) {
        if (shouldObserveWeather) {
            weatherRepository.startObserving()
        } else {
            weatherRepository.stopObserving()
        }
        onDispose {
            weatherRepository.stopObserving()
        }
    }

    val weatherUiState by weatherRepository.uiState.collectAsState()

    val openMediaSettings = remember(context) {
        {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                putExtra(
                    ":settings:fragment_args_key",
                    ComponentName(context, MediaNotificationListenerService::class.java).flattenToString()
                )
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {}
            }
        }
    }

    val openCalendarSettings = remember(context) {
        {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {}
            }
        }
    }

    val openWeatherSettings = remember(context) {
        {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {}
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = PhotoFrameLogic.MAX_PHOTO_COUNT
        )
    ) { uris ->
        if (uris.isNotEmpty()) {
            val sanitized = PhotoFrameLogic.sanitizeUriList(
                uris.map { it.toString() },
                PhotoFrameLogic.MAX_PHOTO_COUNT
            )
            val contentResolver = context.contentResolver
            for (uriString in sanitized) {
                try {
                    val uri = uriString.toUri()
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Failed to take persistable URI permission for $uriString", e)
                }
            }
            val previousUris = preferences.photoFrameUris
            val orphanedUris = PhotoFrameLogic.findOrphanedUris(previousUris, sanitized)
            for (oldUriString in orphanedUris) {
                try {
                    val uri = oldUriString.toUri()
                    contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Failed to release URI permission for $oldUriString", e)
                }
            }
            coroutineScope.launch {
                repository.updatePhotoFrameUris(sanitized)
            }
        }
    }

    val onLaunchPhotoPicker: () -> Unit = remember(photoPickerLauncher) {
        {
            try {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to launch Photo Picker", e)
            }
        }
    }

    val onRemoveAllPhotos: () -> Unit = remember(context, preferences.photoFrameUris) {
        {
            val previousUris = preferences.photoFrameUris
            val contentResolver = context.contentResolver
            for (uriString in previousUris) {
                try {
                    val uri = uriString.toUri()
                    contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Failed to release URI permission for $uriString", e)
                }
            }
            AsyncPhotoLoader.clearCache()
            coroutineScope.launch {
                repository.updatePhotoFrameUris(emptyList())
            }
        }
    }

    when (currentScreen) {
        AppScreen.CLOCK -> {
            FullscreenClockScreen(
                preferences = preferences,
                formattedTime = formattedTime,
                batteryState = batteryState,
                mediaWidgetUiState = mediaWidgetUiState,
                calendarUiState = calendarUiState,
                weatherUiState = weatherUiState,
                pagerState = pagerState,
                isActivityForeground = isActivityForeground,
                leftPaneWidget = leftPaneWidget,
                rightPaneWidget = rightPaneWidget,
                onLeftPaneWidgetChanged = { leftPaneWidget = it },
                onRightPaneWidgetChanged = { rightPaneWidget = it },
                onMediaPlayPause = { mediaSessionRepository.playPause() },
                onMediaPrevious = { mediaSessionRepository.skipToPrevious() },
                onMediaNext = { mediaSessionRepository.skipToNext() },
                onMediaSeek = { positionMillis -> mediaSessionRepository.seekTo(positionMillis) },
                onOpenMediaPermissionSettings = openMediaSettings,
                onOpenCalendarPermissionSettings = openCalendarSettings,
                onCalendarRetry = { calendarRepository.refresh() },
                onOpenWeatherPermissionSettings = openWeatherSettings,
                onWeatherRetry = { weatherRepository.refresh() },
                onLaunchPhotoPicker = onLaunchPhotoPicker,
                onOpenFullscreenPhotoFrame = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(DisplaySpace.PHOTO_FRAME.pageIndex)
                    }
                },
                onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                modifier = modifier
            )
        }
        AppScreen.SETTINGS -> {
            BackHandler {
                currentScreen = AppScreen.CLOCK
            }
            SettingsScreen(
                preferences = preferences,
                formattedTime = formattedTime,
                onClockFaceChanged = { face ->
                    coroutineScope.launch { repository.updateClockFace(face) }
                },
                on24HourChanged = { is24Hour ->
                    coroutineScope.launch { repository.update24Hour(is24Hour) }
                },
                onShowSecondsChanged = { showSeconds ->
                    coroutineScope.launch { repository.updateShowSeconds(showSeconds) }
                },
                onShowDateChanged = { showDate ->
                    coroutineScope.launch { repository.updateShowDate(showDate) }
                },
                onAccentColorChanged = { colorOption ->
                    coroutineScope.launch { repository.updateAccentColor(colorOption) }
                },
                onNightModeChanged = { isNightMode ->
                    coroutineScope.launch { repository.updateNightMode(isNightMode) }
                },
                onShowChargingNotificationChanged = { showChargingNotification ->
                    coroutineScope.launch { repository.updateShowChargingNotification(showChargingNotification) }
                },
                onBurnInProtectionChanged = { isBurnInProtectionEnabled ->
                    coroutineScope.launch { repository.updateBurnInProtection(isBurnInProtectionEnabled) }
                },
                onKeepScreenAwakeChanged = { keepAwake ->
                    coroutineScope.launch { repository.updateKeepScreenAwake(keepAwake) }
                },
                onShowBatteryStatusChanged = { showBattery ->
                    coroutineScope.launch { repository.updateShowBatteryStatus(showBattery) }
                },
                onShowMediaPlayerChanged = { showMedia ->
                    coroutineScope.launch { repository.updateShowMediaPlayer(showMedia) }
                },
                onShowCalendarAgendaChanged = { showCalendar ->
                    coroutineScope.launch { repository.updateShowCalendarAgenda(showCalendar) }
                },
                onShowWeatherWidgetChanged = { showWeather ->
                    coroutineScope.launch { repository.updateShowWeatherWidget(showWeather) }
                },
                onTogglePhotoFrame = { showPhotoFrame ->
                    coroutineScope.launch { repository.updateShowPhotoFrame(showPhotoFrame) }
                },
                onLaunchPhotoPicker = onLaunchPhotoPicker,
                onRemoveAllPhotos = onRemoveAllPhotos,
                onOpenFullscreenPhotoFrame = {
                    currentScreen = AppScreen.CLOCK
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(DisplaySpace.PHOTO_FRAME.pageIndex)
                    }
                },
                onDismiss = { currentScreen = AppScreen.CLOCK },
                modifier = modifier
            )
        }
        AppScreen.FULLSCREEN_PHOTO_FRAME -> {
            val palette = ClockThemePalette.resolve(
                isNightMode = preferences.isNightModeEnabled,
                selectedAccent = preferences.accentColor
            )
            FullscreenPhotoFrameScreen(
                photoUris = preferences.photoFrameUris,
                isSlideshowActive = isActivityForeground && preferences.showPhotoFrame,
                isNightMode = preferences.isNightModeEnabled,
                accentColor = palette.accentColor,
                formattedTime = formattedTime,
                showDate = preferences.showDate,
                onBack = { currentScreen = AppScreen.CLOCK },
                onChoosePhotos = onLaunchPhotoPicker,
                modifier = modifier
            )
        }
    }
}

/**
 * StandBy ambient display screen hosting horizontal swipe navigation between
 * Duo Layout (Page 0) and Fullscreen Clock (Page 1).
 */
@Composable
fun FullscreenClockScreen(
    modifier: Modifier = Modifier,
    preferences: UserClockPreferences,
    formattedTime: com.kunvarpreet.standbymode.core.time.FormattedTime,
    batteryState: BatteryState = BatteryState(),
    mediaWidgetUiState: MediaWidgetUiState = MediaWidgetUiState.NoActiveMedia,
    calendarUiState: CalendarAgendaUiState = CalendarAgendaUiState.Loading,
    weatherUiState: WeatherUiState = WeatherUiState.Loading,
    pagerState: PagerState = rememberPagerState(initialPage = DisplaySpace.DEFAULT_PAGE_INDEX) {
        DisplaySpace.entries.size
    },
    isActivityForeground: Boolean = true,
    leftPaneWidget: DuoWidgetType = DuoWidgetType.CLOCK,
    rightPaneWidget: DuoWidgetType = if (preferences.showMediaPlayer) DuoWidgetType.MEDIA else DuoWidgetType.BATTERY_STATUS,
    onLeftPaneWidgetChanged: (DuoWidgetType) -> Unit = {},
    onRightPaneWidgetChanged: (DuoWidgetType) -> Unit = {},
    onMediaPlayPause: () -> Unit = {},
    onMediaPrevious: () -> Unit = {},
    onMediaNext: () -> Unit = {},
    onMediaSeek: (Long) -> Unit = {},
    onOpenMediaPermissionSettings: () -> Unit = {},
    onOpenCalendarPermissionSettings: () -> Unit = {},
    onCalendarRetry: () -> Unit = {},
    onOpenWeatherPermissionSettings: () -> Unit = {},
    onWeatherRetry: () -> Unit = {},
    onLaunchPhotoPicker: () -> Unit = {},
    onOpenFullscreenPhotoFrame: (() -> Unit)? = null,
    onOpenSettings: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val palette = ClockThemePalette.resolve(
        isNightMode = preferences.isNightModeEnabled,
        selectedAccent = preferences.accentColor
    )

    // Determine target pixel shift offset based on current 5-minute interval and protection preference
    val bucketIndex = formattedTime.timestamp.epochSecond / (BurnInProtectionPolicy.INTERVAL_MINUTES * 60L)
    val targetOffset = remember(bucketIndex, preferences.isBurnInProtectionEnabled) {
        BurnInProtectionPolicy.calculateOffset(
            instant = formattedTime.timestamp,
            isEnabled = preferences.isBurnInProtectionEnabled
        )
    }

    // Gentle 400ms transition between nearby offsets
    val animatedX by animateDpAsState(
        targetValue = targetOffset.xDp.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "burn_in_pixel_shift_x"
    )
    val animatedY by animateDpAsState(
        targetValue = targetOffset.yDp.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "burn_in_pixel_shift_y"
    )

    if (pagerState.currentPage == DisplaySpace.FULLSCREEN_CLOCK.pageIndex) {
        BackHandler {
            coroutineScope.launch {
                pagerState.animateScrollToPage(DisplaySpace.DUO_LAYOUT.pageIndex)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StandByBackground)
            .safeDrawingPadding()
    ) {
        // Horizontal display space pager: 3 primary display spaces (Duo Layout, Fullscreen Clock, Photo Frame)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "StandBy display spaces pager"
                }
        ) { pageIndex ->
            val displaySpace = DisplaySpace.fromPageIndex(pageIndex)

            when (displaySpace) {
                DisplaySpace.DUO_LAYOUT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = animatedX, y = animatedY)
                    ) {
                        DuoLayoutScreen(
                            preferences = preferences,
                            formattedTime = formattedTime,
                            batteryState = batteryState,
                            mediaWidgetUiState = mediaWidgetUiState,
                            calendarUiState = calendarUiState,
                            weatherUiState = weatherUiState,
                            leftPaneWidget = leftPaneWidget,
                            rightPaneWidget = rightPaneWidget,
                            onLeftPaneWidgetChanged = onLeftPaneWidgetChanged,
                            onRightPaneWidgetChanged = onRightPaneWidgetChanged,
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
                            onOpenFullscreenPhotoFrame = onOpenFullscreenPhotoFrame,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                DisplaySpace.FULLSCREEN_CLOCK -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = animatedX, y = animatedY)
                    ) {
                        FullscreenClockPage(
                            preferences = preferences,
                            formattedTime = formattedTime,
                            batteryState = batteryState,
                            palette = palette,
                            mediaWidgetUiState = mediaWidgetUiState,
                            onMediaPlayPause = onMediaPlayPause,
                            onMediaPrevious = onMediaPrevious,
                            onMediaNext = onMediaNext,
                            onMediaSeek = onMediaSeek,
                            onOpenMediaPermissionSettings = onOpenMediaPermissionSettings,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                DisplaySpace.PHOTO_FRAME -> {
                    // Fullscreen Photo Frame: full-bleed without clock pixel shift
                    FullscreenPhotoFrameScreen(
                        photoUris = preferences.photoFrameUris,
                        isSlideshowActive = isActivityForeground && preferences.showPhotoFrame && pagerState.currentPage == DisplaySpace.PHOTO_FRAME.pageIndex,
                        isNightMode = preferences.isNightModeEnabled,
                        accentColor = palette.accentColor,
                        formattedTime = formattedTime,
                        showDate = preferences.showDate,
                        onBack = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(DisplaySpace.FULLSCREEN_CLOCK.pageIndex)
                            }
                        },
                        onChoosePhotos = onLaunchPhotoPicker,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Top-Right: Discoverable Settings Button (Stationary across all display spaces)
        SettingsButton(
            onClick = onOpenSettings,
            isNightMode = preferences.isNightModeEnabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 8.dp)
        )

        // Bottom-Center: Subtle Page Dots Indicator (Stationary across all display spaces)
        PageDotsIndicator(
            pageCount = DisplaySpace.entries.size,
            currentPage = pagerState.currentPage,
            accentColor = palette.accentColor,
            isNightMode = preferences.isNightModeEnabled,
            onPageSelected = { targetPage ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(targetPage)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
    }
}

/**
 * Fullscreen ambient clock page preserving the existing single-focus clock experience,
 * including date capsule, optional battery indicator, center clock face, and compact media player.
 */
@Composable
fun FullscreenClockPage(
    preferences: UserClockPreferences,
    formattedTime: com.kunvarpreet.standbymode.core.time.FormattedTime,
    batteryState: BatteryState,
    palette: ClockThemePalette,
    mediaWidgetUiState: MediaWidgetUiState,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    onMediaSeek: (Long) -> Unit,
    onOpenMediaPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Fullscreen Clock display"
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
                .padding(bottom = if (preferences.showMediaPlayer) 88.dp else 24.dp),
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

        // Bottom-Center: Compact Media Widget (positioned comfortably above stationary page dots)
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Preview(
    name = "Clock Screen - Landscape",
    device = "spec:width=800dp,height=400dp,dpi=320",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun FullscreenClockLandscapePreview() {
    StandByModeTheme {
        FullscreenClockScreen(
            preferences = UserClockPreferences(),
            formattedTime = com.kunvarpreet.standbymode.core.time.FormattedTime(
                hours = "10",
                minutes = "42",
                seconds = "47",
                amPm = "AM",
                formattedDate = "WED, OCT 18"
            ),
            onOpenSettings = {}
        )
    }
}

@Preview(
    name = "Clock Screen - Portrait",
    device = "spec:width=400dp,height=800dp,dpi=320",
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun FullscreenClockPortraitPreview() {
    StandByModeTheme {
        FullscreenClockScreen(
            preferences = UserClockPreferences(),
            formattedTime = com.kunvarpreet.standbymode.core.time.FormattedTime(
                hours = "10",
                minutes = "42",
                seconds = "47",
                amPm = "AM",
                formattedDate = "WED, OCT 18"
            ),
            onOpenSettings = {}
        )
    }
}