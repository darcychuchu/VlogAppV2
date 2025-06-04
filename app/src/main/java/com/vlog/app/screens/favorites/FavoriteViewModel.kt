package com.vlog.app.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.favorites.FavoriteRepository
import com.vlog.app.data.favorites.FavoritesEntity
import com.vlog.app.data.favorites.FavoritesWithVideo
import com.vlog.app.data.users.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(FavoriteUiState())
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()
    
    // 订阅列表（不包含关联视频信息）
    val favorites: StateFlow<List<FavoritesEntity>> = favoriteRepository.getAllFavoriteVideos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 订阅视频详细数据（包含关联视频信息）
    val favoriteVideosWithVideo: StateFlow<List<FavoritesWithVideo>> = favoriteRepository.getAllFavoriteVideosWithVideo()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 订阅数量
    val favoriteCount: StateFlow<Int> = favoriteRepository.getFavoriteVideoCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    /**
     * 从服务器同步订阅列表到本地数据库
     */
    fun syncFavoritesFromServer() {
        val currentUser = userSessionManager.getUser()
        if (currentUser?.name == null || currentUser.accessToken == null) {
            _uiState.value = _uiState.value.copy(
                error = "请先登录",
                isLoading = false
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            favoriteRepository.syncFavoritesFromServer(currentUser.name!!, currentUser.accessToken!!)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null,
                        lastUpdateMessage = "同步成功"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
        }
    }
    
    /**
     * 订阅视频
     */
    fun addToFavorites(videoId: String, onResult: (Boolean, String?) -> Unit) {
        val currentUser = userSessionManager.getUser()
        if (currentUser?.name == null || currentUser.accessToken == null) {
            onResult(false, "请先登录")
            return
        }
        
        viewModelScope.launch {
            favoriteRepository.createFavorite(videoId, currentUser.name!!, currentUser.accessToken!!)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        lastUpdateMessage = "订阅成功"
                    )
                    onResult(true, "订阅成功")
                }
                .onFailure { error ->
                    onResult(false, error.message ?: "订阅失败")
                }
        }
    }
    
    /**
     * 取消订阅视频
     */
    fun removeFromFavorites(videoId: String, onResult: (Boolean, String?) -> Unit) {
        val currentUser = userSessionManager.getUser()
        if (currentUser?.name == null || currentUser.accessToken == null) {
            onResult(false, "请先登录")
            return
        }
        
        viewModelScope.launch {
            favoriteRepository.removeFavorite(videoId, currentUser.name!!, currentUser.accessToken!!)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        lastUpdateMessage = "取消订阅成功"
                    )
                    onResult(true, "取消订阅成功")
                }
                .onFailure { error ->
                    onResult(false, error.message ?: "取消订阅失败")
                }
        }
    }
    
    /**
     * 检查视频是否已订阅
     */
    suspend fun isVideoFavorite(videoId: String): Boolean {
        return favoriteRepository.isVideoFavorite(videoId)
    }
    
    /**
     * 检查视频是否已订阅（Flow版本）
     */
    fun isVideoFavoriteFlow(videoId: String) = favoriteRepository.isVideoFavoriteFlow(videoId)
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 清除更新消息
     */
    fun clearUpdateMessage() {
        _uiState.value = _uiState.value.copy(lastUpdateMessage = null)
    }
}

/**
 * 订阅功能UI状态
 */
data class FavoriteUiState(
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val lastUpdateMessage: String? = null
)