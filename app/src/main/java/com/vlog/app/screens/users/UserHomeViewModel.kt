package com.vlog.app.screens.users

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.ApiResponseCode
import com.vlog.app.data.stories.Stories
import com.vlog.app.data.stories.StoriesRepository
import com.vlog.app.data.users.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用户主页视图模型
 * 负责加载指定用户的动态和作品列表
 */
@HiltViewModel
class UserHomeViewModel @Inject constructor(
    private val storiesRepository: StoriesRepository,
    private val userSessionManager: UserSessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 从导航参数中获取用户名
    val username: String = checkNotNull(savedStateHandle["username"])

    // 用户动态列表
    private val _storiesList = MutableStateFlow<List<Stories>>(emptyList())
    val storiesList: StateFlow<List<Stories>> = _storiesList


    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 关注状态
    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing

    // 关注操作状态
    sealed class FollowActionState {
        object Idle : FollowActionState()
        object Loading : FollowActionState()
        data class Success(val message: String) : FollowActionState()
        data class Error(val message: String) : FollowActionState()
    }

    private val _followActionState = MutableStateFlow<FollowActionState>(FollowActionState.Idle)
    val followActionState: StateFlow<FollowActionState> = _followActionState

    // 粉丝数和关注数
    private val _followersCount = MutableStateFlow(0)
    val followersCount: StateFlow<Int> = _followersCount

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount

    // 初始化时加载数据
    init {
        Log.d("UserHomeViewModel", "初始化，加载用户 $username 的数据")
        loadStoriesList(refresh = true)
    }

    /**
     * 加载用户动态列表
     * @param refresh 是否刷新
     */
    fun loadStoriesList(refresh: Boolean = false) {
        val token = userSessionManager.getAccessToken() ?: ""

        if (_isLoading.value) return

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = storiesRepository.getStoriesList(
                    name = "",
                    token = token
                )

                if (response.code == ApiResponseCode.SUCCESS && response.data != null) {
                    val newStories = response.data
                    _storiesList.value = newStories
                } else {
                    _error.value = response.message ?: "加载失败"
                    Log.e("UserHomeViewModel", "加载用户 $username 的动态列表失败: ${response.message}")
                }
            } catch (e: Exception) {
                _error.value = "加载失败: ${e.message}"
                Log.e("UserHomeViewModel", "加载用户 $username 的动态列表失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }


    /**
     * 刷新数据
     */
    fun refresh() {
        loadStoriesList(refresh = true)
    }

    /**
     * 根据选中的 Tab 加载数据
     * @param tabIndex Tab 索引（0=动态，1=作品）
     */
    fun loadDataByTab(tabIndex: Int) {
        loadStoriesList(refresh = true)
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 检查当前查看的用户是否是当前登录用户
     * @return 是否是当前登录用户
     */
    fun isCurrentUser(): Boolean {
        val currentUserName = userSessionManager.getUserName()
        return currentUserName == username
    }



}
