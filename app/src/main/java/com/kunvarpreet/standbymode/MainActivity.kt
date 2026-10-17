package com.kunvarpreet.standbymode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.ClockThemePalette
import com.kunvarpreet.standbymode.core.preferences.PreferencesRepository
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.core.time.rememberLiveTime
import com.kunvarpreet.standbymode.ui.clock.AnalogClockDialView
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

    // Live time engine is shared across screens without duplicating tickers
    val formattedTime by rememberLiveTime(
        showSeconds = preferences.showSeconds && preferences.clockFace != ClockFaceType.RETRO_FLIP,
        is24Hour = preferences.is24Hour
    )

    when (currentScreen) {
        AppScreen.CLOCK -> {
            FullscreenClockScreen(
                preferences = preferences,
                formattedTime = formattedTime,
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
                onDismiss = { currentScreen = AppScreen.CLOCK },
                modifier = modifier
            )
        }
    }
}

/**
 * Fullscreen ambient clock screen displaying the active persistent clock face,
 * optional date capsule, and discoverable Settings action button.
 */
@Composable
fun FullscreenClockScreen(
    preferences: UserClockPreferences,
    formattedTime: com.kunvarpreet.standbymode.core.time.FormattedTime,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ClockThemePalette.resolve(
        isNightMode = preferences.isNightModeEnabled,
        selectedAccent = preferences.accentColor
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StandByBackground)
            .safeDrawingPadding()
    ) {
        // Top-Left: Date Capsule (when enabled)
        if (preferences.showDate && formattedTime.formattedDate.isNotBlank()) {
            DatePill(
                dateText = formattedTime.formattedDate,
                isNightMode = preferences.isNightModeEnabled,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 16.dp)
            )
        }

        // Top-Right: Discoverable Settings Button
        SettingsButton(
            onClick = onOpenSettings,
            isNightMode = preferences.isNightModeEnabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 8.dp)
        )

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