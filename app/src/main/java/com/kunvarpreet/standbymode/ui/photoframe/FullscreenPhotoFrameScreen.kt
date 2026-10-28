package com.kunvarpreet.standbymode.ui.photoframe

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.core.photoframe.PhotoFrameLogic
import com.kunvarpreet.standbymode.core.time.FormattedTime
import com.kunvarpreet.standbymode.ui.theme.StandByBackground
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

/**
 * Dedicated fullscreen ambient photo frame slideshow experience.
 * Displays user-selected photographs across the full content area with
 * on-device face-aware smart cropping, smooth 400ms crossfade transitions,
 * 15-second slideshow intervals, and minimal ambient overlays (exit button,
 * subtle corner clock pill, and photo counter).
 */
@Composable
fun FullscreenPhotoFrameScreen(
    modifier: Modifier = Modifier,
    photoUris: List<String>,
    isSlideshowActive: Boolean,
    isNightMode: Boolean,
    accentColor: Color,
    formattedTime: FormattedTime,
    showDate: Boolean = true,
    onBack: () -> Unit,
    onChoosePhotos: () -> Unit
) {
    // Intercept hardware and gesture back navigation
    BackHandler(onBack = onBack)

    if (photoUris.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(StandByBackground)
                .safeDrawingPadding()
        ) {
            PhotoFrameEmptyState(
                isNightMode = isNightMode,
                accentColor = accentColor,
                onChoosePhotos = onChoosePhotos,
                modifier = Modifier.fillMaxSize()
            )

            // Minimal Exit Button
            ExitFullscreenButton(
                isNightMode = isNightMode,
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
        return
    }

    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    val validCount = photoUris.size

    val clampedIndex = currentIndex.coerceIn(0, validCount - 1)
    if (clampedIndex != currentIndex) {
        currentIndex = clampedIndex
    }

    // Automatically advance slideshow every 15 seconds when active
    LaunchedEffect(isSlideshowActive, validCount) {
        if (isSlideshowActive && validCount > 1) {
            while (isActive) {
                delay(PhotoFrameLogic.SLIDESHOW_INTERVAL_MILLIS.milliseconds)
                currentIndex = PhotoFrameLogic.nextIndex(currentIndex, validCount)
            }
        }
    }

    val currentUriString = photoUris[currentIndex]

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StandByBackground)
            .semantics {
                contentDescription = "Fullscreen photo frame slideshow, photo ${currentIndex + 1} of $validCount"
            }
    ) {
        // Fullscreen Photo Slideshow with 400ms crossfade
        Crossfade(
            targetState = currentUriString,
            animationSpec = tween(durationMillis = 400),
            label = "fullscreen_photo_crossfade"
        ) { uriString ->
            PhotoItemView(
                uriString = uriString,
                photoIndex = currentIndex,
                totalPhotos = validCount,
                isNightMode = isNightMode,
                accentColor = accentColor,
                onManagePhotos = onChoosePhotos,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay Container adhering to safe drawing padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            // Top-Start: Exit Fullscreen Button
            ExitFullscreenButton(
                isNightMode = isNightMode,
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )

            // Top-End: Ambient Clock / Date Pill
            AmbientClockPill(
                formattedTime = formattedTime,
                showDate = showDate,
                isNightMode = isNightMode,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )

            // Bottom-Center: Subtle Photo Counter Pill
            if (validCount > 1) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isNightMode) StandByNightSurface else Color(0x66000000),
                    border = BorderStroke(
                        1.dp,
                        if (isNightMode) StandByNightTextMuted.copy(alpha = 0.25f) else Color(0x33FFFFFF)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / $validCount",
                        color = if (isNightMode) StandByNightText else StandByTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitFullscreenButton(
    isNightMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isNightMode) StandByNightSurface else Color(0x66000000),
        border = BorderStroke(
            1.dp,
            if (isNightMode) StandByNightTextMuted.copy(alpha = 0.25f) else Color(0x33FFFFFF)
        ),
        modifier = modifier
            .size(44.dp)
            .semantics {
                contentDescription = "Exit fullscreen photo frame"
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = if (isNightMode) StandByNightText else StandByTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AmbientClockPill(
    formattedTime: FormattedTime,
    showDate: Boolean,
    isNightMode: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isNightMode) StandByNightSurface else Color(0x66000000),
        border = BorderStroke(
            1.dp,
            if (isNightMode) StandByNightTextMuted.copy(alpha = 0.25f) else Color(0x33FFFFFF)
        ),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            val timeText = "${formattedTime.hours}:${formattedTime.minutes}${if (formattedTime.amPm != null) " ${formattedTime.amPm}" else ""}"
            Text(
                text = timeText,
                color = if (isNightMode) StandByNightText else StandByTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
            if (showDate && formattedTime.formattedDate.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•  ${formattedTime.formattedDate}",
                    color = if (isNightMode) StandByNightTextMuted else Color(0xCCFFFFFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}
