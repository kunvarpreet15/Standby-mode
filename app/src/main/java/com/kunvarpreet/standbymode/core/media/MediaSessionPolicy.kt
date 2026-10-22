package com.kunvarpreet.standbymode.core.media

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState

/**
 * Standard media playback actions bitmask matching android.media.session.PlaybackState.
 */
object PlaybackActions {
    const val ACTION_STOP: Long = 1L shl 0
    const val ACTION_PAUSE: Long = 1L shl 1
    const val ACTION_PLAY: Long = 1L shl 2
    const val ACTION_REWIND: Long = 1L shl 3
    const val ACTION_SKIP_TO_PREVIOUS: Long = 1L shl 4
    const val ACTION_SKIP_TO_NEXT: Long = 1L shl 5
    const val ACTION_FAST_FORWARD: Long = 1L shl 6
    const val ACTION_SET_RATING: Long = 1L shl 7
    const val ACTION_SEEK_TO: Long = 1L shl 8
    const val ACTION_PLAY_PAUSE: Long = 1L shl 9
}

/**
 * Summary representation of a media session candidate for deterministic selection.
 */
data class SessionCandidate(
    val sessionTokenId: Any,
    val packageName: String,
    val isPlaying: Boolean,
    val isPaused: Boolean = false,
    val hasMetadata: Boolean = true
)

/**
 * Raw data extracted from platform media controller and metadata.
 */
data class RawPlaybackData(
    val title: String? = null,
    val artist: String? = null,
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val actionsBitmask: Long = 0L,
    val packageName: String? = null
)

/**
 * Pure policy rules for media session selection, metadata mapping, and access verification.
 */
object MediaSessionPolicy {

    /**
     * Checks whether Notification Listener access has been granted for StandBy Mode.
     */
    fun isNotificationListenerGranted(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        val componentName = ComponentName(context, MediaNotificationListenerService::class.java)
        return try {
            notificationManager.isNotificationListenerAccessGranted(componentName)
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Deterministically selects the best candidate session from active sessions:
     * 1. Filters out StandBy Mode's own package to prevent self-selection.
     * 2. Filters out idle/stopped sessions that have no playback and no metadata.
     * 3. If current session is actively playing, keep it stable.
     * 4. If an actively playing candidate exists, switch to it (first in priority order).
     * 5. If current session is still present (even paused), retain it to avoid jumping.
     * 6. Otherwise, pick the first candidate in Android's priority list.
     */
    fun selectBestCandidate(
        candidates: List<SessionCandidate>,
        currentSessionId: Any?,
        ownPackageName: String
    ): SessionCandidate? {
        val validCandidates = candidates.filter {
            it.packageName != ownPackageName && (it.isPlaying || it.isPaused || it.hasMetadata)
        }
        if (validCandidates.isEmpty()) return null

        val currentCandidate = currentSessionId?.let { id ->
            validCandidates.find { it.sessionTokenId == id }
        }

        // 1. If currently selected session is playing, keep it stable
        if (currentCandidate != null && currentCandidate.isPlaying) {
            return currentCandidate
        }

        // 2. Prefer an actively playing compatible session
        val firstPlaying = validCandidates.find { it.isPlaying }
        if (firstPlaying != null) {
            return firstPlaying
        }

        // 3. Retain currently selected paused session if still valid
        if (currentCandidate != null) {
            return currentCandidate
        }

        // 4. Fallback to first available candidate in priority order
        return validCandidates.firstOrNull()
    }

    /**
     * Resolves action availability flags given playback state and actions bitmask.
     * Returns Triple(canPlayPause, canSkipPrevious, canSkipNext).
     */
    fun resolveActions(isPlaying: Boolean, actionsBitmask: Long): Triple<Boolean, Boolean, Boolean> {
        val canPlay = (actionsBitmask and PlaybackActions.ACTION_PLAY) != 0L
        val canPause = (actionsBitmask and PlaybackActions.ACTION_PAUSE) != 0L
        val canToggle = (actionsBitmask and PlaybackActions.ACTION_PLAY_PAUSE) != 0L

        val canPlayPause = canToggle || (isPlaying && canPause) || (!isPlaying && canPlay)
        val canSkipPrevious = (actionsBitmask and PlaybackActions.ACTION_SKIP_TO_PREVIOUS) != 0L
        val canSkipNext = (actionsBitmask and PlaybackActions.ACTION_SKIP_TO_NEXT) != 0L

        return Triple(canPlayPause, canSkipPrevious, canSkipNext)
    }

    /**
     * Maps raw playback data into immutable MediaPlaybackInfo.
     */
    fun mapPlaybackInfo(raw: RawPlaybackData?): MediaPlaybackInfo? {
        if (raw == null) return null
        val (canPlayPause, canSkipPrev, canSkipNext) = resolveActions(raw.isPlaying, raw.actionsBitmask)
        return MediaPlaybackInfo(
            title = raw.title?.takeIf { it.isNotBlank() },
            artist = raw.artist?.takeIf { it.isNotBlank() },
            albumArt = raw.albumArt,
            isPlaying = raw.isPlaying,
            canPlayPause = canPlayPause,
            canSkipPrevious = canSkipPrev,
            canSkipNext = canSkipNext,
            packageName = raw.packageName
        )
    }

    /**
     * Resolves the overall MediaWidgetUiState based on access status and playback metadata.
     */
    fun resolveWidgetUiState(
        accessState: MediaAccessState,
        playbackInfo: MediaPlaybackInfo?
    ): MediaWidgetUiState {
        return when {
            accessState == MediaAccessState.PERMISSION_REQUIRED -> MediaWidgetUiState.UnavailableAccess
            playbackInfo == null -> MediaWidgetUiState.NoActiveMedia
            else -> MediaWidgetUiState.ActiveSession(playbackInfo)
        }
    }

    /**
     * Safely extracts raw playback data from a platform MediaController.
     */
    fun extractRawData(controller: MediaController?): RawPlaybackData? {
        if (controller == null) return null
        return try {
            val metadata = controller.metadata
            val playbackState = controller.playbackState

            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.description?.title?.toString()
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: metadata?.description?.subtitle?.toString()
            val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.description?.iconBitmap

            val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
            val actions = playbackState?.actions ?: 0L

            RawPlaybackData(
                title = title,
                artist = artist,
                albumArt = albumArt,
                isPlaying = isPlaying,
                actionsBitmask = actions,
                packageName = controller.packageName
            )
        } catch (_: Exception) {
            null
        }
    }
}
