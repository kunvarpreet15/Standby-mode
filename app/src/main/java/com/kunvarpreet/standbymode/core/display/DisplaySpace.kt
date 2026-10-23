package com.kunvarpreet.standbymode.core.display

/*
    Primary display spaces available in StandBy Mode.
    Page 0: [DUO_LAYOUT] - Split side-by-side widget panels (clock on left, secondary on right).
    Page 1: [FULLSCREEN_CLOCK] - Fullscreen immersive clock display.
*/
enum class DisplaySpace(val pageIndex: Int, val title: String) {
    DUO_LAYOUT(pageIndex = 0, title = "Duo Layout"),
    FULLSCREEN_CLOCK(pageIndex = 1, title = "Fullscreen Clock");

    companion object {
        const val DEFAULT_PAGE_INDEX = 0

        fun fromPageIndex(pageIndex: Int): DisplaySpace {
            return entries.firstOrNull { it.pageIndex == pageIndex } ?: DUO_LAYOUT
        }
    }
}
