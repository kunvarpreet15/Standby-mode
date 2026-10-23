package com.kunvarpreet.standbymode.ui.duo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText

/**
 * Subtle vertical indicator for Duo Layout widget stacks.
 * Distinguishable from the horizontal 2-dot display space indicator.
 * Automatically hidden when the stack has only 1 available widget.
 */
@Composable
fun VerticalStackIndicator(
    count: Int,
    currentIndex: Int,
    accentColor: Color,
    isNightMode: Boolean,
    paneLabel: String,
    currentWidgetName: String,
    modifier: Modifier = Modifier
) {
    if (count <= 1) return

    val activeColor = if (isNightMode) StandByNightText else accentColor
    val inactiveColor = if (isNightMode) StandByNightSurface else Color(0xFF3A3A3C)

    val accessibilityDesc = "$paneLabel: $currentWidgetName, widget ${currentIndex + 1} of $count"

    Column(
        modifier = modifier
            .padding(horizontal = 6.dp)
            .semantics {
                contentDescription = accessibilityDesc
            },
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (index in 0 until count) {
            val isSelected = index == currentIndex

            val dotHeight by animateDpAsState(
                targetValue = if (isSelected) 14.dp else 5.dp,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "stack_indicator_height_$index"
            )

            val dotColor by animateColorAsState(
                targetValue = if (isSelected) activeColor else inactiveColor,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "stack_indicator_color_$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(dotHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(dotColor)
            )
        }
    }
}
