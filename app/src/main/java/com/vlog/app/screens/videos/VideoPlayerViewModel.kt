package com.vlog.app.screens.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.vlog.app.data.videos.GatherList
import com.vlog.app.data.videos.PlayList
import javax.inject.Inject

data class VideoPlayerUiState(
    val isLoading: Boolean = false,
    val isPlaying: Boolean = false,
    val isFullscreen: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPosition: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val error: String? = null
)

data class PlaylistState(
    val currentGatherIndex: Int = 0,
    val currentPlayIndex: Int = 0,
    val gatherList: List<GatherList> = emptyList(),
    val currentPlayList: List<PlayList> = emptyList()
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()
    
    private val _playlistState = MutableStateFlow(PlaylistState())
    val playlistState: StateFlow<PlaylistState> = _playlistState.asStateFlow()
    
    fun initializePlaylist(gatherList: List<GatherList>, gatherIndex: Int = 0, playIndex: Int = 0) {
        viewModelScope.launch {
            val currentGather = gatherList.getOrNull(gatherIndex)
            val currentPlayList = currentGather?.playList ?: emptyList()
            
            _playlistState.value = PlaylistState(
                currentGatherIndex = gatherIndex,
                currentPlayIndex = playIndex,
                gatherList = gatherList,
                currentPlayList = currentPlayList
            )
        }
    }
    
    fun playPrevious() {
        viewModelScope.launch {
            val currentState = _playlistState.value
            val currentPlayList = currentState.currentPlayList
            
            if (currentState.currentPlayIndex > 0) {
                // 播放当前集数的上一个播放源
                _playlistState.value = currentState.copy(
                    currentPlayIndex = currentState.currentPlayIndex - 1
                )
            } else if (currentState.currentGatherIndex > 0) {
                // 播放上一集的最后一个播放源
                val previousGatherIndex = currentState.currentGatherIndex - 1
                val previousGather = currentState.gatherList.getOrNull(previousGatherIndex)
                val previousPlayList = previousGather?.playList ?: emptyList()
                
                if (previousPlayList.isNotEmpty()) {
                    _playlistState.value = currentState.copy(
                        currentGatherIndex = previousGatherIndex,
                        currentPlayIndex = previousPlayList.size - 1,
                        currentPlayList = previousPlayList
                    )
                }
            }
        }
    }
    
    fun playNext() {
        viewModelScope.launch {
            val currentState = _playlistState.value
            val currentPlayList = currentState.currentPlayList
            
            if (currentState.currentPlayIndex < currentPlayList.size - 1) {
                // 播放当前集数的下一个播放源
                _playlistState.value = currentState.copy(
                    currentPlayIndex = currentState.currentPlayIndex + 1
                )
            } else if (currentState.currentGatherIndex < currentState.gatherList.size - 1) {
                // 播放下一集的第一个播放源
                val nextGatherIndex = currentState.currentGatherIndex + 1
                val nextGather = currentState.gatherList.getOrNull(nextGatherIndex)
                val nextPlayList = nextGather?.playList ?: emptyList()
                
                if (nextPlayList.isNotEmpty()) {
                    _playlistState.value = currentState.copy(
                        currentGatherIndex = nextGatherIndex,
                        currentPlayIndex = 0,
                        currentPlayList = nextPlayList
                    )
                }
            }
        }
    }
    
    fun selectGather(gatherIndex: Int, playIndex: Int = 0) {
        viewModelScope.launch {
            val currentState = _playlistState.value
            val selectedGather = currentState.gatherList.getOrNull(gatherIndex)
            val selectedPlayList = selectedGather?.playList ?: emptyList()
            
            if (selectedPlayList.isNotEmpty()) {
                val validPlayIndex = playIndex.coerceIn(0, selectedPlayList.size - 1)
                _playlistState.value = currentState.copy(
                    currentGatherIndex = gatherIndex,
                    currentPlayIndex = validPlayIndex,
                    currentPlayList = selectedPlayList
                )
            }
        }
    }
    
    fun selectPlaySource(playIndex: Int) {
        viewModelScope.launch {
            val currentState = _playlistState.value
            if (playIndex in 0 until currentState.currentPlayList.size) {
                _playlistState.value = currentState.copy(
                    currentPlayIndex = playIndex
                )
            }
        }
    }
    
    fun getCurrentPlayUrl(): String? {
        val currentState = _playlistState.value
        return currentState.currentPlayList.getOrNull(currentState.currentPlayIndex)?.playUrl
    }
    
    fun getCurrentPlayTitle(): String? {
        val currentState = _playlistState.value
        return currentState.currentPlayList.getOrNull(currentState.currentPlayIndex)?.title
    }
    
    fun getCurrentGatherTitle(): String? {
        val currentState = _playlistState.value
        return currentState.gatherList.getOrNull(currentState.currentGatherIndex)?.gatherTitle
    }
    
    fun hasPrevious(): Boolean {
        val currentState = _playlistState.value
        return currentState.currentPlayIndex > 0 || currentState.currentGatherIndex > 0
    }
    
    fun hasNext(): Boolean {
        val currentState = _playlistState.value
        return currentState.currentPlayIndex < currentState.currentPlayList.size - 1 ||
                currentState.currentGatherIndex < currentState.gatherList.size - 1
    }
    
    // 播放器状态管理
    fun updatePlaybackState(
        isPlaying: Boolean? = null,
        currentPosition: Long? = null,
        duration: Long? = null,
        bufferedPosition: Long? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPlaying = isPlaying ?: _uiState.value.isPlaying,
                currentPosition = currentPosition ?: _uiState.value.currentPosition,
                duration = duration ?: _uiState.value.duration,
                bufferedPosition = bufferedPosition ?: _uiState.value.bufferedPosition
            )
        }
    }
    
    fun toggleFullscreen() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isFullscreen = !_uiState.value.isFullscreen
            )
        }
    }
    
    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                playbackSpeed = speed
            )
        }
    }
    
    fun setError(error: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                error = error
            )
        }
    }
    
    fun clearError() {
        setError(null)
    }
}