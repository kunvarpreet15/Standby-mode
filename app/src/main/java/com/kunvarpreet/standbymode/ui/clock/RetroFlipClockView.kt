package com.kunvarpreet.standbymode.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.core.time.FlipClockExtractor
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * C04: RetroFlipClockView
 * Complete 4-digit mechanical flip clock displaying hours and minutes (HH:MM).
 * Automatically calculates proportional card sizing from parent bounds,
 * guarantees two-digit hours and minutes, and displays hours and minutes only.
 */
@Composable
fun RetroFlipClockView(
    hours: String,
    minutes: String,
    modifier: Modifier = Modifier,
    cardColor: Color = StandBySurfaceCard,
    textColor: Color = StandByTextPrimary,
    isNightMode: Boolean = false
) {
    val digits = FlipClockExtractor.extractDigits(hours, minutes)
    val colonColor = if (isNightMode) StandByNightTextMuted else StandByTextMuted

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight

        // 4 cards + 2 pair gaps (0.08 * cardWidth) + 1 center colon gap (0.28 * cardWidth) = 4.44 * cardWidth
        val maxCardWidthFromWidth = (availableWidth * 0.90f) / 4.44f
        val maxCardWidthFromHeight = (availableHeight * 0.65f) / 1.42f
        val cardWidth = min(maxCardWidthFromWidth, maxCardWidthFromHeight)
        val cardHeight = cardWidth * 1.42f
        val cornerRadius = cardWidth * 0.12f
        val gapPair = cardWidth * 0.08f
        val gapCenter = cardWidth * 0.28f
        val digitFontSize = (cardHeight * 0.58f).value.sp

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Hour Tens
            FlipDigitCard(
                digit = digits.hourTens,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                fontSize = digitFontSize,
                cornerRadius = cornerRadius,
                cardColor = cardColor,
                textColor = textColor,
                isNightMode = isNightMode
            )

            Spacer(modifier = Modifier.width(gapPair))

            // Hour Units
            FlipDigitCard(
                digit = digits.hourUnits,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                fontSize = digitFontSize,
                cornerRadius = cornerRadius,
                cardColor = cardColor,
                textColor = textColor,
                isNightMode = isNightMode
            )

            // Center Colon Divider
            Box(
                modifier = Modifier
                    .width(gapCenter)
                    .height(cardHeight),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val dotSize = cardWidth * 0.09f
                    val dotSpacing = cardHeight * 0.22f

                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(colonColor)
                    )
                    Spacer(modifier = Modifier.height(dotSpacing))
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(colonColor)
                    )
                }
            }

            // Minute Tens
            FlipDigitCard(
                digit = digits.minuteTens,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                fontSize = digitFontSize,
                cornerRadius = cornerRadius,
                cardColor = cardColor,
                textColor = textColor,
                isNightMode = isNightMode
            )

            Spacer(modifier = Modifier.width(gapPair))

            // Minute Units
            FlipDigitCard(
                digit = digits.minuteUnits,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                fontSize = digitFontSize,
                cornerRadius = cornerRadius,
                cardColor = cardColor,
                textColor = textColor,
                isNightMode = isNightMode
            )
        }
    }
}
