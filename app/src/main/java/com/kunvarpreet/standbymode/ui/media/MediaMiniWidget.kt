package com.kunvarpreet.standbymode.ui.media

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kunvarpreet.standbymode.R
import com.kunvarpreet.standbymode.core.media.MediaPlaybackInfo
import com.kunvarpreet.standbymode.core.media.MediaWidgetUiState
import com.kunvarpreet.standbymode.ui.theme.StandByBorder
import com.kunvarpreet.standbymode.ui.theme.StandByNightSurface
import com.kunvarpreet.standbymode.ui.theme.StandByNightText
import com.kunvarpreet.standbymode.ui.theme.StandByNightTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandBySurfaceCard
import com.kunvarpreet.standbymode.ui.theme.StandByTextMuted
import com.kunvarpreet.standbymode.ui.theme.StandByTextPrimary

/**
 * Compact ambient media control widget designed for StandBy bedside clock display.
 * Displays artwork, track title, artist, and transport controls (prev, play/pause, next).
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
    onOpenPermissionSettings: () -> Unit = {}
) {
    val surfaceColor = if (isNightMode) StandByNightSurface else StandBySurfaceCard
    val borderColor = if (isNightMode) StandByNightSurface else StandByBorder
    val primaryTextColor = if (isNightMode) StandByNightText else StandByTextPrimary
    val mutedTextColor = if (isNightMode) StandByNightTextMuted else StandByTextMuted
    val activeAccentColor = if (isNightMode) StandByNightText else accentColor

    Surface(
        modifier = modifier
            .widthIn(min = 280.dp, max = 380.dp)
            .height(60.dp),
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
                    onNext = onNext
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
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Artwork or generic fallback
        AlbumArtwork(
            bitmap = info.albumArt,
            tint = mutedText,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Title and Artist metadata
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = info.displayTitle,
                color = primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = info.displayArtist,
                color = mutedText,
                fontSize = 11.sp,
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
            // Previous Track Button
            val prevAlpha = if (info.canSkipPrevious) 1f else 0.35f
            IconButton(
                onClick = onPrevious,
                enabled = info.canSkipPrevious,
                modifier = Modifier
                    .size(44.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Previous track"
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_skip_previous),
                    contentDescription = null,
                    tint = primaryText.copy(alpha = prevAlpha),
                    modifier = Modifier.size(20.dp)
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
                    .size(44.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = playPauseDesc
                    }
            ) {
                Icon(
                    painter = painterResource(id = playPauseIconRes),
                    contentDescription = null,
                    tint = accentColor.copy(alpha = playPauseAlpha),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Next Track Button
            val nextAlpha = if (info.canSkipNext) 1f else 0.35f
            IconButton(
                onClick = onNext,
                enabled = info.canSkipNext,
                modifier = Modifier
                    .size(44.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Next track"
                    }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_skip_next),
                    contentDescription = null,
                    tint = primaryText.copy(alpha = nextAlpha),
                    modifier = Modifier.size(20.dp)
                )
            }
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
