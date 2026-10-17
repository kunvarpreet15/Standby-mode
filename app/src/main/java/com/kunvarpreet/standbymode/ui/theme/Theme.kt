package com.kunvarpreet.standbymode.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StandByAccentOrange,
    onPrimary = Color(0xFF000000),
    background = StandByBackground,
    onBackground = StandByTextPrimary,
    surface = StandBySurface,
    onSurface = StandByTextPrimary,
    surfaceVariant = StandBySurfaceCard,
    onSurfaceVariant = StandByTextMuted,
    outline = StandByBorder
)

@Composable
fun StandByModeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}