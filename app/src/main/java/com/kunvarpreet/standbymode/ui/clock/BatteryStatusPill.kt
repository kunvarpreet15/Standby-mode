package com.kunvarpreet.standbymode.ui.clock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.R
import com.kunvarpreet.standbymode.core.battery.BatteryState
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted

/**
 * Compact ambient battery status indicator capsule.
 *
 * Displays current battery percentage and an explicit charging icon when power is connected.
 * Adheres to the bedside design system tokens, Night Mode styling, and accessibility guidelines.
 */
@Composable
fun BatteryStatusPill(
    batteryState: BatteryState,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.Unspecified,
    isNightMode: Boolean = false
) {
    if (!batteryState.isAvailable || batteryState.percentage == null) return

    val surfaceColor = if (isNightMode) StandByNightSurface else StandBySurfaceCard
    val textColor = if (isNightMode) StandByNightTextMuted else StandByTextMuted
    val borderColor = if (isNightMode) StandByNightSurface else StandByBorder
    val iconColor = if (isNightMode) {
        StandByNightText
    } else {
        if (accentColor != Color.Unspecified) accentColor else textColor
    }

    val accessibilityDescription = buildString {
        append("Battery ")
        append(batteryState.percentage)
        append(" percent")
        if (batteryState.isCharging) {
            append(", charging")
        }
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.semantics {
            contentDescription = accessibilityDescription
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (batteryState.isCharging) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_charging_bolt),
                    contentDescription = null, // Described by parent semantics
                    tint = iconColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = "${batteryState.percentage}%",
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
