package com.kunvarpreet.standbymode.ui.media

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.R
import com.kunvarpreet.standbymode.core.media.MediaPlaybackInfo
import com.kunvarpreet.standbymode.core.media.MediaSessionPolicy
import com.kunvarpreet.standbymode.core.media.MediaWidgetUiState
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

/**
 * Compact ambient media control widget designed for StandBy bedside clock display.
 * Displays artwork, track title, artist, transport controls (prev, play/pause, next),
 * and compact playback progress with seeking for seekable sessions.
 */
@Composable
fun MediaMiniWidget(
    modifier: Modifier = Modifier,
    uiState: MediaWidgetUiState,
    isNightMode: Boolean,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit = {},
    onOpenPermissionSettings: () -> Unit = {}
) {
    val surfaceColor = if (isNightMode) StandByNightSurface else StandBySurfaceCard
    val borderColor = if (isNightMode) StandByNightSurface else StandByBorder
    val primaryTextColor = if (isNightMode) StandByNightText else StandByTextPrimary
    val mutedTextColor = if (isNightMode) StandByNightTextMuted else StandByTextMuted
    val activeAccentColor = if (isNightMode) StandByNightText else accentColor

    val hasValidProgress = uiState is MediaWidgetUiState.ActiveSession && uiState.info.hasValidProgress
    val widgetHeight = if (hasValidProgress) 76.dp else 60.dp

    Surface(
        modifier = modifier
            .widthIn(min = 280.dp, max = 380.dp)
            .height(widgetHeight),
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        when (uiState) {
            is MediaWidgetUiState.UnavailableAccess -> {
                UnavailableAccessState(
                    primaryText = primaryTextColor,
                    mutedText = mutedTextColor,
                    accentColor = activeAccentColor,
                    onOpenSettings = onOpenPermissionSettings
                )
            }
            is MediaWidgetUiState.NoActiveMedia -> {
                NoActiveMediaState(
                    primaryText = primaryTextColor,
                    mutedText = mutedTextColor
                )
            }
            is MediaWidgetUiState.ActiveSession -> {
                ActiveSessionControls(
                    info = uiState.info,
                    primaryText = primaryTextColor,
                    mutedText = mutedTextColor,
                    accentColor = activeAccentColor,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeek = onSeek
                )
            }
        }
    }
}

@Composable
private fun ActiveSessionControls(
    info: MediaPlaybackInfo,
    primaryText: Color,
    mutedText: Color,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = if (info.hasValidProgress) 5.dp else 6.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Top Row: Album Artwork + Metadata + Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtwork(
                bitmap = info.albumArt,
                tint = mutedText,
                modifier = Modifier
                    .size(if (info.hasValidProgress) 36.dp else 44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = info.displayTitle,
                    color = primaryText,
                    fontSize = if (info.hasValidProgress) 12.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = info.displayArtist,
                    color = mutedText,
                    fontSize = if (info.hasValidProgress) 10.sp else 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Transport Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val buttonSize = if (info.hasValidProgress) 36.dp else 44.dp
                val iconSize = if (info.hasValidProgress) 18.dp else 20.dp
                val playIconSize = if (info.hasValidProgress) 20.dp else 22.dp

                // Previous Track Button
                val prevAlpha = if (info.canSkipPrevious) 1f else 0.35f
                IconButton(
                    onClick = onPrevious,
                    enabled = info.canSkipPrevious,
                    modifier = Modifier
                        .size(buttonSize)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Previous track"
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_previous),
                        contentDescription = null,
                        tint = primaryText.copy(alpha = prevAlpha),
                        modifier = Modifier.size(iconSize)
                    )
                }

                // Play / Pause Button
                val playPauseAlpha = if (info.canPlayPause) 1f else 0.35f
                val isPlaying = info.isPlaying
                val playPauseIconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                val playPauseDesc = if (isPlaying) "Pause" else "Play"

                IconButton(
                    onClick = onPlayPause,
                    enabled = info.canPlayPause,
                    modifier = Modifier
                        .size(buttonSize)
                        .semantics {
                            role = Role.Button
                            contentDescription = playPauseDesc
                        }
                ) {
                    Icon(
                        painter = painterResource(id = playPauseIconRes),
                        contentDescription = null,
                        tint = accentColor.copy(alpha = playPauseAlpha),
                        modifier = Modifier.size(playIconSize)
                    )
                }

                // Next Track Button
                val nextAlpha = if (info.canSkipNext) 1f else 0.35f
                IconButton(
                    onClick = onNext,
                    enabled = info.canSkipNext,
                    modifier = Modifier
                        .size(buttonSize)
                        .semantics {
                            role = Role.Button
                            contentDescription = "Next track"
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_skip_next),
                        contentDescription = null,
                        tint = primaryText.copy(alpha = nextAlpha),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        // Bottom Row: Playback Progress (only if valid progress is present)
        if (info.hasValidProgress) {
            Spacer(modifier = Modifier.height(3.dp))
            MediaProgressRow(
                info = info,
                primaryText = primaryText,
                mutedText = mutedText,
                accentColor = accentColor,
                onSeek = onSeek
            )
        }
    }
}

@Composable
private fun MediaProgressRow(
    info: MediaPlaybackInfo,
    primaryText: Color,
    mutedText: Color,
    accentColor: Color,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgressFraction by remember { mutableFloatStateOf(0f) }

    // Position estimation state
    var estimatedPositionMillis by remember(info.positionMillis, info.lastPositionUpdateTime, info.isPlaying) {
        mutableLongStateOf(
            MediaSessionPolicy.estimateCurrentPosition(
                positionMillis = info.positionMillis,
                lastPositionUpdateTime = info.lastPositionUpdateTime,
                playbackSpeed = info.playbackSpeed,
                isPlaying = info.isPlaying,
                durationMillis = info.durationMillis,
                currentElapsedRealtime = SystemClock.elapsedRealtime()
            )
        )
    }

    // Lifecycle-safe UI ticker running strictly while playing with valid duration
    LaunchedEffect(info.isPlaying, info.positionMillis, info.lastPositionUpdateTime, info.playbackSpeed, info.durationMillis) {
        if (info.isPlaying && info.durationMillis > 0L) {
            while (isActive) {
                estimatedPositionMillis = MediaSessionPolicy.estimateCurrentPosition(
                    positionMillis = info.positionMillis,
                    lastPositionUpdateTime = info.lastPositionUpdateTime,
                    playbackSpeed = info.playbackSpeed,
                    isPlaying = info.isPlaying,
                    durationMillis = info.durationMillis,
                    currentElapsedRealtime = SystemClock.elapsedRealtime()
                )
                delay(500.milliseconds)
            }
        } else {
            estimatedPositionMillis = info.positionMillis.coerceIn(0L, info.durationMillis.coerceAtLeast(0L))
        }
    }

    val currentMillis = if (isDragging) {
        (dragProgressFraction * info.durationMillis).toLong().coerceIn(0L, info.durationMillis)
    } else {
        estimatedPositionMillis
    }

    val currentFraction = if (info.durationMillis > 0L) {
        (currentMillis.toFloat() / info.durationMillis.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val elapsedText = MediaSessionPolicy.formatDuration(currentMillis)
    val totalDurationText = MediaSessionPolicy.formatDuration(info.durationMillis)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = elapsedText,
            color = mutedText,
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.widthIn(min = 26.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (info.canSeek) {
                InteractiveProgressBar(
                    progressFraction = currentFraction,
                    accentColor = accentColor,
                    trackColor = primaryText.copy(alpha = 0.2f),
                    onDragStart = { fraction ->
                        isDragging = true
                        dragProgressFraction = fraction
                    },
                    onDragChange = { fraction ->
                        dragProgressFraction = fraction
                    },
                    onDragEnd = {
                        val targetMillis = (dragProgressFraction * info.durationMillis).toLong()
                        onSeek(MediaSessionPolicy.clampSeekPosition(targetMillis, info.durationMillis))
                        isDragging = false
                    }
                )
            } else {
                NonInteractiveProgressBar(
                    progressFraction = currentFraction,
                    accentColor = accentColor,
                    trackColor = primaryText.copy(alpha = 0.2f)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = totalDurationText,
            color = mutedText,
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.widthIn(min = 26.dp)
        )
    }
}

@Composable
private fun InteractiveProgressBar(
    progressFraction: Float,
    accentColor: Color,
    trackColor: Color,
    onDragStart: (Float) -> Unit,
    onDragChange: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableFloatStateOf(1f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .semantics {
                role = Role.Button
                contentDescription = "Playback scrubber"
                progressBarRangeInfo = ProgressBarRangeInfo(progressFraction, 0f..1f)
            }
            .pointerInput(widthPx) {
                detectTapGestures(
                    onPress = { offset ->
                        val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
                        onDragStart(fraction)
                        val released = tryAwaitRelease()
                        if (released) {
                            onDragEnd()
                        }
                    }
                )
            }
            .pointerInput(widthPx) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
                        onDragStart(fraction)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                        onDragChange(fraction)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
    ) {
        val centerY = size.height / 2f
        val trackHeight = 3.dp.toPx()
        val thumbRadius = 4.5.dp.toPx()

        // Inactive Track
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )

        // Active Track
        val activeEnd = (size.width * progressFraction).coerceIn(0f, size.width)
        if (activeEnd > 0f) {
            drawLine(
                color = accentColor,
                start = Offset(0f, centerY),
                end = Offset(activeEnd, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
        }

        // Scrubbing Thumb
        drawCircle(
            color = accentColor,
            radius = thumbRadius,
            center = Offset(activeEnd, centerY)
        )
    }
}

@Composable
private fun NonInteractiveProgressBar(
    progressFraction: Float,
    accentColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progressFraction, 0f..1f)
            }
    ) {
        val centerY = size.height / 2f
        val trackHeight = 3.dp.toPx()

        // Inactive Track
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )

        // Active Track
        val activeEnd = (size.width * progressFraction).coerceIn(0f, size.width)
        if (activeEnd > 0f) {
            drawLine(
                color = accentColor,
                start = Offset(0f, centerY),
                end = Offset(activeEnd, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun AlbumArtwork(
    bitmap: Bitmap?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Album artwork",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = "No album artwork",
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun NoActiveMediaState(
    primaryText: Color,
    mutedText: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = null,
                tint = mutedText,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "No media playing",
                color = primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Play audio in a compatible app to control it here",
                color = mutedText,
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UnavailableAccessState(
    primaryText: Color,
    mutedText: Color,
    accentColor: Color,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_music_note),
                contentDescription = null,
                tint = mutedText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Media access required",
                    color = primaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Grant notification listener access",
                    color = mutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = "Settings",
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
