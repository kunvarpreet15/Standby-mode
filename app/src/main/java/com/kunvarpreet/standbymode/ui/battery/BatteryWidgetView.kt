package com.kunvarpreet.standbymode.ui.battery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * Dedicated, compact battery panel widget for the Duo Layout widget stack.
 * Displays battery level percentage, visual charge meter, and charging state.
 */
@Composable
fun BatteryWidgetView(
    modifier: Modifier = Modifier,
    batteryState: BatteryState,
    accentColor: Color,
    isNightMode: Boolean = false
) {
    val surfaceColor = if (isNightMode) StandByNightSurface else StandBySurfaceCard
    val borderColor = if (isNightMode) StandByNightSurface else StandByBorder
    val primaryTextColor = if (isNightMode) StandByNightText else StandByTextPrimary
    val mutedTextColor = if (isNightMode) StandByNightTextMuted else StandByTextMuted
    val activeAccentColor = if (isNightMode) StandByNightText else accentColor

    val percentage = batteryState.percentage
    val isCharging = batteryState.isCharging
    val isAvailable = batteryState.isAvailable && percentage != null

    val statusText = when {
        !isAvailable -> "Unavailable"
        percentage == 100 && isCharging -> "Fully Charged"
        isCharging -> "Charging"
        else -> "On Battery"
    }

    val accessibilityDesc = if (isAvailable) {
        "Battery status: $percentage percent, $statusText"
    } else {
        "Battery status unavailable"
    }

    Surface(
        modifier = modifier
            .widthIn(min = 260.dp, max = 380.dp)
            .padding(16.dp)
            .semantics {
                contentDescription = accessibilityDesc
            },
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
            // Header
            Text(
                text = "BATTERY STATUS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = mutedTextColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Large Percentage or Fallback
            if (isAvailable) {
                Text(
                    text = "$percentage%",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = primaryTextColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Visual Charge Meter
                val progressFraction = (percentage.coerceIn(0, 100)) / 100f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isNightMode) StandByNightSurface.copy(alpha = 0.6f) else StandByBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(activeAccentColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Badge (Charging bolt + state text)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isCharging) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_charging_bolt),
                            contentDescription = null,
                            tint = activeAccentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isCharging) activeAccentColor else mutedTextColor
                    )
                }
            } else {
                Text(
                    text = "--%",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = mutedTextColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Battery information unavailable",
                    fontSize = 13.sp,
                    color = mutedTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
