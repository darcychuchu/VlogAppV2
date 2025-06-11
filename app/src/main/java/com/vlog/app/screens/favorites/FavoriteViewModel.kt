package com.vlog.app.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.favorites.FavoriteRepository
import com.vlog.app.data.favorites.FavoritesEntity
import com.vlog.app.data.favorites.FavoritesWithVideo
import com.vlog.app.data.users.UserDataRepository // Changed import
import com.vlog.app.data.users.UserSessionManager
// Removed: import com.vlog.app.screens.users.UserViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository,
    private val userSessionManager: UserSessionManager,
    private val userDataRepository: UserDataRepository // Changed to UserDataRepository
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(FavoriteUiState())
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()

    // Event for signaling login requirement
    private val _loginRequiredEvent = MutableStateFlow<Boolean>(false)
    val loginRequiredEvent: StateFlow<Boolean> = _loginRequiredEvent.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = userDataRepository.currentUser // Changed source to userDataRepository
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), userSessionManager.isLoggedIn())

    // 订阅列表（不包含关联视频信息）
    @OptIn(ExperimentalCoroutinesApi::class)
    val favorites: StateFlow<List<FavoritesEntity>> = isLoggedIn.flatMapLatest { loggedIn ->
        if (loggedIn) {
            favoriteRepository.getAllFavoriteVideos()
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 订阅视频详细数据（包含关联视频信息）
    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteVideosWithVideo: StateFlow<List<FavoritesWithVideo>> = isLoggedIn.flatMapLatest { loggedIn ->
        if (loggedIn) {
            favoriteRepository.getAllFavoriteVideosWithVideo()
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 订阅数量
    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteCount: StateFlow<Int> = isLoggedIn.flatMapLatest { loggedIn ->
        if (loggedIn) {
            favoriteRepository.getFavoriteVideoCountFlow()
        } else {
            flowOf(0) // Return 0 count if not logged in
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * 从服务器同步订阅列表到本地数据库
     */
    fun syncFavoritesFromServer() {
        // ViewModel's isLoggedIn StateFlow will ensure this doesn't run if not logged in,
        // but an early return here is still good practice if called directly.
        if (!isLoggedIn.value) {
            _uiState.value = _uiState.value.copy(
                error = "请先登录才能同步订阅", // More specific message
                isLoading = false
            )
            return
        }
        val currentUser = userSessionManager.getUser() // Should not be null if isLoggedIn is true
        if (currentUser?.name == null || currentUser.accessToken == null) {
             // This case should ideally not be reached if isLoggedIn is true and derived from currentUser
            _uiState.value = _uiState.value.copy(error = "用户信息不完整", isLoading = false)
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
        // Check if user is logged in by checking if critical user details are missing.
        if (currentUser?.name == null || currentUser.accessToken == null) {
            userDataRepository.setPendingSubscription(videoId) // Changed to userDataRepository
            _loginRequiredEvent.value = true
            onResult(false, "请先登录") // Existing callback for immediate UI feedback
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

    fun consumeLoginRequiredEvent() {
        _loginRequiredEvent.value = false
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