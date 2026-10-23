package com.kunvarpreet.standbymode.ui.duo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kunvarpreet.standbymode.core.widget.DuoWidgetType

/**
 * Reusable vertical widget stack container for Duo Layout panes.
 *
 * Hosts a native VerticalPager allowing vertical swipe navigation between
 * available widgets in this pane, without affecting the other pane or horizontal paging.
 */
@Composable
fun DuoWidgetStackView(
    availableWidgets: List<DuoWidgetType>,
    selectedWidget: DuoWidgetType,
    onWidgetSelected: (DuoWidgetType) -> Unit,
    paneLabel: String,
    accentColor: Color,
    isNightMode: Boolean,
    modifier: Modifier = Modifier,
    indicatorAlignment: Alignment = Alignment.CenterEnd,
    content: @Composable (DuoWidgetType) -> Unit
) {
    if (availableWidgets.isEmpty()) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    val initialIndex = availableWidgets.indexOf(selectedWidget).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialIndex
    ) {
        availableWidgets.size
    }

    // Synchronize pager when selectedWidget is changed externally (e.g. settings fallback)
    val targetIndex = availableWidgets.indexOf(selectedWidget).coerceAtLeast(0)
    LaunchedEffect(targetIndex, availableWidgets) {
        if (pagerState.currentPage != targetIndex && targetIndex in availableWidgets.indices) {
            pagerState.scrollToPage(targetIndex)
        }
    }

    // Notify caller when user finishes a vertical swipe gesture to a new widget
    LaunchedEffect(pagerState.currentPage, availableWidgets) {
        if (pagerState.currentPage in availableWidgets.indices) {
            val newlySelected = availableWidgets[pagerState.currentPage]
            if (newlySelected != selectedWidget) {
                onWidgetSelected(newlySelected)
            }
        }
    }

    val currentWidget = availableWidgets.getOrElse(pagerState.currentPage) { selectedWidget }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "$paneLabel stack displaying ${currentWidget.displayName}"
            }
    ) {
        // Vertical Pager: finite, bounded navigation between available widgets
        VerticalPager(
            state = pagerState,
            userScrollEnabled = availableWidgets.size > 1,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val widgetType = availableWidgets[pageIndex]
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                content(widgetType)
            }
        }

        // Unobtrusive Vertical Stack Affordance Indicator
        VerticalStackIndicator(
            count = availableWidgets.size,
            currentIndex = pagerState.currentPage,
            accentColor = accentColor,
            isNightMode = isNightMode,
            paneLabel = paneLabel,
            currentWidgetName = currentWidget.displayName,
            modifier = Modifier
                .align(indicatorAlignment)
                .padding(end = 4.dp)
        )
    }
}
