package com.vlog.app.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.ApiResponseCode
import com.vlog.app.data.followers.Followers
import com.vlog.app.data.stories.Stories
import com.vlog.app.data.stories.StoriesRepository
import com.vlog.app.data.users.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页视图模型
 * 负责加载全局动态和作品列表
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val storiesRepository: StoriesRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val DEFAULT_STORY_TYPE = 0
        private const val DEFAULT_ORDER_BY = 0 // 0 for 'latest'
    }

    // 全局动态和作品列表
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
    private val _pagination = MutableStateFlow<PaginationInfo?>(null)
    val pagination: StateFlow<PaginationInfo?> = _pagination

    // 当前页码
    private var currentPage = 1

    // 是否有更多数据
    private val _hasMoreData = MutableStateFlow(true)
    val hasMoreData: StateFlow<Boolean> = _hasMoreData

    // 关注的用户列表
    private val _followingUsers = MutableStateFlow<List<Followers>>(emptyList())
    val followingUsers: StateFlow<List<Followers>> = _followingUsers

    // 关注用户的内容列表
    private val _followingStoriesList = MutableStateFlow<List<Stories>>(emptyList())
    val followingStoriesList: StateFlow<List<Stories>> = _followingStoriesList

    // 关注标签页的加载状态
    private val _isFollowingLoading = MutableStateFlow(false)
    val isFollowingLoading: StateFlow<Boolean> = _isFollowingLoading

    // 关注标签页的刷新状态
    private val _isFollowingRefreshing = MutableStateFlow(false)
    val isFollowingRefreshing: StateFlow<Boolean> = _isFollowingRefreshing

    // 关注标签页的错误信息
    private val _followingError = MutableStateFlow<String?>(null)
    val followingError: StateFlow<String?> = _followingError

    // 关注标签页的分页信息
    private var followingCurrentPage = 1
    private val _followingHasMoreData = MutableStateFlow(true)
    val followingHasMoreData: StateFlow<Boolean> = _followingHasMoreData

    // 初始化时加载数据
    init {
        loadGlobalStoriesList(refresh = true)
    }

    /**
     * 加载全局动态和作品列表
     * @param refresh 是否刷新
     */
    fun loadGlobalStoriesList(refresh: Boolean = false) {
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
                val token = userSessionManager.getAccessToken()
                // Ensure token is not null or empty, though getAccessToken should handle this or throw.
                // For robustness, a check here might be good, or ensure getAccessToken() contract.
                val response = storiesRepository.getGlobalStoriesList(
                    typed = DEFAULT_STORY_TYPE,
                    page = currentPage,
                    size = DEFAULT_PAGE_SIZE,
                    orderBy = DEFAULT_ORDER_BY,
                    token = token.toString() // Assuming getAccessToken() returns a non-null, valid token string.
                )

                Log.d("HomeViewModel", "API响应: code=${response.code}, message=${response.message}, data=${response.data != null}")

                if (response.code == ApiResponseCode.SUCCESS && response.data != null) {
                    val paginatedResponse = response.data
                    val newStories = paginatedResponse.items // Assuming PaginatedResponse has 'items'

                    // 添加日志
                    Log.d("HomeViewModel", "获取到全局列表: ${newStories.size}个, 当前页: $currentPage")
                    newStories.forEach { story ->
                        Log.d("HomeViewModel", "内容: id=${story.id}, title=${story.title}, isTyped=${story.isTyped}")
                    }

                    // 更新列表
                    if (refresh) {
                        _storiesList.value = newStories
                    } else {
                        _storiesList.value = _storiesList.value + newStories
                    }

                    // Update pagination info if available from paginatedResponse
                    // For example: _pagination.value = PaginationInfo(paginatedResponse.currentPage, paginatedResponse.totalPages, ...)

                    // 更新是否有更多数据
                    _hasMoreData.value = newStories.size == DEFAULT_PAGE_SIZE && newStories.isNotEmpty()
                    // Alternative if PaginatedResponse has hasNextPage or totalPages:
                    // _hasMoreData.value = paginatedResponse.hasNextPage
                    // _hasMoreData.value = currentPage < paginatedResponse.totalPages

                    // 更新当前页码 only if data was successfully loaded and there might be more
                    if (newStories.isNotEmpty()) {
                        currentPage++
                    }

                } else {
                    _error.value = response.message ?: "加载失败"
                    _hasMoreData.value = false // Stop pagination on error
                    Log.e("HomeViewModel", "加载全局列表失败: ${response.message}")
                }
            } catch (e: Exception) {
                _error.value = "加载失败: ${e.message}"
                _hasMoreData.value = false // Stop pagination on exception
                Log.e("HomeViewModel", "加载全局列表失败", e)
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
        loadGlobalStoriesList(refresh = false)
    }

    /**
     * 刷新数据
     */
    fun refresh() {

//        isRefreshing = true
//        coroutineScope.launch {
//            delay(1500)
//            itemCount += 5
//            isRefreshing = false
//        }
        loadGlobalStoriesList(refresh = true)
    }


    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }


}

/**
 * 分页信息
 */
data class PaginationInfo(
    val currentPage: Int,
    val totalPages: Int,
    val pageSize: Int,
    val totalItems: Int
)
