package com.vlog.app.screens.profile

import android.util.Log
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
 * 个人主页视图模型
 * 负责加载用户动态和作品列表
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val storiesRepository: StoriesRepository,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    // 用户动态列表
    private val _storiesList = MutableStateFlow<List<Stories>>(emptyList())
    val storiesList: StateFlow<List<Stories>> = _storiesList

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // 初始化时加载数据
    init {
        loadStoriesList(refresh = true)
    }

    /**
     * 加载用户动态列表
     * @param refresh 是否刷新
     */
    fun loadStoriesList(refresh: Boolean = false) {
        val userName = userSessionManager.getUserName() ?: return
        val token = userSessionManager.getAccessToken() ?: return

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
                    val newStories: List<Stories>? = response.data

                    // 添加日志
                    Log.d("ProfileViewModel", "获取到动态列表: ${newStories?.size}个")
                    newStories?.forEach { story ->
                        Log.d("ProfileViewModel", "动态: id=${story.id}, title=${story.title}, isTyped=${story.isTyped}")
                    }

                    // 更新列表
                    if (newStories != null) {
                        _storiesList.value = newStories
                    }
                } else {
                    _error.value = response.message ?: "加载失败"
                    Log.e("ProfileViewModel", "加载动态列表失败: ${response.message}")
                }
            } catch (e: Exception) {
                _error.value = "加载失败: ${e.message}"
                Log.e("ProfileViewModel", "加载动态列表失败", e)
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
        Log.d("ProfileViewModel", "加载 Tab $tabIndex 的数据")
        loadStoriesList(refresh = true)
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }
}
