package com.kunvarpreet.standbymode.core.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lifecycle-aware repository managing Android MediaSession discovery,
 * active session selection, callback registration, and transport actions.
 */
class MediaSessionRepository(private val context: Context) {

    private val mediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val componentName =
        ComponentName(context, MediaNotificationListenerService::class.java)

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _uiStateFlow = MutableStateFlow<MediaWidgetUiState>(MediaWidgetUiState.NoActiveMedia)
    val uiStateFlow: StateFlow<MediaWidgetUiState> = _uiStateFlow.asStateFlow()

    private var selectedController: MediaController? = null
    private var isObserving = false

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updatePlaybackInfo()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updatePlaybackInfo()
        }

        override fun onSessionDestroyed() {
            mainHandler.post {
                handleSessionDestroyed()
            }
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            mainHandler.post {
                handleActiveSessionsChanged(controllers ?: emptyList())
            }
        }

    /**
     * Checks if notification listener access is currently granted.
     */
    fun hasAccess(): Boolean {
        return MediaSessionPolicy.isNotificationListenerGranted(context)
    }

    /**
     * Re-checks permission access and immediately refreshes sessions if granted.
     */
    fun refreshAccess() {
        if (!hasAccess()) {
            if (isObserving) {
                stopObserving()
            } else {
                detachCurrentController()
            }
            _uiStateFlow.value = MediaWidgetUiState.UnavailableAccess
            return
        }
        if (isObserving) {
            refreshActiveSessions()
        } else {
            // When not actively observing, just clear unavailable status
            _uiStateFlow.value = MediaWidgetUiState.NoActiveMedia
        }
    }

    /**
     * Starts active observation of media sessions. Call when media controls are visible/needed.
     */
    fun startObserving() {
        if (isObserving) return

        if (!hasAccess()) {
            detachCurrentController()
            _uiStateFlow.value = MediaWidgetUiState.UnavailableAccess
            return
        }

        isObserving = true
        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(
                sessionsChangedListener,
                componentName,
                mainHandler
            )
            refreshActiveSessions()
        } catch (e: SecurityException) {
            Log.w("MediaSessionRepo", "SecurityException adding active sessions listener", e)
            _uiStateFlow.value = MediaWidgetUiState.UnavailableAccess
            isObserving = false
        }
    }

    /**
     * Stops session observation and unregisters callbacks. Call when screen is hidden or feature disabled.
     */
    fun stopObserving() {
        if (!isObserving) return
        isObserving = false

        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            Log.w("MediaSessionRepo", "Error removing active sessions listener", e)
        }

        detachCurrentController()
    }

    private fun refreshActiveSessions() {
        if (!hasAccess()) {
            detachCurrentController()
            _uiStateFlow.value = MediaWidgetUiState.UnavailableAccess
            return
        }

        val controllers = try {
            mediaSessionManager?.getActiveSessions(componentName) ?: emptyList()
        } catch (e: SecurityException) {
            Log.w("MediaSessionRepo", "SecurityException getting active sessions", e)
            detachCurrentController()
            _uiStateFlow.value = MediaWidgetUiState.UnavailableAccess
            return
        } catch (e: Exception) {
            Log.e("MediaSessionRepo", "Unexpected error querying active sessions", e)
            emptyList()
        }

        handleActiveSessionsChanged(controllers)
    }

    private fun handleActiveSessionsChanged(controllers: List<MediaController>) {
        val candidates = controllers.map { controller ->
            val playbackState = try {
                controller.playbackState
            } catch (_: Exception) {
                null
            }
            val state = playbackState?.state ?: PlaybackState.STATE_NONE
            val isPlaying = state == PlaybackState.STATE_PLAYING
            val isPaused = state == PlaybackState.STATE_PAUSED

            val metadata = try {
                controller.metadata
            } catch (_: Exception) {
                null
            }
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.description?.title?.toString()
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.description?.subtitle?.toString()
            val hasMetadata = !title.isNullOrBlank() || !artist.isNullOrBlank() ||
                    metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) != null ||
                    metadata?.description?.iconBitmap != null

            SessionCandidate(
                sessionTokenId = controller.sessionToken,
                packageName = controller.packageName ?: "",
                isPlaying = isPlaying,
                isPaused = isPaused,
                hasMetadata = hasMetadata
            )
        }

        val currentSessionId = selectedController?.sessionToken
        val bestCandidate = MediaSessionPolicy.selectBestCandidate(
            candidates = candidates,
            currentSessionId = currentSessionId,
            ownPackageName = context.packageName
        )

        if (bestCandidate == null) {
            detachCurrentController()
            _uiStateFlow.value = MediaWidgetUiState.NoActiveMedia
            return
        }

        // If the best candidate is already selected, simply refresh its metadata
        if (bestCandidate.sessionTokenId == currentSessionId && selectedController != null) {
            updatePlaybackInfo()
            return
        }

        // Otherwise, switch to the new best controller
        val newController = controllers.find { it.sessionToken == bestCandidate.sessionTokenId }
        attachController(newController)
    }

    private fun handleSessionDestroyed() {
        detachCurrentController()
        if (isObserving) {
            refreshActiveSessions()
        } else {
            _uiStateFlow.value = MediaWidgetUiState.NoActiveMedia
        }
    }

    private fun attachController(controller: MediaController?) {
        detachCurrentController()
        if (controller == null) {
            _uiStateFlow.value = MediaWidgetUiState.NoActiveMedia
            return
        }

        selectedController = controller
        try {
            controller.registerCallback(controllerCallback, mainHandler)
        } catch (e: Exception) {
            Log.w("MediaSessionRepo", "Failed to register controller callback", e)
        }

        updatePlaybackInfo()
    }

    private fun detachCurrentController() {
        val controller = selectedController ?: return
        selectedController = null
        try {
            controller.unregisterCallback(controllerCallback)
        } catch (e: Exception) {
            Log.w("MediaSessionRepo", "Failed to unregister controller callback", e)
        }
    }

    private fun updatePlaybackInfo() {
        val controller = selectedController
        if (controller == null) {
            val accessState = if (hasAccess()) MediaAccessState.PERMISSION_GRANTED else MediaAccessState.PERMISSION_REQUIRED
            _uiStateFlow.value = MediaSessionPolicy.resolveWidgetUiState(accessState, null)
            return
        }

        val rawData = MediaSessionPolicy.extractRawData(controller)
        val playbackInfo = MediaSessionPolicy.mapPlaybackInfo(rawData)
        val accessState = if (hasAccess()) MediaAccessState.PERMISSION_GRANTED else MediaAccessState.PERMISSION_REQUIRED
        _uiStateFlow.value = MediaSessionPolicy.resolveWidgetUiState(accessState, playbackInfo)
    }

    // Transport Actions

    fun playPause() {
        try {
            val controller = selectedController ?: return
            val playbackState = controller.playbackState ?: return
            val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING
            val (canPlayPause, _, _) = MediaSessionPolicy.resolveActions(isPlaying, playbackState.actions)
            if (!canPlayPause) {
                Log.d("MediaSessionRepo", "playPause ignored: action not supported by session")
                return
            }
            if (isPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        } catch (e: Exception) {
            Log.e("MediaSessionRepo", "Error executing play/pause transport control", e)
            handleSessionDestroyed()
        }
    }

    fun skipToPrevious() {
        try {
            val controller = selectedController ?: return
            val playbackState = controller.playbackState ?: return
            val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING
            val (_, canSkipPrevious, _) = MediaSessionPolicy.resolveActions(isPlaying, playbackState.actions)
            if (!canSkipPrevious) {
                Log.d("MediaSessionRepo", "skipToPrevious ignored: action not supported by session")
                return
            }
            controller.transportControls.skipToPrevious()
        } catch (e: Exception) {
            Log.e("MediaSessionRepo", "Error executing skipToPrevious transport control", e)
            handleSessionDestroyed()
        }
    }

    fun skipToNext() {
        try {
            val controller = selectedController ?: return
            val playbackState = controller.playbackState ?: return
            val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING
            val (_, _, canSkipNext) = MediaSessionPolicy.resolveActions(isPlaying, playbackState.actions)
            if (!canSkipNext) {
                Log.d("MediaSessionRepo", "skipToNext ignored: action not supported by session")
                return
            }
            controller.transportControls.skipToNext()
        } catch (e: Exception) {
            Log.e("MediaSessionRepo", "Error executing skipToNext transport control", e)
            handleSessionDestroyed()
        }
    }
}
