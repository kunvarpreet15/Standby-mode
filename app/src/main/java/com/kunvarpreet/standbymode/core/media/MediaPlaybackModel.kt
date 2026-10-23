package com.kunvarpreet.standbymode.core.media

import android.graphics.Bitmap

/**
 * Access permission state for Android Notification Listener Service.
 */
enum class MediaAccessState {
    PERMISSION_GRANTED,
    PERMISSION_REQUIRED
}

/**
 * Immutable representation of active media session playback state and metadata.
 */
data class MediaPlaybackInfo(
    val title: String? = null,
    val artist: String? = null,
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val canPlayPause: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSeek: Boolean = false,
    val durationMillis: Long = 0L,
    val positionMillis: Long = 0L,
    val lastPositionUpdateTime: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val packageName: String? = null
) {
    val displayTitle: String
        get() = if (title.isNullOrBlank()) "Unknown track" else title

    val displayArtist: String
        get() = if (artist.isNullOrBlank()) "Unknown artist" else artist

    val hasValidProgress: Boolean
        get() = durationMillis > 0L && positionMillis >= 0L
}

/**
 * Distinct presentation states for MediaMiniWidget.
 */
sealed interface MediaWidgetUiState {
    /**
     * User has not granted Notification Listener access in Android Settings.
     */
    data object UnavailableAccess : MediaWidgetUiState

    /**
     * Notification Listener access is granted, but no compatible media session is playing or paused.
     */
    data object NoActiveMedia : MediaWidgetUiState

    /**
     * An active media session was selected with current playback metadata.
     */
    data class ActiveSession(
        val info: MediaPlaybackInfo
    ) : MediaWidgetUiState
}
