package com.kunvarpreet.standbymode.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSessionPolicyTest {

    private val ownPackage = "com.kunvarpreet.standbymode"

    // 1. Session Candidate Selection Tests

    @Test
    fun selectBestCandidate_emptyList_returnsNull() {
        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = emptyList(),
            currentSessionId = null,
            ownPackageName = ownPackage
        )
        assertNull(result)
    }

    @Test
    fun selectBestCandidate_ignoresOwnPackageName() {
        val candidates = listOf(
            SessionCandidate(
                sessionTokenId = "token-own",
                packageName = ownPackage,
                isPlaying = true
            )
        )
        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = candidates,
            currentSessionId = null,
            ownPackageName = ownPackage
        )
        assertNull(result)
    }

    @Test
    fun selectBestCandidate_singleExternalCandidate_selected() {
        val candidate = SessionCandidate(
            sessionTokenId = "token-spotify",
            packageName = "com.spotify.music",
            isPlaying = false
        )
        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = listOf(candidate),
            currentSessionId = null,
            ownPackageName = ownPackage
        )
        assertEquals(candidate, result)
    }

    @Test
    fun selectBestCandidate_prefersActivelyPlayingOverPaused() {
        val paused = SessionCandidate("token-podcast", "com.google.podcasts", isPlaying = false)
        val playing = SessionCandidate("token-music", "com.google.music", isPlaying = true)

        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = listOf(paused, playing),
            currentSessionId = null,
            ownPackageName = ownPackage
        )
        assertEquals(playing, result)
    }

    @Test
    fun selectBestCandidate_preservesCurrentlySelectedSessionWhenPlaying() {
        val playing1 = SessionCandidate("token-1", "com.player.one", isPlaying = true)
        val playing2 = SessionCandidate("token-2", "com.player.two", isPlaying = true)

        // Currently selected is playing2, which is second in the list
        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = listOf(playing1, playing2),
            currentSessionId = "token-2",
            ownPackageName = ownPackage
        )
        // Maintains stability for current active player
        assertEquals(playing2, result)
    }

    @Test
    fun selectBestCandidate_switchesFromPausedToNewlyPlayingCandidate() {
        val currentPaused = SessionCandidate("token-1", "com.player.one", isPlaying = false)
        val newPlaying = SessionCandidate("token-2", "com.player.two", isPlaying = true)

        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = listOf(currentPaused, newPlaying),
            currentSessionId = "token-1",
            ownPackageName = ownPackage
        )
        assertEquals(newPlaying, result)
    }

    @Test
    fun selectBestCandidate_maintainsCurrentPausedWhenAllPaused() {
        val paused1 = SessionCandidate("token-1", "com.player.one", isPlaying = false)
        val paused2 = SessionCandidate("token-2", "com.player.two", isPlaying = false)

        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = listOf(paused1, paused2),
            currentSessionId = "token-2",
            ownPackageName = ownPackage
        )
        // Keeps token-2 for stability
        assertEquals(paused2, result)
    }

    @Test
    fun selectBestCandidate_tieBreakerUsesAndroidPriorityOrderWhenNoCurrent() {
        val paused1 = SessionCandidate("token-1", "com.player.one", isPlaying = false)
        val paused2 = SessionCandidate("token-2", "com.player.two", isPlaying = false)

        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = listOf(paused1, paused2),
            currentSessionId = null,
            ownPackageName = ownPackage
        )
        assertEquals(paused1, result)
    }

    // 2. Playback Actions Resolution Tests

    @Test
    fun resolveActions_emptyActions_allControlsDisabled() {
        val (canPlayPause, canSkipPrev, canSkipNext) = MediaSessionPolicy.resolveActions(
            isPlaying = false,
            actionsBitmask = 0L
        )
        assertFalse(canPlayPause)
        assertFalse(canSkipPrev)
        assertFalse(canSkipNext)
    }

    @Test
    fun resolveActions_playAction_enabledOnlyWhenPaused() {
        val actions = PlaybackActions.ACTION_PLAY

        val (canPlayWhenPaused, _, _) = MediaSessionPolicy.resolveActions(isPlaying = false, actionsBitmask = actions)
        assertTrue(canPlayWhenPaused)

        val (canPlayWhenPlaying, _, _) = MediaSessionPolicy.resolveActions(isPlaying = true, actionsBitmask = actions)
        assertFalse(canPlayWhenPlaying)
    }

    @Test
    fun resolveActions_pauseAction_enabledOnlyWhenPlaying() {
        val actions = PlaybackActions.ACTION_PAUSE

        val (canPauseWhenPlaying, _, _) = MediaSessionPolicy.resolveActions(isPlaying = true, actionsBitmask = actions)
        assertTrue(canPauseWhenPlaying)

        val (canPauseWhenPaused, _, _) = MediaSessionPolicy.resolveActions(isPlaying = false, actionsBitmask = actions)
        assertFalse(canPauseWhenPaused)
    }

    @Test
    fun resolveActions_toggleAction_enabledInBothStates() {
        val actions = PlaybackActions.ACTION_PLAY_PAUSE

        val (pausedToggle, _, _) = MediaSessionPolicy.resolveActions(isPlaying = false, actionsBitmask = actions)
        assertTrue(pausedToggle)

        val (playingToggle, _, _) = MediaSessionPolicy.resolveActions(isPlaying = true, actionsBitmask = actions)
        assertTrue(playingToggle)
    }

    @Test
    fun resolveActions_skipPreviousAndNext_independentFlags() {
        val prevOnly = PlaybackActions.ACTION_SKIP_TO_PREVIOUS
        val (prevPlay, canPrev, canNext1) = MediaSessionPolicy.resolveActions(isPlaying = false, actionsBitmask = prevOnly)
        assertFalse(prevPlay)
        assertTrue(canPrev)
        assertFalse(canNext1)

        val nextOnly = PlaybackActions.ACTION_SKIP_TO_NEXT
        val (_, canPrev2, canNext2) = MediaSessionPolicy.resolveActions(isPlaying = false, actionsBitmask = nextOnly)
        assertFalse(canPrev2)
        assertTrue(canNext2)

        val both = PlaybackActions.ACTION_SKIP_TO_PREVIOUS or PlaybackActions.ACTION_SKIP_TO_NEXT
        val (_, canPrevBoth, canNextBoth) = MediaSessionPolicy.resolveActions(isPlaying = false, actionsBitmask = both)
        assertTrue(canPrevBoth)
        assertTrue(canNextBoth)
    }

    // 3. Metadata Mapping & Fallback Tests

    @Test
    fun mapPlaybackInfo_nullRaw_returnsNull() {
        assertNull(MediaSessionPolicy.mapPlaybackInfo(null))
    }

    @Test
    fun mapPlaybackInfo_completeMetadata_mapsAccurately() {
        val raw = RawPlaybackData(
            title = "Midnight City",
            artist = "M83",
            albumArt = null,
            isPlaying = true,
            actionsBitmask = PlaybackActions.ACTION_PAUSE or PlaybackActions.ACTION_SKIP_TO_NEXT,
            packageName = "com.spotify.music"
        )
        val info = MediaSessionPolicy.mapPlaybackInfo(raw)
        assertNotNull(info)
        assertEquals("Midnight City", info?.title)
        assertEquals("Midnight City", info?.displayTitle)
        assertEquals("M83", info?.artist)
        assertEquals("M83", info?.displayArtist)
        assertNull(info?.albumArt)
        assertTrue(info?.isPlaying == true)
        assertTrue(info?.canPlayPause == true)
        assertFalse(info?.canSkipPrevious == true)
        assertTrue(info?.canSkipNext == true)
        assertEquals("com.spotify.music", info?.packageName)
    }

    @Test
    fun mapPlaybackInfo_missingTitleAndArtist_fallsBackToSensibleDefaults() {
        val raw = RawPlaybackData(
            title = "",
            artist = "   ",
            albumArt = null,
            isPlaying = false,
            actionsBitmask = PlaybackActions.ACTION_PLAY
        )
        val info = MediaSessionPolicy.mapPlaybackInfo(raw)
        assertNotNull(info)
        assertNull(info?.title)
        assertNull(info?.artist)
        assertEquals("Unknown track", info?.displayTitle)
        assertEquals("Unknown artist", info?.displayArtist)
    }

    // 4. Widget UI State Resolution Tests

    @Test
    fun resolveWidgetUiState_permissionRequired_returnsUnavailableAccess() {
        val state = MediaSessionPolicy.resolveWidgetUiState(
            accessState = MediaAccessState.PERMISSION_REQUIRED,
            playbackInfo = null
        )
        assertEquals(MediaWidgetUiState.UnavailableAccess, state)
    }

    @Test
    fun resolveWidgetUiState_grantedWithoutSession_returnsNoActiveMedia() {
        val state = MediaSessionPolicy.resolveWidgetUiState(
            accessState = MediaAccessState.PERMISSION_GRANTED,
            playbackInfo = null
        )
        assertEquals(MediaWidgetUiState.NoActiveMedia, state)
    }

    @Test
    fun resolveWidgetUiState_grantedWithSession_returnsActiveSession() {
        val info = MediaPlaybackInfo(title = "Song", isPlaying = true)
        val state = MediaSessionPolicy.resolveWidgetUiState(
            accessState = MediaAccessState.PERMISSION_GRANTED,
            playbackInfo = info
        )
        assertTrue(state is MediaWidgetUiState.ActiveSession)
        assertEquals(info, (state as MediaWidgetUiState.ActiveSession).info)
    }

    // 5. Idle / No-Media Session Candidate Filtering Tests

    @Test
    fun selectBestCandidate_filtersOutIdleCandidateWithNoPlaybackAndNoMetadata() {
        val idleAssistant = SessionCandidate(
            sessionTokenId = "token-assistant",
            packageName = "com.google.android.googlequicksearchbox",
            isPlaying = false,
            isPaused = false,
            hasMetadata = false
        )
        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = listOf(idleAssistant),
            currentSessionId = null,
            ownPackageName = ownPackage
        )
        assertNull(result)
    }

    @Test
    fun selectBestCandidate_acceptsPausedCandidateEvenWithoutMetadata() {
        val paused = SessionCandidate(
            sessionTokenId = "token-player",
            packageName = "com.spotify.music",
            isPlaying = false,
            isPaused = true,
            hasMetadata = false
        )
        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = listOf(paused),
            currentSessionId = null,
            ownPackageName = ownPackage
        )
        assertEquals(paused, result)
    }

    @Test
    fun selectBestCandidate_acceptsCandidateWithMetadataEvenWhenNotPlaying() {
        val loadedTrack = SessionCandidate(
            sessionTokenId = "token-player",
            packageName = "com.spotify.music",
            isPlaying = false,
            isPaused = false,
            hasMetadata = true
        )
        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = listOf(loadedTrack),
            currentSessionId = null,
            ownPackageName = ownPackage
        )
        assertEquals(loadedTrack, result)
    }

    @Test
    fun selectBestCandidate_prefersPlayingOverIdleWithoutMetadata() {
        val idle = SessionCandidate(
            sessionTokenId = "token-idle",
            packageName = "com.idle.app",
            isPlaying = false,
            isPaused = false,
            hasMetadata = false
        )
        val playing = SessionCandidate(
            sessionTokenId = "token-playing",
            packageName = "com.music.app",
            isPlaying = true,
            isPaused = false,
            hasMetadata = true
        )
        val result = MediaSessionPolicy.selectBestCandidate(
            candidates = listOf(idle, playing),
            currentSessionId = null,
            ownPackageName = ownPackage
        )
        assertEquals(playing, result)
    }

    // 4. Playback Progress Estimation Tests

    @Test
    fun estimateCurrentPosition_advancesWhilePlayingAtNormalSpeed() {
        val position = 10_000L // 10s
        val lastUpdate = 100_000L
        val currentElapsed = 105_000L // 5s elapsed
        val duration = 60_000L // 60s

        val estimated = MediaSessionPolicy.estimateCurrentPosition(
            positionMillis = position,
            lastPositionUpdateTime = lastUpdate,
            playbackSpeed = 1.0f,
            isPlaying = true,
            durationMillis = duration,
            currentElapsedRealtime = currentElapsed
        )
        assertEquals(15_000L, estimated)
    }

    @Test
    fun estimateCurrentPosition_maintainsStablePositionWhilePaused() {
        val position = 10_000L
        val lastUpdate = 100_000L
        val currentElapsed = 120_000L // 20s later, but paused!
        val duration = 60_000L

        val estimated = MediaSessionPolicy.estimateCurrentPosition(
            positionMillis = position,
            lastPositionUpdateTime = lastUpdate,
            playbackSpeed = 1.0f,
            isPlaying = false,
            durationMillis = duration,
            currentElapsedRealtime = currentElapsed
        )
        assertEquals(10_000L, estimated)
    }

    @Test
    fun estimateCurrentPosition_adjustsForPlaybackSpeed() {
        val position = 10_000L
        val lastUpdate = 100_000L
        val currentElapsed = 104_000L // 4s elapsed
        val duration = 60_000L

        // 1.5x speed: 4s elapsed = 6s audio advanced
        val estimated15x = MediaSessionPolicy.estimateCurrentPosition(
            positionMillis = position,
            lastPositionUpdateTime = lastUpdate,
            playbackSpeed = 1.5f,
            isPlaying = true,
            durationMillis = duration,
            currentElapsedRealtime = currentElapsed
        )
        assertEquals(16_000L, estimated15x)

        // 0.5x speed: 4s elapsed = 2s audio advanced
        val estimated05x = MediaSessionPolicy.estimateCurrentPosition(
            positionMillis = position,
            lastPositionUpdateTime = lastUpdate,
            playbackSpeed = 0.5f,
            isPlaying = true,
            durationMillis = duration,
            currentElapsedRealtime = currentElapsed
        )
        assertEquals(12_000L, estimated05x)

        // 0.0x speed: does not advance
        val estimated0x = MediaSessionPolicy.estimateCurrentPosition(
            positionMillis = position,
            lastPositionUpdateTime = lastUpdate,
            playbackSpeed = 0.0f,
            isPlaying = true,
            durationMillis = duration,
            currentElapsedRealtime = currentElapsed
        )
        assertEquals(10_000L, estimated0x)
    }

    @Test
    fun estimateCurrentPosition_handlesMissingOrInvalidDuration() {
        // Zero duration
        val zeroDuration = MediaSessionPolicy.estimateCurrentPosition(
            positionMillis = 5_000L,
            lastPositionUpdateTime = 100_000L,
            playbackSpeed = 1.0f,
            isPlaying = true,
            durationMillis = 0L,
            currentElapsedRealtime = 105_000L
        )
        assertEquals(0L, zeroDuration)

        // Negative duration
        val negativeDuration = MediaSessionPolicy.estimateCurrentPosition(
            positionMillis = 5_000L,
            lastPositionUpdateTime = 100_000L,
            playbackSpeed = 1.0f,
            isPlaying = true,
            durationMillis = -1L,
            currentElapsedRealtime = 105_000L
        )
        assertEquals(0L, negativeDuration)
    }

    @Test
    fun estimateCurrentPosition_handlesUnknownOrNegativePosition() {
        val unknownPosition = MediaSessionPolicy.estimateCurrentPosition(
            positionMillis = -1L, // PlaybackState.PLAYBACK_POSITION_UNKNOWN
            lastPositionUpdateTime = 100_000L,
            playbackSpeed = 1.0f,
            isPlaying = true,
            durationMillis = 60_000L,
            currentElapsedRealtime = 105_000L
        )
        assertEquals(0L, unknownPosition)
    }

    @Test
    fun estimateCurrentPosition_clampsAtTrackDuration() {
        val position = 58_000L
        val lastUpdate = 100_000L
        val currentElapsed = 110_000L // 10s elapsed -> would be 68_000L
        val duration = 60_000L

        val estimated = MediaSessionPolicy.estimateCurrentPosition(
            positionMillis = position,
            lastPositionUpdateTime = lastUpdate,
            playbackSpeed = 1.0f,
            isPlaying = true,
            durationMillis = duration,
            currentElapsedRealtime = currentElapsed
        )
        assertEquals(60_000L, estimated)
    }

    @Test
    fun estimateCurrentPosition_clampsAtZeroForNegativeCalculations() {
        val estimated = MediaSessionPolicy.estimateCurrentPosition(
            positionMillis = 0L,
            lastPositionUpdateTime = 0L,
            playbackSpeed = 1.0f,
            isPlaying = false,
            durationMillis = 60_000L,
            currentElapsedRealtime = 0L
        )
        assertEquals(0L, estimated)
    }

    // 5. Time Formatting Tests

    @Test
    fun formatDuration_formatsZeroAndNegative() {
        assertEquals("0:00", MediaSessionPolicy.formatDuration(0L))
        assertEquals("0:00", MediaSessionPolicy.formatDuration(-1000L))
    }

    @Test
    fun formatDuration_formatsSecondsWithZeroPadding() {
        assertEquals("0:07", MediaSessionPolicy.formatDuration(7_000L))
        assertEquals("0:45", MediaSessionPolicy.formatDuration(45_000L))
    }

    @Test
    fun formatDuration_formatsMinutesAndSecondsStandard() {
        assertEquals("3:07", MediaSessionPolicy.formatDuration(187_000L))
        assertEquals("42:18", MediaSessionPolicy.formatDuration(2_538_000L))
    }

    @Test
    fun formatDuration_formatsHoursMinutesSeconds() {
        assertEquals("1:02:18", MediaSessionPolicy.formatDuration(3_738_000L))
        assertEquals("10:00:00", MediaSessionPolicy.formatDuration(36_000_000L))
    }

    // 6. Seeking Capability and Clamping Tests

    @Test
    fun canSeek_returnsTrueWhenActionSeekToPresentWithValidDurationAndPosition() {
        val actions = PlaybackActions.ACTION_SEEK_TO or PlaybackActions.ACTION_PLAY
        val result = MediaSessionPolicy.canSeek(
            actionsBitmask = actions,
            durationMillis = 180_000L,
            positionMillis = 5_000L
        )
        assertTrue(result)
    }

    @Test
    fun canSeek_returnsFalseWhenActionSeekToMissing() {
        val actions = PlaybackActions.ACTION_PLAY or PlaybackActions.ACTION_PAUSE
        val result = MediaSessionPolicy.canSeek(
            actionsBitmask = actions,
            durationMillis = 180_000L,
            positionMillis = 5_000L
        )
        assertFalse(result)
    }

    @Test
    fun canSeek_returnsFalseWhenDurationIsZeroOrNegative() {
        val actions = PlaybackActions.ACTION_SEEK_TO
        assertFalse(MediaSessionPolicy.canSeek(actions, durationMillis = 0L, positionMillis = 0L))
        assertFalse(MediaSessionPolicy.canSeek(actions, durationMillis = -1L, positionMillis = 0L))
    }

    @Test
    fun canSeek_returnsFalseWhenPositionIsUnknownOrNegative() {
        val actions = PlaybackActions.ACTION_SEEK_TO
        assertFalse(MediaSessionPolicy.canSeek(actions, durationMillis = 60_000L, positionMillis = -1L))
    }

    @Test
    fun clampSeekPosition_clampsBetweenZeroAndDuration() {
        assertEquals(0L, MediaSessionPolicy.clampSeekPosition(-500L, 60_000L))
        assertEquals(25_000L, MediaSessionPolicy.clampSeekPosition(25_000L, 60_000L))
        assertEquals(60_000L, MediaSessionPolicy.clampSeekPosition(99_999L, 60_000L))
        assertEquals(0L, MediaSessionPolicy.clampSeekPosition(10_000L, 0L))
    }

    @Test
    fun mapPlaybackInfo_derivesProgressAndSeekCorrectly() {
        val raw = RawPlaybackData(
            title = "Midnight City",
            artist = "M83",
            isPlaying = true,
            actionsBitmask = PlaybackActions.ACTION_PLAY_PAUSE or PlaybackActions.ACTION_SEEK_TO,
            durationMillis = 243_000L,
            positionMillis = 30_000L,
            lastPositionUpdateTime = 123456L,
            playbackSpeed = 1.0f
        )
        val info = MediaSessionPolicy.mapPlaybackInfo(raw)
        assertNotNull(info)
        assertEquals(243_000L, info!!.durationMillis)
        assertEquals(30_000L, info.positionMillis)
        assertEquals(123456L, info.lastPositionUpdateTime)
        assertEquals(1.0f, info.playbackSpeed)
        assertTrue(info.canSeek)
        assertTrue(info.hasValidProgress)
    }

    @Test
    fun mapPlaybackInfo_trackChangeResetsProgressToNewState() {
        val track1 = RawPlaybackData(
            title = "Track 1",
            durationMillis = 180_000L,
            positionMillis = 90_000L
        )
        val track2 = RawPlaybackData(
            title = "Track 2",
            durationMillis = 240_000L,
            positionMillis = 0L
        )
        val info1 = MediaSessionPolicy.mapPlaybackInfo(track1)
        val info2 = MediaSessionPolicy.mapPlaybackInfo(track2)

        assertEquals(180_000L, info1!!.durationMillis)
        assertEquals(90_000L, info1.positionMillis)

        assertEquals(240_000L, info2!!.durationMillis)
        assertEquals(0L, info2.positionMillis)
    }
}
