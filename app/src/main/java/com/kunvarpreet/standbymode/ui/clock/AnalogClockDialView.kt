package com.kunvarpreet.standbymode.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kunvarpreet.standbymode.core.time.AnalogClockCalculator
import com.kunvarpreet.standbymode.ui.theme.StandByAccentOrange
import com.kunvarpreet.standbymode.ui.theme.StandByBackground
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByBorderInput
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * C06: AnalogClockDialView
 * Minimalist luxury analog dial clock drawn on Compose Canvas.
 * Supports adaptive geometry, 12 major hour ticks, 48 minor minute ticks,
 * precision hands, and optional sweeping/stepping seconds hand.
 */
@Composable
fun AnalogClockDialView(
    modifier: Modifier = Modifier,
    hourAngle: Float,
    minuteAngle: Float,
    secondAngle: Float? = null,
    accentColor: Color = StandByAccentOrange,
    isNightMode: Boolean = false
) {
    val rimColor = if (isNightMode) StandByNightSurface else StandByBorder
    val majorTickColor = if (isNightMode) StandByNightText else StandByTextPrimary
    val minorTickColor = if (isNightMode) StandByNightTextMuted else StandByBorderInput
    val hourHandColor = if (isNightMode) StandByNightText else StandByTextPrimary
    val minuteHandColor = if (isNightMode) StandByNightText else StandByTextPrimary
    val secondHandColor = if (isNightMode) StandByNightText else accentColor
    val pivotCapColor = if (isNightMode) StandByNightText else accentColor

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val radius = min(canvasWidth, canvasHeight) * 0.42f

            if (radius <= 0f) return@Canvas

            // 1. Outer Dial Rim
            val rimStrokeWidth = 2.dp.toPx()
            drawCircle(
                color = rimColor,
                radius = radius,
                center = center,
                style = Stroke(width = rimStrokeWidth)
            )

            // 2. Hour & Minute Ticks (60 ticks total)
            val majorTickLength = radius * 0.12f
            val minorTickLength = radius * 0.06f
            val majorTickWidth = 3.dp.toPx()
            val minorTickWidth = 1.dp.toPx()

            for (i in 0 until 60) {
                val angleDeg = i * 6f
                val isMajor = (i % 5 == 0)
                val tickLength = if (isMajor) majorTickLength else minorTickLength
                val tickColor = if (isMajor) majorTickColor else minorTickColor
                val strokeWidth = if (isMajor) majorTickWidth else minorTickWidth

                val startOffset = angleToOffset(center, radius - tickLength, angleDeg)
                val endOffset = angleToOffset(center, radius - (rimStrokeWidth / 2f), angleDeg)

                drawLine(
                    color = tickColor,
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 3. Hour Hand (length: 55% of radius)
            val hourHandLength = radius * 0.55f
            val hourHandWidth = 4.5.dp.toPx()
            val hourEnd = angleToOffset(center, hourHandLength, hourAngle)
            drawLine(
                color = hourHandColor,
                start = center,
                end = hourEnd,
                strokeWidth = hourHandWidth,
                cap = StrokeCap.Round
            )

            // 4. Minute Hand (length: 82% of radius)
            val minuteHandLength = radius * 0.82f
            val minuteHandWidth = 3.dp.toPx()
            val minuteEnd = angleToOffset(center, minuteHandLength, minuteAngle)
            drawLine(
                color = minuteHandColor,
                start = center,
                end = minuteEnd,
                strokeWidth = minuteHandWidth,
                cap = StrokeCap.Round
            )

            // 5. Optional Second Hand (length: 92% of radius, 15% tail)
            if (secondAngle != null) {
                val secondHandLength = radius * 0.92f
                val secondHandTail = radius * 0.15f
                val secondHandWidth = 1.5.dp.toPx()

                val tipOffset = angleToOffset(center, secondHandLength, secondAngle)
                val tailOffset = angleToOffset(center, secondHandTail, (secondAngle + 180f) % 360f)

                drawLine(
                    color = secondHandColor,
                    start = tailOffset,
                    end = tipOffset,
                    strokeWidth = secondHandWidth,
                    cap = StrokeCap.Round
                )
            }

            // 6. Center Pivot Cap
            val capRadius = 4.5.dp.toPx()
            drawCircle(
                color = pivotCapColor,
                radius = capRadius,
                center = center
            )
            // Center pin detail
            drawCircle(
                color = StandByBackground,
                radius = 1.5.dp.toPx(),
                center = center
            )
        }
    }
}

/**
 * Convenience overload calculating angles directly from hours, minutes, and optional seconds.
 */
@Composable
fun AnalogClockDialView(
    modifier: Modifier = Modifier,
    hours: Int,
    minutes: Int,
    seconds: Int? = null,
    showSeconds: Boolean = true,
    accentColor: Color = StandByAccentOrange,
    isNightMode: Boolean = false
) {
    val angles = AnalogClockCalculator.calculateAngles(
        hours = hours,
        minutes = minutes,
        seconds = seconds,
        showSeconds = showSeconds
    )

    AnalogClockDialView(
        hourAngle = angles.hourAngle,
        minuteAngle = angles.minuteAngle,
        secondAngle = angles.secondAngle,
        accentColor = accentColor,
        isNightMode = isNightMode,
        modifier = modifier
    )
}

/**
 * Projects a distance along a clock angle (where 0° is 12 o'clock / straight up).
 */
private fun angleToOffset(center: Offset, radius: Float, angleDegrees: Float): Offset {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val x = center.x + radius * sin(radians).toFloat()
    val y = center.y - radius * cos(radians).toFloat()
    return Offset(x, y)
}
