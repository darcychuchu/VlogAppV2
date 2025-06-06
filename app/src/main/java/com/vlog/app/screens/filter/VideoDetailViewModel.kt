package com.vlog.app.screens.filter // Or com.vlog.app.screens.videos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.database.Resource // Assuming this is the correct Resource path
import com.vlog.app.data.videos.VideoEntity
import com.vlog.app.data.videos.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoDetailUiState(
    val video: VideoEntity? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Potentially add gatherList items here later if needed
)

@HiltViewModel
class VideoDetailViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailUiState())
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

    private val videoId: String = savedStateHandle.get<String>("videoId") ?: ""

    init {
        if (videoId.isNotBlank()) {
            observeLocalVideoDetail()
            checkAndFetchRemoteVideoDetail()
        } else {
            _uiState.update { it.copy(error = "Video ID is missing", isLoading = false) }
        }
    }

    private fun observeLocalVideoDetail() {
        viewModelScope.launch {
            videoRepository.getLocalVideoDetail(videoId).collectLatest { localVideo ->
                _uiState.update { currentState ->
                    // Only update if it's different, or if we don't have video data yet
                    // This avoids replacing a fresh remote fetch with a slightly older local one unnecessarily
                    // if the remote fetch completes before this collector emits an older version.
                    if (currentState.video == null || localVideo != currentState.video) {
                         currentState.copy(video = localVideo)
                    } else {
                        currentState
                    }
                }
            }
        }
    }

    private fun checkAndFetchRemoteVideoDetail() {
        viewModelScope.launch {
            // Make decision based on the first emission from local data
            val localVideoInitial = videoRepository.getLocalVideoDetail(videoId).firstOrNull()

            val needsFetch = if (localVideoInitial == null) {
                true
            } else {
                val lastRefreshed = localVideoInitial.lastRefreshed ?: 0L
                (System.currentTimeMillis() - lastRefreshed) >= (24 * 60 * 60 * 1000L) // 24-hour check
            }

            if (needsFetch) {
                _uiState.update { it.copy(isLoading = true, error = null) } // Show loading before fetch
                videoRepository.fetchAndCacheVideoDetail(videoId).collectLatest { resource ->
                    _uiState.update { currentState ->
                        when (resource) {
                            is Resource.Loading -> currentState.copy(isLoading = true) // Can keep it true
                            is Resource.Success -> currentState.copy(
                                isLoading = false,
                                video = resource.data ?: currentState.video, // Update with fresh data
                                error = null
                            )
                            is Resource.Error -> currentState.copy(
                                isLoading = false,
                                error = resource.message ?: "An unknown error occurred"
                            )
                        }
                    }
                }
            } else {
                // If no fetch is needed, the local observer `observeLocalVideoDetail`
                // should have already populated the UI. We just ensure loading is false,
                // and explicitly set the video data from our initial check.
                 _uiState.update { it.copy(isLoading = false, video = localVideoInitial) }
            }
        }
    }

    fun retryFetch() {
        if (videoId.isNotBlank()) {
            // Force fetch by ignoring timestamp check, directly call repository's remote fetch
             viewModelScope.launch {
                videoRepository.fetchAndCacheVideoDetail(videoId).collectLatest { resource ->
                    _uiState.update { currentState ->
                        when (resource) {
                            is Resource.Loading -> currentState.copy(isLoading = true, error = null)
                            is Resource.Success -> currentState.copy(
                                isLoading = false,
                                video = resource.data ?: currentState.video,
                                error = null
                            )
                            is Resource.Error -> currentState.copy(
                                isLoading = false,
                                error = resource.message ?: "An unknown error occurred"
                            )
                        }
                    }
                }
            }
        }
    }
}
