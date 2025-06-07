package com.vlog.app.screens.filter // Or com.vlog.app.screens.videos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.database.Resource
import com.vlog.app.data.videos.GatherList // Assuming this is the correct Resource path
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

data class FilterDetailUiState(
    val video: VideoEntity? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val gatherList: List<com.vlog.app.data.videos.GatherList>? = null,
    val isGatherListLoading: Boolean = false,
    val gatherListError: String? = null,
    val showGatherListDialog: Boolean = false
)

@HiltViewModel
class FilterDetailViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilterDetailUiState())
    val uiState: StateFlow<FilterDetailUiState> = _uiState.asStateFlow()

    private val videoId: String = savedStateHandle.get<String>("videoId") ?: ""

    init {
        if (videoId.isBlank()) {
            _uiState.update { it.copy(error = "Video ID is missing", isLoading = false) }
            return
        }
        loadInitialVideoDetailThenObserve()
        fetchGatherList()
    }

    private fun loadInitialVideoDetailThenObserve() {
        viewModelScope.launch {
            val localVideoInitial = videoRepository.getLocalVideoDetail(videoId).firstOrNull()
            val needsFetch = localVideoInitial == null || (System.currentTimeMillis() - (localVideoInitial.lastRefreshed ?: 0L)) >= (24 * 60 * 60 * 1000L)

            if (needsFetch) {
                _uiState.update { it.copy(isLoading = true, error = null) }
                videoRepository.fetchAndCacheVideoDetail(videoId).collectLatest { resource ->
                    _uiState.update { currentState ->
                        when (resource) {
                            is Resource.Loading -> currentState.copy(isLoading = true)
                            is Resource.Success -> currentState.copy(isLoading = false, video = resource.data ?: currentState.video, error = null)
                            is Resource.Error -> currentState.copy(isLoading = false, error = resource.message ?: "An unknown error occurred")
                        }
                    }
                }
                // After the flow from fetchAndCacheVideoDetail completes
                observeLocalVideoDetailContinuously()
            } else {
                _uiState.update { it.copy(isLoading = false, video = localVideoInitial, error = null) }
                observeLocalVideoDetailContinuously()
            }
        }
    }

    private fun fetchGatherList() {
        if (videoId.isNotBlank()) {
            viewModelScope.launch {
                videoRepository.getGatherList(videoId).collectLatest { resource ->
                    _uiState.update { currentState ->
                        when (resource) {
                            is Resource.Loading -> currentState.copy(
                                isGatherListLoading = true,
                                gatherListError = null
                            )
                            is Resource.Success -> currentState.copy(
                                isGatherListLoading = false,
                                gatherList = resource.data ?: emptyList(),
                                gatherListError = null
                            )
                            is Resource.Error -> currentState.copy(
                                isGatherListLoading = false,
                                gatherListError = resource.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeLocalVideoDetailContinuously() {
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

    fun retryFetch() {
        if (videoId.isNotBlank()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null, gatherListError = null) } // Reset errors
                videoRepository.fetchAndCacheVideoDetail(videoId).collectLatest { resource ->
                    _uiState.update { currentState ->
                        when (resource) {
                            is Resource.Loading -> currentState.copy(isLoading = true)
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
                observeLocalVideoDetailContinuously() // Restart continuous observation
            }
            fetchGatherList() // Re-fetch gather list as well
        }
    }

    fun onShowGatherListDialog() {
        _uiState.update { it.copy(showGatherListDialog = true) }
    }

    fun onDismissGatherListDialog() {
        _uiState.update { it.copy(showGatherListDialog = false) }
    }
}
