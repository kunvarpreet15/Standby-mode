package com.kunvarpreet.standbymode.ui.display

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kunvarpreet.standbymode.core.display.DisplaySpace
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted

/**
 * Subtle, accessible page dots indicator for StandBy display spaces.
 * Anchored to the bottom center of the display canvas.
 */
@Composable
fun PageDotsIndicator(
    modifier: Modifier = Modifier,
    pageCount: Int,
    currentPage: Int,
    accentColor: Color,
    isNightMode: Boolean,
    onPageSelected: ((Int) -> Unit)? = null
) {
    val activeColor = if (isNightMode) StandByNightText else accentColor
    val inactiveColor = if (isNightMode) {
        StandByNightTextMuted.copy(alpha = 0.35f)
    } else {
        StandByTextMuted.copy(alpha = 0.40f)
    }

    Row(
        modifier = modifier
            .semantics {
                contentDescription = "Display space ${currentPage + 1} of $pageCount: ${DisplaySpace.fromPageIndex(currentPage).title}"
            }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val dotColor by animateColorAsState(
                targetValue = if (isSelected) activeColor else inactiveColor,
                animationSpec = tween(durationMillis = 250),
                label = "page_dot_color_$index"
            )

            val interactionSource = remember { MutableInteractionSource() }
            val space = DisplaySpace.fromPageIndex(index)

            Box(
                modifier = Modifier
                    .size(if (onPageSelected != null) 24.dp else 6.dp)
                    .then(
                        if (onPageSelected != null) {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Tab,
                                onClick = { onPageSelected(index) }
                            )
                        } else {
                            Modifier
                        }
                    )
                    .semantics {
                        role = Role.Tab
                        contentDescription = "${space.title}, page ${index + 1} of $pageCount"
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}
