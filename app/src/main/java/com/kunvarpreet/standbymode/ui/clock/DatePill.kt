package com.kunvarpreet.standbymode.ui.clock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted

/**
 * C07: DatePill
 * Clean, ambient capsule showing day of week and date (e.g. "SUNDAY, OCT 18").
 */
@Composable
fun DatePill(
    modifier: Modifier = Modifier,
    dateText: String,
    isNightMode: Boolean = false
) {
    if (dateText.isBlank()) return

    val surfaceColor = if (isNightMode) StandByNightSurface else StandBySurfaceCard
    val textColor = if (isNightMode) StandByNightTextMuted else StandByTextMuted
    val borderColor = if (isNightMode) StandByNightSurface else StandByBorder

    Surface(
        shape = RoundedCornerShape(50),
        color = surfaceColor,
        border = BorderStroke(2.dp, borderColor),
        modifier = modifier
    ) {
        Text(
            text = dateText.uppercase(),
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
