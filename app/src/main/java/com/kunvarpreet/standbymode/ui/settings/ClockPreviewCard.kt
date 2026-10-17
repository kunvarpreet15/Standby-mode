package com.kunvarpreet.standbymode.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.core.preferences.ClockFaceType
import com.kunvarpreet.standbymode.core.time.FormattedTime
import com.kunvarpreet.standbymode.ui.clock.AnalogClockDialView
import com.kunvarpreet.standbymode.ui.clock.BoldDigitalClockView
import com.kunvarpreet.standbymode.ui.clock.DatePill
import com.kunvarpreet.standbymode.ui.clock.RetroFlipClockView
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurface
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted

/**
 * C08: ClockPreviewCard
 * Scaled interactive live clock preview inside the Settings screen.
 * Seamlessly adapts to whatever clock face and display preferences are active.
 */
@Composable
fun ClockPreviewCard(
    modifier: Modifier = Modifier,
    clockFace: ClockFaceType,
    formattedTime: FormattedTime,
    showSeconds: Boolean,
    showDate: Boolean,
    accentColor: Color,
    isNightMode: Boolean = false

) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = StandBySurface,
        border = BorderStroke(1.dp, StandByBorder),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .padding(12.dp)
        ) {
            // Live Preview Badge at Bottom Center
            val badgeColor = if (isNightMode) StandByNightTextMuted.copy(alpha = 0.7f) else StandByTextMuted.copy(alpha = 0.7f)
            Text(
                text = "LIVE PREVIEW",
                color = badgeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            )

            // Date Capsule in Top-Left (when enabled)
            if (showDate && formattedTime.formattedDate.isNotBlank()) {
                DatePill(
                    dateText = formattedTime.formattedDate,
                    isNightMode = isNightMode,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }

            // Live Clock Face rendering
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                when (clockFace) {
                    ClockFaceType.BOLD_DIGITAL -> {
                        BoldDigitalClockView(
                            hours = formattedTime.hours,
                            minutes = formattedTime.minutes,
                            seconds = if (showSeconds) formattedTime.seconds else null,
                            amPm = formattedTime.amPm,
                            accentColor = accentColor,
                            isNightMode = isNightMode,
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
                            showSeconds = showSeconds,
                            accentColor = accentColor,
                            isNightMode = isNightMode,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    ClockFaceType.RETRO_FLIP -> {
                        RetroFlipClockView(
                            hours = formattedTime.hours,
                            minutes = formattedTime.minutes,
                            isNightMode = isNightMode,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
