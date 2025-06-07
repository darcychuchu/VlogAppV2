package com.vlog.app.screens.filter

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.vlog.app.data.histories.watch.WatchHistoryEntity
import com.vlog.app.data.histories.watch.WatchHistoryRepository
import com.vlog.app.data.videos.GatherList
import com.vlog.app.data.videos.PlayList
import com.vlog.app.data.videos.Videos
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoPlayerUiState(
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    // isFullscreen will now be the primary flag for the player UI to occupy the entire view bounds.
    // The actual device orientation (portrait/landscape) will be handled by the system.
    val isFullscreen: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val error: String? = null,
    val videoId: String? = null,
    val title: String? = null,
    var remarks: String? = null,
    var coverUrl: String? = null,
    val watchHistory: WatchHistoryEntity? = null, // 观看历史
    // val isOrientationFullscreen: Boolean = false, // This state is being removed or re-evaluated.
    // If needed, it would be derived from actual device configuration changes, not a direct toggle.
)

data class PlaylistState(
    val currentGatherIndex: Int = 0,
    val currentPlayIndex: Int = 0,
    val gatherList: List<GatherList> = emptyList(),
    val currentPlayList: List<PlayList> = emptyList(),
    val gatherId: String? = null,
    val gatherName: String? = null,
    val playerUrl: String? = null,
    val playerTitle: String? = null
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {

    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer?
        get() = _exoPlayer

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private val _playlistState = MutableStateFlow(PlaylistState())
    val playlistState: StateFlow<PlaylistState> = _playlistState.asStateFlow()

    private var playbackStateJob: Job? = null

    init {
        initializePlayerInternal()
    }

    private fun initializePlayerInternal() {
        if (_exoPlayer == null) {
            _exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(playerListener)
                playWhenReady = false // Default to not playing until user interaction
            }
            _uiState.update { it.copy(isLoading = false) } // Initial load done
        }
    }

    fun initializePlaylist(videoDetail: Videos, gatherList: List<GatherList>, gatherIndex: Int = 0, playIndex: Int = 0) {
        viewModelScope.launch {
            val currentGather = gatherList.getOrNull(gatherIndex)
            val currentPlayList = currentGather?.playList ?: emptyList()
            val currentPlayItem = currentPlayList.getOrNull(playIndex)

            _uiState.update {
                it.copy(
                    videoId = videoDetail.id,
                    title = videoDetail.title, // Keep main video title
                    coverUrl = videoDetail.coverUrl,
                    remarks = videoDetail.remarks,
                    isLoading = currentPlayItem?.playUrl == null // if no URL, might be loading
                )
            }

            _playlistState.update {
                it.copy(
                    currentGatherIndex = gatherIndex,
                    currentPlayIndex = playIndex,
                    gatherList = gatherList,
                    currentPlayList = currentPlayList,
                    gatherId = currentGather?.gatherId,
                    gatherName = currentGather?.gatherTitle,
                    playerUrl = currentPlayItem?.playUrl,
                    playerTitle = currentPlayItem?.title
                )
            }

            currentPlayItem?.playUrl?.let { url ->
                setMediaItem(url, currentPlayItem.title)
            } ?: _uiState.update { it.copy(error = "No valid media URL found for initial item.") }


            watchHistoryRepository.addWatchHistoryFromVideo(
                video = videoDetail,
                gatherId = currentGather?.gatherId,
                gatherName = currentGather?.gatherTitle,
                playerUrl = currentPlayItem?.playUrl,
                episodeTitle = currentPlayItem?.title,
                episodeIndex = playIndex
            )
        }
    }

    private fun setMediaItem(url: String, title: String? = null) {
        if (_exoPlayer == null) initializePlayerInternal()
        val mediaItem = MediaItem.fromUri(url)
        _exoPlayer?.setMediaItem(mediaItem)
        _exoPlayer?.prepare()
        // Update UI with the title of the current media item being prepared
        _uiState.update { it.copy(title = title ?: it.title) } // Use new title or keep existing video title
    }

    fun play() {
        _exoPlayer?.play()
    }

    fun pause() {
        _exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        _exoPlayer?.seekTo(positionMs)
    }

    private fun releasePlayer() {
        playbackStateJob?.cancel()
        _exoPlayer?.removeListener(playerListener)
        _exoPlayer?.release()
        _exoPlayer = null
        // Reset relevant parts of UI state, keep playlist info
        _uiState.update {
            it.copy(
                isPlaying = false,
                currentPosition = 0L,
                duration = 0L,
                bufferedPosition = 0L,
                isLoading = true, // Set to true as player is released
                error = null
                // title could be cleared or kept based on desired UX
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }


    fun playPrevious() {
        viewModelScope.launch {
            val current = _playlistState.value
            var newGatherIndex = current.currentGatherIndex
            var newPlayIndex = current.currentPlayIndex

            if (current.currentPlayIndex > 0) {
                newPlayIndex--
            } else if (current.currentGatherIndex > 0) {
                newGatherIndex--
                val previousGather = current.gatherList.getOrNull(newGatherIndex)
                newPlayIndex = (previousGather?.playList?.size ?: 1) - 1 // last item of prev gather
            } else {
                return@launch // No previous item
            }
            updatePlayerForPlaylistNavigation(newGatherIndex, newPlayIndex)
        }
    }

    fun playNext() {
        viewModelScope.launch {
            val current = _playlistState.value
            val currentGatherPlayList = current.currentPlayList
            var newGatherIndex = current.currentGatherIndex
            var newPlayIndex = current.currentPlayIndex

            if (current.currentPlayIndex < currentGatherPlayList.size - 1) {
                newPlayIndex++
            } else if (current.currentGatherIndex < current.gatherList.size - 1) {
                newGatherIndex++
                newPlayIndex = 0 // first item of next gather
            } else {
                return@launch // No next item
            }
            updatePlayerForPlaylistNavigation(newGatherIndex, newPlayIndex)
        }
    }

    fun selectGather(gatherIndex: Int, playIndex: Int = 0) {
        viewModelScope.launch {
            updatePlayerForPlaylistNavigation(gatherIndex, playIndex, true)
        }
    }

    fun selectPlaySource(playIndex: Int) {
        viewModelScope.launch {
            updatePlayerForPlaylistNavigation(_playlistState.value.currentGatherIndex, playIndex, true)
        }
    }

    private fun updatePlayerForPlaylistNavigation(gatherIndex: Int, playIndex: Int, isExplicitSelection: Boolean = false) {
        val currentPlaylist = _playlistState.value
        val targetGather = currentPlaylist.gatherList.getOrNull(gatherIndex)
        val targetPlayList = targetGather?.playList ?: emptyList()
        val validPlayIndex = playIndex.coerceIn(0, (targetPlayList.size -1).coerceAtLeast(0))
        val targetPlayItem = targetPlayList.getOrNull(validPlayIndex)

        if (targetPlayItem?.playUrl == null) {
            _uiState.update { it.copy(error = "Selected media has no URL.") }
            return
        }

        _playlistState.update {
            it.copy(
                currentGatherIndex = gatherIndex,
                currentPlayIndex = validPlayIndex,
                currentPlayList = targetPlayList,
                gatherId = targetGather?.gatherId,
                gatherName = targetGather?.gatherTitle,
                playerUrl = targetPlayItem.playUrl,
                playerTitle = targetPlayItem.title
            )
        }
        setMediaItem(targetPlayItem.playUrl, targetPlayItem.title)
        if (_uiState.value.isPlaying || isExplicitSelection) { // if was playing or user explicitly selected, start playing new item
            play()
        }

        watchHistoryRepository.updatePlayProgress( // Consider moving this to player event listener for position updates
            videoId = uiState.value.videoId ?: return,
            gatherId = targetGather?.gatherId,
            gatherName = targetGather?.gatherTitle,
            episodeIndex = validPlayIndex,
            playerTitle = targetPlayItem.title,
            playerUrl = targetPlayItem.playUrl
        )
    }


    fun getCurrentPlayUrl(): String? = _playlistState.value.playerUrl
    fun getCurrentPlayTitle(): String? = _playlistState.value.playerTitle
    fun getCurrentGatherTitle(): String? = _playlistState.value.gatherName

    fun hasPrevious(): Boolean {
        val current = _playlistState.value
        return current.currentPlayIndex > 0 || current.currentGatherIndex > 0
    }

    fun hasNext(): Boolean {
        val current = _playlistState.value
        return current.currentPlayIndex < current.currentPlayList.size - 1 ||
                current.currentGatherIndex < current.gatherList.size - 1
    }

    // This method is no longer needed as state is updated via playerListener
    // fun updatePlaybackState(...) {}

    fun toggleFullscreen() {
        // This will toggle the player's UI to take up the full available space,
        // hiding other UI elements in VideoDetailScreen.
        // It does not directly change device orientation.
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    // fun toggleOrientationFullscreen() {
    //     // This method is being removed. Device orientation is system-controlled.
    //     // _uiState.update { it.copy(isOrientationFullscreen = !it.isOrientationFullscreen) }
    // }

    fun setPlaybackSpeed(speed: Float) {
        _exoPlayer?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(playbackSpeed = speed) }
    }

    fun setError(error: String?) { // Should ideally be handled by Player.Listener
        _uiState.update { it.copy(error = error) }
    }

    fun clearError() {
        setError(null)
    }

    private fun startPlaybackStateUpdate() {
        playbackStateJob?.cancel()
        playbackStateJob = viewModelScope.launch {
            while (true) {
                _uiState.update {
                    it.copy(
                        currentPosition = _exoPlayer?.currentPosition ?: 0L,
                        duration = _exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L,
                        bufferedPosition = _exoPlayer?.bufferedPosition ?: 0L
                    )
                }
                // Update watch history progress
                val currentVideoId = _uiState.value.videoId
                if (currentVideoId != null && _exoPlayer?.isPlaying == true) {
                    watchHistoryRepository.updatePlayProgress(
                        videoId = currentVideoId,
                        playPosition = _exoPlayer?.currentPosition ?: 0L,
                        duration = _exoPlayer?.duration ?: 0L
                        // Other params like gatherId, episodeIndex if they can change during playback
                        // For now, assuming they are set when media item changes
                    )
                }
                delay(1000) // Update every second
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPlaybackStateUpdate()
            } else {
                playbackStateJob?.cancel()
                // Save final position when paused
                val currentVideoId = _uiState.value.videoId
                 if (currentVideoId != null) {
                    watchHistoryRepository.updatePlayProgress(
                        videoId = currentVideoId,
                        playPosition = _exoPlayer?.currentPosition ?: 0L,
                        duration = _exoPlayer?.duration ?: 0L
                    )
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update {
                it.copy(
                    isLoading = playbackState == Player.STATE_BUFFERING,
                    duration = _exoPlayer?.duration?.coerceAtLeast(0L) ?: it.duration // Ensure duration is not negative
                )
            }
            when (playbackState) {
                Player.STATE_READY -> {
                    // Duration might be available now
                     _uiState.update { it.copy(duration = _exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L) }
                }
                Player.STATE_ENDED -> {
                    _uiState.update { it.copy(isPlaying = false /*, currentPosition = 0L // or keep at end */) }
                    playbackStateJob?.cancel()
                    // Potentially play next item here if auto-play is enabled
                    // playNext()
                }
                Player.STATE_IDLE -> {
                    // Player is idle, possibly stopped or failed.
                     playbackStateJob?.cancel()
                }
                else -> {}
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _uiState.update { it.copy(error = error.message ?: "Unknown player error", isLoading = false) }
            playbackStateJob?.cancel()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // This is called when the player transitions to a new media item.
            // You might want to update UI based on the new mediaItem's metadata.
            // For example, if your MediaItem has tag with title:
            // val newTitle = mediaItem?.mediaMetadata?.title?.toString()
            // _uiState.update { it.copy(title = newTitle ?: "Loading...") }
            // Ensure playback state job is running if player is playing
            if (_exoPlayer?.isPlaying == true) {
                 startPlaybackStateUpdate()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
             _uiState.update { it.copy(currentPosition = newPosition.positionMs) }
        }
    }


    /**
     * 加载观看历史
     */
    fun loadWatchHistory(videoId: String) {
        viewModelScope.launch {
            try {
                val watchHistory = watchHistoryRepository.getWatchHistoryById(videoId)
                if (watchHistory != null) {
                    _uiState.update { it.copy(watchHistory = watchHistory) }
                    // Optionally, seek to last known position from history
                    // This should be done carefully, perhaps only if user confirms or if it's a fresh session
                    // seekTo(watchHistory.playPosition)
                    // And update playlist to match history
                    // setCurrentGatherAndEpisode(watchHistory.gatherId, watchHistory.episodeIndex)
                }
            } catch (e: Exception) {
                Log.e("VideoPlayerViewModel", "Error loading watch history", e)
            }
        }
    }
    
    /**
     * 设置当前服务商 (gather) 和剧集 (episode)
     * This function is intended to be called when restoring state, e.g., from watch history.
     */
    fun setCurrentGatherAndEpisode(gatherId: String?, episodeIndex: Int?) {
        viewModelScope.launch {
            if (gatherId == null || episodeIndex == null) return@launch

            val current = _playlistState.value
            val targetGatherIndex = current.gatherList.indexOfFirst { it.gatherId == gatherId }

            if (targetGatherIndex != -1) {
                // Found the gather, now set episode
                val targetGather = current.gatherList[targetGatherIndex]
                val targetPlayList = targetGather.playList ?: emptyList()
                val validEpisodeIndex = episodeIndex.coerceIn(0, (targetPlayList.size -1).coerceAtLeast(0))

                // Update player and state
                updatePlayerForPlaylistNavigation(targetGatherIndex, validEpisodeIndex, true)

                // Optionally, seek to a stored position after setting the item
                // This part needs to be coordinated with how `setPlayPosition` is used.
                // For example, if `loadWatchHistory` also calls `setPlayPosition`.
            }
        }
    }
    
    /**
     * 设置当前播放的 gatherId (from deep link or history)
     */
    fun setCurrentGather(gatherId: String) { // Renamed for clarity from previous version
        viewModelScope.launch {
            val current = _playlistState.value
            val gatherIndex = current.gatherList.indexOfFirst { it.gatherId == gatherId }
            if (gatherIndex >= 0) {
                // Default to first episode of this gather
                updatePlayerForPlaylistNavigation(gatherIndex, 0, true)
            }
        }
    }
    
    /**
     * 设置当前播放的 episodeIndex (from deep link or history)
     */
    fun setCurrentEpisode(episodeIndex: Int) { // Renamed for clarity
        viewModelScope.launch {
            val current = _playlistState.value
            // Assumes current gather is already correct or doesn't need to change
            updatePlayerForPlaylistNavigation(current.currentGatherIndex, episodeIndex, true)
        }
    }

    /**
     * 设置播放位置 (e.g., from watch history restoration)
     */
    fun setPlayPosition(position: Long) {
        // This should ideally be called AFTER the correct media item is set in ExoPlayer
        // If ExoPlayer is not ready or has no media, seekTo might be ignored or error.
        if (_exoPlayer?.playbackState == Player.STATE_READY || _exoPlayer?.playbackState == Player.STATE_BUFFERING || _exoPlayer?.playbackState == Player.STATE_ENDED ) {
             _exoPlayer?.seekTo(position)
        }
        // We update UI state optimistically, or rely on onPositionDiscontinuity listener
        _uiState.update { it.copy(currentPosition = position) }
    }
}