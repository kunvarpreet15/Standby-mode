package com.kunvarpreet.standbymode.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.core.preferences.AccentColorOption
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.preferences.UserClockPreferences
import com.kunvarpreet.standbymode.core.time.FormattedTime
import com.kunvarpreet.standbymode.ui.theme.StandByBackground
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * Screen D: SettingsScreen (With Live Clock Preview)
 * Complete responsive settings screen supporting landscape two-column split
 * and portrait stacked layouts. Immediately writes changes to DataStore.
 */
@Composable
fun SettingsScreen(
    preferences: UserClockPreferences,
    formattedTime: FormattedTime,
    onClockFaceChanged: (ClockFaceType) -> Unit,
    on24HourChanged: (Boolean) -> Unit,
    onShowSecondsChanged: (Boolean) -> Unit,
    onShowDateChanged: (Boolean) -> Unit,
    onAccentColorChanged: (AccentColorOption) -> Unit,
    onNightModeChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = preferences.accentColor.color

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
                                onNightModeChanged = onNightModeChanged
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
                            onNightModeChanged = onNightModeChanged
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
    onNightModeChanged: (Boolean) -> Unit
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

    // 4. Display Mode (Night Mode)
    SettingsSection(title = "Display Mode") {
        SettingsToggleRow(
            title = "Night Mode",
            subtitle = "Switch clock display to red on black for low-light environments",
            checked = preferences.isNightModeEnabled,
            onCheckedChange = onNightModeChanged,
            accentColor = accentColor
        )
    }
}

