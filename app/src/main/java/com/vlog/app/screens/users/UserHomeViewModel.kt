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

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val DEFAULT_USER_STORY_TYPE = 0 // Assuming 0 for general user posts/stories
        private const val DEFAULT_ORDER_BY = 0 // 0 for 'latest'
    }

    // 从导航参数中获取用户名
    val username: String = checkNotNull(savedStateHandle["username"])

    // 用户动态列表
    private val _storiesList = MutableStateFlow<List<Stories>>(emptyList())
    val storiesList: StateFlow<List<Stories>> = _storiesList

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 刷新状态
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 分页信息
    private var currentPage = 1
    private val _hasMoreData = MutableStateFlow(true)
    val hasMoreData: StateFlow<Boolean> = _hasMoreData

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
    fun loadStoriesList(refresh: Boolean = false, storyType: Int = DEFAULT_USER_STORY_TYPE) {
        if (_isLoading.value && !refresh && _hasMoreData.value) return // Do not load if already loading or no more data unless refreshing

        if (refresh) {
            _isRefreshing.value = true
            currentPage = 1 // Reset page for refresh
            _storiesList.value = emptyList() // Clear existing list on refresh
            _hasMoreData.value = true // Assume there's data when refreshing
        } else {
            if (!_hasMoreData.value) return // Don't load more if no more data
            _isLoading.value = true
        }

        _error.value = null

        viewModelScope.launch {
            try {
                val token = userSessionManager.getAccessToken() ?: "" // Consider proper null handling or default token
                val response = storiesRepository.getUserStoriesList(
                    name = username, // Use the username from constructor
                    typed = storyType,
                    page = currentPage,
                    size = DEFAULT_PAGE_SIZE,
                    orderBy = DEFAULT_ORDER_BY,
                    token = token
                )

                Log.d("UserHomeViewModel", "API响应 for $username: code=${response.code}, message=${response.message}, data=${response.data != null}")

                if (response.code == ApiResponseCode.SUCCESS && response.data != null) {
                    val paginatedResponse = response.data
                    val newStories = paginatedResponse.items // Assuming PaginatedResponse has 'items'

                    Log.d("UserHomeViewModel", "获取到用户 $username 的列表: ${newStories.size}个, 当前页: $currentPage")

                    if (refresh) {
                        _storiesList.value = newStories
                    } else {
                        _storiesList.value = _storiesList.value + newStories
                    }

                    _hasMoreData.value = newStories.size == DEFAULT_PAGE_SIZE && newStories.isNotEmpty()
                    // Potential: _hasMoreData.value = currentPage < paginatedResponse.totalPages

                    if (newStories.isNotEmpty()) {
                        currentPage++
                    }
                } else {
                    _error.value = response.message ?: "加载失败"
                    _hasMoreData.value = false // Stop pagination on error
                    Log.e("UserHomeViewModel", "加载用户 $username 的列表失败: ${response.message}")
                }
            } catch (e: Exception) {
                _error.value = "加载失败: ${e.message}"
                _hasMoreData.value = false // Stop pagination on exception
                Log.e("UserHomeViewModel", "加载用户 $username 的列表异常", e)
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    /**
     * 加载更多数据
     */
    fun loadMore() {
        if (_isLoading.value || _isRefreshing.value || !_hasMoreData.value) return
        // Potentially pass the current tab's story type if loadDataByTab sets a specific type
        loadStoriesList(refresh = false, storyType = DEFAULT_USER_STORY_TYPE)
    }

    /**
     * 刷新数据
     */
    fun refresh() {
        // Potentially pass the current tab's story type if loadDataByTab sets a specific type
        loadStoriesList(refresh = true, storyType = DEFAULT_USER_STORY_TYPE)
    }

    /**
     * 根据选中的 Tab 加载数据
     * @param tabIndex Tab 索引（0=动态，1=作品）
     * For now, this simplifies to just refreshing the default type.
     * If different types are needed (e.g., "动态" vs "作品"), this function should
     * determine the 'storyType' based on tabIndex and pass it to loadStoriesList.
     */
    fun loadDataByTab(tabIndex: Int) {
        // Example: val storyType = if (tabIndex == 0) DEFAULT_USER_STORY_TYPE else OTHER_USER_STORY_TYPE
        // For now, always refreshing the default type.
        loadStoriesList(refresh = true, storyType = DEFAULT_USER_STORY_TYPE)
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
