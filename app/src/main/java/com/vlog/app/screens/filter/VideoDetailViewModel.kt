package com.vlog.app.screens.filter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.videos.VideoRepository
import com.vlog.app.data.histories.watch.WatchHistoryRepository
import com.vlog.app.data.comments.Comments
import com.vlog.app.data.database.Resource
import com.vlog.app.data.videos.GatherList
import com.vlog.app.data.videos.PlayList
import com.vlog.app.data.videos.Videos
import com.vlog.app.data.videos.toVideos
import com.vlog.app.navigation.NavigationRoutes.OtherRoute.WatchHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class VideoDetailViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val watchHistoryRepository: WatchHistoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {


    private val videoId: String = savedStateHandle.get<String>("videoId") ?: ""

    private val _uiState = MutableStateFlow(VideoDetailUiState())
    val uiState: StateFlow<VideoDetailUiState> = _uiState.asStateFlow()

    init {
        if (videoId.isNotBlank()) {
            observeLocalVideoDetail()
            checkAndFetchRemoteVideoDetail()
            fetchGatherList()
            loadWatchHistory()
            loadComments()
            loadRecommendedVideos()
        } else {
            _uiState.update { it.copy(error = "Video ID is missing", isLoading = false) }
        }
    }

    private fun observeLocalVideoDetail() {
        viewModelScope.launch {
            videoRepository.getLocalVideoDetail(videoId).collectLatest { localVideo ->
                _uiState.update { currentState ->
                    if (currentState.videoDetail == null || localVideo != currentState.videoDetail) {
                        currentState.copy(videoDetail = localVideo?.toVideos(),isLoading = false)
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
                                videoDetail = resource.data?.toVideos() ?: currentState.videoDetail, // Update with fresh data
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
                _uiState.update { it.copy(isLoading = false, videoDetail = localVideoInitial?.toVideos()) }
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
                                isLoadingGathers = true,
                                error = null
                            )
                            is Resource.Success -> {
                                val gatherList = resource.data
                                val selectedGatherId = gatherList?.first()?.gatherId

                                if (!selectedGatherId.isNullOrEmpty()){
                                    loadPlayers(selectedGatherId)
                                }

                                val videos = uiState.value.videoDetail
                                if (videos?.gatherList.isNullOrEmpty()){
                                    videos?.gatherList?.addAll(gatherList?:emptyList())
                                }

                                currentState.copy(
                                    isLoadingGathers = false,
                                    gathers = gatherList ?: emptyList(),
                                    selectedGatherId = selectedGatherId,
                                    error = null
                                )
                            }
                            is Resource.Error -> currentState.copy(
                                isLoadingGathers = false,
                                error = resource.message
                            )
                        }
                    }
                }
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
                                videoDetail = resource.data?.toVideos() ?: currentState.videoDetail,
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
            fetchGatherList()
        }
    }

    /**
     * 加载观看历史
     */
    private fun loadWatchHistory() {
        viewModelScope.launch {
//            try {
//                val watchHistory = watchHistoryRepository.getWatchHistoryById(videoId)
//                ////Log.d("VideoDetailViewModel", "Watch history loaded: $watchHistory")
//
//                if (watchHistory != null) {
//                    _uiState.update {
//                        it.copy(
//                            watchHistory = watchHistory,
//                            lastPlayedPosition = watchHistory.lastPlayedPosition
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("VideoDetailViewModel", "Error loading watch history", e)
//            }
        }
    }

//    /**
//     * 加载视频的服务商列表
//     * 从视频详情中获取服务商列表
//     */
//    fun loadGathers() {
//        _uiState.update { it.copy(isLoadingGathers = true) }
//
//        viewModelScope.launch {
//            // 从视频详情中获取服务商列表
//            val videoDetail = uiState.value.videoDetail
//            if (videoDetail != null) {
//                // 从视频详情中获取服务商列表
//                val gathers = videoDetail.gatherList
//                ////Log.d("VideoDetailViewModel", "Gathers loaded from video detail: ${gathers.size}")
//
//                // 获取历史记录中的服务商ID
//                //val historyGatherId = uiState.value.watchHistory?.gatherId
//
//                // 如果有历史记录且服务商存在，选择该服务商
//                val selectedGatherId = gathers?.firstOrNull()?.gatherId
//
//                _uiState.update {
//                    it.copy(
//                        gathers = gathers?:emptyList(),
//                        selectedGatherId = selectedGatherId,
//                        isLoadingGathers = false
//                    )
//                }
//
//                // 如果有选中的服务商，自动加载播放地址
//                selectedGatherId?.let { gatherId ->
//                    loadPlayers(gatherId)
//                }
//            } else {
//
//                videoRepository.getGatherList(videoId).collectLatest { resource ->
//                    _uiState.update { currentState ->
//                        when (resource) {
//                            is Resource.Loading -> currentState.copy(
//                                isLoadingGathers = true,
//                                error = null
//                            )
//                            is Resource.Success -> currentState.copy(
//                                isLoadingGathers = false,
//                                gathers = resource.data ?: emptyList(),
//                                selectedGatherId = resource.data?.first()?.gatherId,
//                                players = resource.data?.first()?.playList ?: emptyList(),
//                                selectedPlayerUrl = resource.data?.first()?.playList?.first()?.playUrl,
//                                isLoadingPlayers = false,
//                                error = null
//                            )
//                            is Resource.Error -> currentState.copy(
//                                isLoadingGathers = false,
//                                error = resource.message
//                            )
//                        }
//                    }
//                }
//
//
//
//            }
//        }
//    }

    /**
     * 加载服务商的播放地址列表
     * 从视频详情中获取播放地址列表
     */
    fun loadPlayers(gatherId: String) {
        _uiState.update { it.copy(isLoadingPlayers = true, selectedGatherId = gatherId) }

        viewModelScope.launch {
            // 从视频详情中获取播放地址列表
            val gatherList = uiState.value.gathers
            val playList = gatherList.find { it.gatherId == gatherId }?.playList ?: emptyList()
            val selectedPlayerUrl = playList.firstOrNull()?.playUrl
            val selectedPlayer = playList.find { it.playUrl == selectedPlayerUrl }
            val gatherTitle = uiState.value.gathers.find { it.gatherId == gatherId }?.gatherTitle
            _uiState.update {
                it.copy(
                    players = playList,
                    selectedPlayerUrl = selectedPlayerUrl,
                    isLoadingPlayers = false
                )
            }
            selectedPlayerUrl?.let { playerUrl ->
                // 记录观看历史
                uiState.value.videoDetail?.let { videoDetail ->
                    watchHistoryRepository.addWatchHistoryFromVideo(
                        video = videoDetail,
                        gatherId = gatherId,
                        gatherName = gatherTitle,
                        playerUrl = selectedPlayerUrl,
                        episodeTitle = selectedPlayer?.title,
                        episodeIndex = 0
                    )
                }
            }
        }
    }
//
//    /**
//     * 选择播放地址
//     */
//    fun selectPlayerUrl(playerUrl: String, playerTitle: String? = null) {
//        _uiState.update { it.copy(selectedPlayerUrl = playerUrl) }
//
//        // 记录观看历史
//        uiState.value.videoDetail?.let { videoDetail ->
//            viewModelScope.launch {
//                // 获取当前服务商信息
//                val gatherId = uiState.value.selectedGatherId
//                val gatherName = uiState.value.gathers.find { it.gatherId == gatherId }?.gatherTitle
//
//                watchHistoryRepository.addWatchHistoryFromVideoDetail(
//                    videoDetail = videoDetail,
//                    playPosition = 0, // 新选择的剧集从头开始播放
//                    duration = 0,
//                    episodeTitle = playerTitle,
//                    gatherId = gatherId,
//                    gatherName = gatherName,
//                    playerUrl = playerUrl
//                )
//            }
//        }
//    }


//    /**
//     * 更新播放进度
//     */
//    fun updatePlayProgress(playPosition: Long, duration: Long) {
//        viewModelScope.launch {
//            // 获取当前服务商和播放地址信息
//            val gatherId = uiState.value.selectedGatherId
//            val gatherName = uiState.value.gathers.find { it.gatherId == gatherId }?.gatherTitle
//            val playerUrl = uiState.value.selectedPlayerUrl
//
//            watchHistoryRepository.updatePlayProgress(
//                videoId = videoId,
//                playPosition = playPosition,
//                duration = duration,
//                gatherId = gatherId,
//                gatherName = gatherName,
//                playerUrl = playerUrl
//            )
//        }
//    }

    /**
     * 加载评论
     */
    fun loadComments() {
        _uiState.update { it.copy(isLoadingComments = true) }

        viewModelScope.launch {
//            try {
//                val comments = commentRepository.getComments(videoId, 0)
//                ////Log.d("VideoDetailViewModel", "Comments loaded: ${comments.size}")
//
//                _uiState.update {
//                    it.copy(
//                        comments = comments,
//                        isLoadingComments = false
//                    )
//                }
//            } catch (e: Exception) {
//                Log.e("VideoDetailViewModel", "Error loading comments", e)
//                _uiState.update {
//                    it.copy(
//                        isLoadingComments = false,
//                        error = e.message ?: "Failed to load comments"
//                    )
//                }
//            }
        }
    }

    /**
     * 发表评论
     */
    fun postComment(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
//            try {
//                val success = commentRepository.postComment(videoId, 0, content)
//                if (success) {
//                    // 发表成功后重新加载评论
//                    loadComments()
//                }
//            } catch (e: Exception) {
//                Log.e("VideoDetailViewModel", "Error posting comment", e)
//                _uiState.update {
//                    it.copy(error = e.message ?: "Failed to post comment")
//                }
//            }
        }
    }

    /**
     * 加载推荐视频
     */
    fun loadRecommendedVideos() {
        _uiState.update { it.copy(isLoadingRecommendations = true) }

        viewModelScope.launch {
//            val result = videoRepository.getMoreLiked(videoId, 0)
//
//            if (result.isSuccess) {
//                val videos = result.getOrNull() ?: emptyList()
//                ////Log.d("VideoDetailViewModel", "Recommended videos loaded: ${videos.size}")
//
//                _uiState.update {
//                    it.copy(
//                        recommendedVideos = videos,
//                        isLoadingRecommendations = false
//                    )
//                }
//            } else {
//                val exception = result.exceptionOrNull()
//                Log.e("VideoDetailViewModel", "Error loading recommended videos", exception)
//                _uiState.update {
//                    it.copy(
//                        isLoadingRecommendations = false,
//                        error = exception?.message ?: "Failed to load recommendations"
//                    )
//                }
//            }
        }
    }


    /**
     * 显示服务商和播放列表整合对话框
     */
    fun showGatherAndPlayerDialog() {
        _uiState.update { it.copy(showGatherAndPlayerDialog = true) }
    }

    /**
     * 隐藏服务商和播放列表整合对话框
     */
    fun hideGatherAndPlayerDialog() {
        _uiState.update { it.copy(showGatherAndPlayerDialog = false) }
    }

    /**
     * 切换标签页
     */
    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
}

data class VideoDetailUiState(
    val videoDetail: Videos? = null,
    val gathers: List<GatherList> = emptyList(),
    val selectedGatherId: String? = null,
    val players: List<PlayList> = emptyList(),
    val selectedPlayerUrl: String? = null, // 当前选中的播放地址
    val isLoading: Boolean = false,
    val isLoadingGathers: Boolean = false,
    val isLoadingPlayers: Boolean = false,
    val isLoadingComments: Boolean = false,
    val isLoadingRecommendations: Boolean = false,
    val error: String? = null,
    val watchHistory: WatchHistory? = null, // 观看历史
    val lastPlayedPosition: Long = 0, // 上次播放位置
    val comments: List<Comments> = emptyList(), // 评论列表
    val recommendedVideos: List<Videos> = emptyList(), // 推荐视频列表
    val showGatherDialog: Boolean = false, // 是否显示服务商对话框
    val showPlayerDialog: Boolean = false, // 是否显示播放列表对话框
    val showGatherAndPlayerDialog: Boolean = false, // 是否显示服务商和播放列表整合对话框
    val selectedTab: Int = 0 // 0: 详情, 1: 评论
)