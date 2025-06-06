package com.vlog.app.screens.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.videos.VideoRepository
import com.vlog.app.data.videos.Videos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoDetailViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoDetailUiState())
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

    private val _videoDetail = MutableStateFlow<Videos?>(null)
    val videoDetail: StateFlow<Videos?> = _videoDetail.asStateFlow()

    private val _showGatherDialog = MutableStateFlow(false)
    val showGatherDialog: StateFlow<Boolean> = _showGatherDialog.asStateFlow()

    /**
     * 加载视频详情
     */
    fun loadVideoDetail(videoId: String, gatherId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            videoRepository.getVideoDetail(
                id = videoId
            ).fold(
                onSuccess = { detail ->
                    _videoDetail.value = detail
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "加载视频详情失败"
                    )
                }
            )
        }
    }



    /**
     * 刷新视频详情
     */
    fun refreshVideoDetail(videoId: String, gatherId: String? = null) {
        loadVideoDetail(videoId, gatherId)
    }

    /**
     * 显示集数选择对话框
     */
    fun showGatherDialog() {
        _showGatherDialog.value = true
    }

    /**
     * 隐藏集数选择对话框
     */
    fun hideGatherDialog() {
        _showGatherDialog.value = false
    }

    /**
     * 选择集数
     */
    fun selectGather(videoId: String, gatherId: String) {
        hideGatherDialog()
        loadVideoDetail(videoId, gatherId)
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * 视频详情页面UI状态
 */
data class VideoDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)