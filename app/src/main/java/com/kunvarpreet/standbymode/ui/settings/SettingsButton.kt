package com.kunvarpreet.standbymode.ui.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted

/**
 * C02: SettingsButton
 * Discoverable top-right action button navigating to the Settings screen.
 * Provides a 48dp touch target with a 24dp icon and accessible description.
 */
@Composable
fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false
) {
    val tint = if (isNightMode) StandByNightTextMuted else StandByTextMuted

    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Open Settings",
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
