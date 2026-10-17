package com.kunvarpreet.standbymode.ui.clock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * C03: FlipDigitCard
 * Individual split-flap digit card with upper and lower halves, horizontal seam,
 * side hinge details, and restrained 3D folding animation on digit transition.
 */
@Composable
fun FlipDigitCard(
    digit: Char,
    cardWidth: Dp,
    cardHeight: Dp,
    fontSize: TextUnit,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    cardColor: Color = StandBySurfaceCard,
    textColor: Color = StandByTextPrimary,
    isNightMode: Boolean = false
) {
    val resolvedCardColor = if (isNightMode) StandByNightSurface else cardColor
    val resolvedTextColor = if (isNightMode) StandByNightText else textColor
    val resolvedBorderColor = if (isNightMode) StandByNightSurface else StandByBorder

    var currentDigit by remember { mutableStateOf(digit) }
    var previousDigit by remember { mutableStateOf(digit) }
    var isInitialized by remember { mutableStateOf(false) }
    val flipProgress = remember { Animatable(1f) }

    LaunchedEffect(digit) {
        if (!isInitialized) {
            isInitialized = true
            currentDigit = digit
            previousDigit = digit
        } else if (digit != currentDigit) {
            previousDigit = currentDigit
            currentDigit = digit
            flipProgress.snapTo(0f)
            flipProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
    }

    val halfHeight = cardHeight / 2f
    val seamGap = 1.5.dp
    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()

    val digitStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        lineHeight = fontSize,
        color = resolvedTextColor,
        textAlign = TextAlign.Center
    )

    Box(
        modifier = modifier.size(cardWidth, cardHeight)
    ) {
        // 1. Static Background Card (Base layer)
        Column(modifier = Modifier.fillMaxSize()) {
            // Background Upper Flap: Shows the NEW digit
            UpperFlap(
                digit = currentDigit,
                width = cardWidth,
                height = halfHeight - (seamGap / 2),
                fullHeight = cardHeight,
                cornerRadius = cornerRadius,
                cardColor = resolvedCardColor,
                borderColor = resolvedBorderColor,
                style = digitStyle,
                textMeasurer = textMeasurer
            )
            Spacer(modifier = Modifier.height(seamGap))
            // Background Lower Flap: Shows OLD digit during flip (revealed underneath), then NEW
            LowerFlap(
                digit = if (flipProgress.value < 0.5f) previousDigit else currentDigit,
                width = cardWidth,
                height = halfHeight - (seamGap / 2),
                fullHeight = cardHeight,
                cornerRadius = cornerRadius,
                cardColor = resolvedCardColor,
                borderColor = resolvedBorderColor,
                style = digitStyle,
                textMeasurer = textMeasurer
            )
        }

        // 2. Animated Flaps (Active only during flip transition)
        if (flipProgress.value < 1f) {
            val progress = flipProgress.value

            if (progress < 0.5f) {
                // Top flap folding downward (0° -> 90°) showing PREVIOUS upper digit
                val rotationAngle = -progress * 180f
                val shadowAlpha = (progress * 2f) * 0.45f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(halfHeight - (seamGap / 2))
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            rotationX = rotationAngle
                            cameraDistance = 16 * density
                            transformOrigin = TransformOrigin(0.5f, 1.0f)
                        }
                ) {
                    UpperFlap(
                        digit = previousDigit,
                        width = cardWidth,
                        height = halfHeight - (seamGap / 2),
                        fullHeight = cardHeight,
                        cornerRadius = cornerRadius,
                        cardColor = resolvedCardColor,
                        borderColor = resolvedBorderColor,
                        style = digitStyle,
                        textMeasurer = textMeasurer
                    )
                    // Dynamic shadow as flap folds downward
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    topStart = cornerRadius,
                                    topEnd = cornerRadius,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            )
                            .background(Color.Black.copy(alpha = shadowAlpha))
                    )
                }
            } else {
                // Bottom flap unfolding downward into place (90° -> 0°) showing NEW lower digit
                val rotationAngle = 180f - (progress * 180f)
                val shadowAlpha = ((1f - progress) * 2f) * 0.45f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(halfHeight - (seamGap / 2))
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            rotationX = rotationAngle
                            cameraDistance = 16 * density
                            transformOrigin = TransformOrigin(0.5f, 0.0f)
                        }
                ) {
                    LowerFlap(
                        digit = currentDigit,
                        width = cardWidth,
                        height = halfHeight - (seamGap / 2),
                        fullHeight = cardHeight,
                        cornerRadius = cornerRadius,
                        cardColor = resolvedCardColor,
                        borderColor = resolvedBorderColor,
                        style = digitStyle,
                        textMeasurer = textMeasurer
                    )
                    // Shadow dissipating as flap snaps into place
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 0.dp,
                                    topEnd = 0.dp,
                                    bottomStart = cornerRadius,
                                    bottomEnd = cornerRadius
                                )
                            )
                            .background(Color.Black.copy(alpha = shadowAlpha))
                    )
                }
            }
        }

        // 3. Center Seam Overlay & Side Hinges
        // Center seam line
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black.copy(alpha = 0.6f))
        )

        // Left hinge notch
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(2.5.dp)
                .height(cardHeight * 0.08f)
                .background(Color.Black.copy(alpha = 0.85f))
        )

        // Right hinge notch
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(2.5.dp)
                .height(cardHeight * 0.08f)
                .background(Color.Black.copy(alpha = 0.85f))
        )
    }
}

/**
 * Draws the upper half of a split-flap card.
 * Clips to top rounded corners, drawing only the upper half of the measured glyph.
 */
@Composable
private fun UpperFlap(
    digit: Char,
    width: Dp,
    height: Dp,
    fullHeight: Dp,
    cornerRadius: Dp,
    cardColor: Color,
    borderColor: Color = StandByBorder,
    style: TextStyle,
    textMeasurer: TextMeasurer
) {
    val layoutResult = textMeasurer.measure(
        text = AnnotatedString(digit.toString()),
        style = style
    )

    Box(
        modifier = Modifier
            .size(width, height)
            .clip(
                RoundedCornerShape(
                    topStart = cornerRadius,
                    topEnd = cornerRadius,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .background(cardColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(
                    topStart = cornerRadius,
                    topEnd = cornerRadius,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val textX = (size.width - layoutResult.size.width) / 2f
            val fullHeightPx = fullHeight.toPx()
            val textY = (fullHeightPx - layoutResult.size.height) / 2f
            drawText(
                textLayoutResult = layoutResult,
                topLeft = Offset(textX, textY)
            )
        }
    }
}

/**
 * Draws the lower half of a split-flap card.
 * Clips to bottom rounded corners, drawing only the lower half of the measured glyph.
 */
@Composable
private fun LowerFlap(
    digit: Char,
    width: Dp,
    height: Dp,
    fullHeight: Dp,
    cornerRadius: Dp,
    cardColor: Color,
    borderColor: Color = StandByBorder,
    style: TextStyle,
    textMeasurer: TextMeasurer
) {
    val layoutResult = textMeasurer.measure(
        text = AnnotatedString(digit.toString()),
        style = style
    )

    Box(
        modifier = Modifier
            .size(width, height)
            .clip(
                RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = cornerRadius,
                    bottomEnd = cornerRadius
                )
            )
            .background(cardColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = cornerRadius,
                    bottomEnd = cornerRadius
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val textX = (size.width - layoutResult.size.width) / 2f
            val fullHeightPx = fullHeight.toPx()
            val halfHeightPx = fullHeightPx / 2f
            val textY = ((fullHeightPx - layoutResult.size.height) / 2f) - halfHeightPx
            drawText(
                textLayoutResult = layoutResult,
                topLeft = Offset(textX, textY)
            )
        }
    }
}
