package com.vlog.app.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.favorites.FavoriteRepository
import com.vlog.app.data.favorites.Favorites
import com.vlog.app.data.users.UserSessionManager
import com.vlog.app.data.videos.VideoDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    
    // 订阅列表
    private val _favorites = MutableStateFlow<List<Favorites>>(emptyList())
    val favorites: StateFlow<List<Favorites>> = _favorites.asStateFlow()
    
    // 订阅视频详细数据
    private val _favoriteVideos = MutableStateFlow<List<VideoDetail>>(emptyList())
    val favoriteVideos: StateFlow<List<VideoDetail>> = _favoriteVideos.asStateFlow()
    
    init {
        // 监听订阅数据变化
        viewModelScope.launch {
            favoriteRepository.favorites.collect {
                _favorites.value = it
            }
        }
        
        viewModelScope.launch {
            favoriteRepository.favoriteVideos.collect {
                _favoriteVideos.value = it
            }
        }
    }
    
    /**
     * 加载订阅列表
     */
    fun loadFavorites() {
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
            
            favoriteRepository.getFavorites(currentUser.name!!, currentUser.accessToken!!)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                    // 获取订阅列表成功后，立即获取视频详情
                    updateFavoriteVideos(forceUpdate = true)
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
                    // 订阅成功后立即更新本地数据
                    _uiState.value = _uiState.value.copy(
                        lastUpdateMessage = "订阅成功"
                    )
                    // 重新加载订阅列表
                    loadFavorites()
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
                    // 取消订阅成功后立即更新本地数据
                    _uiState.value = _uiState.value.copy(
                        lastUpdateMessage = "取消订阅成功"
                    )
                    // 重新加载订阅列表
                    loadFavorites()
                    onResult(true, "取消订阅成功")
                }
                .onFailure { error ->
                    onResult(false, error.message ?: "取消订阅失败")
                }
        }
    }
    
    /**
     * 更新订阅视频数据
     */
    fun updateFavoriteVideos(forceUpdate: Boolean = false) {
        val currentUser = userSessionManager.getUser()
        if (currentUser?.name == null || currentUser.accessToken == null) {
            _uiState.value = _uiState.value.copy(
                error = "请先登录",
                isUpdating = false
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, error = null)
            
            favoriteRepository.updateFavoriteVideos(currentUser.name!!, currentUser.accessToken!!, forceUpdate)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        error = null,
                        lastUpdateMessage = "更新成功"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        error = error.message,
                        lastUpdateMessage = null
                    )
                }
        }
    }
    
    /**
     * 检查视频是否已订阅
     */
    fun isVideoFavorited(videoId: String): Boolean {
        return favoriteRepository.isVideoFavorited(videoId)
    }
    
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