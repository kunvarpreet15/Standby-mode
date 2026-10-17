package com.kunvarpreet.standbymode.ui.clock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.ui.theme.StandByAccentOrange
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * C05: BoldDigitalClockView
 * High-legibility digital time presentation that dynamically adapts to
 * landscape (single row HH:MM with optional seconds/AM-PM) and
 * portrait (stacked 2-line HH over MM) layouts.
 */
@Composable
fun BoldDigitalClockView(
    hours: String,
    minutes: String,
    seconds: String? = null,
    amPm: String? = null,
    accentColor: Color = StandByAccentOrange,
    isNightMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val digitsColor = if (isNightMode) StandByNightText else StandByTextPrimary
    val colonColor = if (isNightMode) StandByNightText else accentColor
    val secondsColor = if (isNightMode) StandByNightTextMuted else accentColor
    val amPmColor = if (isNightMode) StandByNightTextMuted else StandByTextMuted

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            // Landscape layout: Single horizontal row HH:MM
            val calculatedFontSize = min(maxWidth * 0.22f, maxHeight * 0.50f).value.sp
            val digitStyle = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "tnum",
                fontWeight = FontWeight.ExtraBold,
                fontSize = calculatedFontSize,
                lineHeight = calculatedFontSize,
                color = digitsColor
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = hours,
                    style = digitStyle
                )
                Text(
                    text = ":",
                    style = digitStyle.copy(color = colonColor)
                )
                Text(
                    text = minutes,
                    style = digitStyle
                )

                if (seconds != null || amPm != null) {
                    Column(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .align(Alignment.CenterVertically),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (amPm != null) {
                            Text(
                                text = amPm,
                                color = amPmColor,
                                fontSize = (calculatedFontSize.value * 0.18f).sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 1.sp
                            )
                        }
                        if (seconds != null) {
                            Text(
                                text = ":$seconds",
                                style = TextStyle(
                                    color = secondsColor,
                                    fontSize = (calculatedFontSize.value * 0.28f).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait layout: Stacked 2-line HH over MM (ensure 2 digits for stacked poster symmetry)
            val displayHours = if (hours.length == 1) "0$hours" else hours
            val calculatedFontSize = min(maxWidth * 0.40f, maxHeight * 0.28f).value.sp
            val calculatedLineHeight = (calculatedFontSize.value * 0.90f).sp
            val digitStyle = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontFeatureSettings = "tnum",
                fontWeight = FontWeight.ExtraBold,
                fontSize = calculatedFontSize,
                lineHeight = calculatedLineHeight,
                color = digitsColor,
                textAlign = TextAlign.Center
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = displayHours,
                    style = digitStyle
                )
                Text(
                    text = minutes,
                    style = digitStyle
                )

                if (seconds != null || amPm != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (amPm != null) {
                            Text(
                                text = amPm,
                                color = amPmColor,
                                fontSize = (calculatedFontSize.value * 0.18f).sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 1.2.sp
                            )
                        }
                        if (seconds != null) {
                            if (amPm != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = ":$seconds",
                                style = TextStyle(
                                    color = secondsColor,
                                    fontSize = (calculatedFontSize.value * 0.24f).sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
