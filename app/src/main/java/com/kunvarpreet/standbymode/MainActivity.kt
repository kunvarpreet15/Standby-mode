package com.kunvarpreet.standbymode

import android.app.Activity
import android.content.ContextWrapper
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kunvarpreet.standbymode.core.battery.BatteryState
import com.kunvarpreet.standbymode.core.battery.rememberBatteryState
import com.kunvarpreet.standbymode.core.burnin.BurnInProtectionPolicy
import com.kunvarpreet.standbymode.core.charging.ChargingNotificationManager
import com.kunvarpreet.standbymode.core.display.KeepScreenAwakePolicy
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.ClockThemePalette
import com.kunvarpreet.standbymode.core.preferences.PreferencesRepository
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.core.time.rememberLiveTime
import com.kunvarpreet.standbymode.ui.clock.AnalogClockDialView
import com.kunvarpreet.standbymode.ui.clock.BatteryStatusPill
import com.kunvarpreet.standbymode.ui.clock.BoldDigitalClockView
import com.kunvarpreet.standbymode.ui.clock.DatePill
import com.kunvarpreet.standbymode.ui.clock.RetroFlipClockView
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
    SETTINGS
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

    val context = LocalContext.current
    val window = remember(context) {
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>()
            .firstOrNull()?.window
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

    // Live time engine is shared across screens without duplicating tickers
    val formattedTime by rememberLiveTime(
        showSeconds = preferences.showSeconds && preferences.clockFace != ClockFaceType.RETRO_FLIP,
        is24Hour = preferences.is24Hour
    )

    val batteryState = rememberBatteryState(
        enabled = preferences.showBatteryStatus,
        isClockScreen = currentScreen == AppScreen.CLOCK,
        isActivityForeground = isActivityForeground
    )

    when (currentScreen) {
        AppScreen.CLOCK -> {
            FullscreenClockScreen(
                preferences = preferences,
                formattedTime = formattedTime,
                batteryState = batteryState,
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
                onDismiss = { currentScreen = AppScreen.CLOCK },
                modifier = modifier
            )
        }
    }
}

/**
 * Fullscreen ambient clock screen displaying the active persistent clock face,
 * optional date capsule, optional battery status indicator, and discoverable Settings action button.
 */
@Composable
fun FullscreenClockScreen(
    preferences: UserClockPreferences,
    formattedTime: com.kunvarpreet.standbymode.core.time.FormattedTime,
    batteryState: BatteryState = BatteryState(),
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StandByBackground)
            .safeDrawingPadding()
    ) {
        // Shifted Visual Content Group (Clock face + Date Pill + Battery Status Pill)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = animatedX, y = animatedY)
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
                modifier = Modifier.fillMaxSize(),
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

        // Top-Right: Discoverable Settings Button (Stationary)
        SettingsButton(
            onClick = onOpenSettings,
            isNightMode = preferences.isNightModeEnabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 8.dp)
        )
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