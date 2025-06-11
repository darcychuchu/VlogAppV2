package com.vlog.app.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.ApiResponseCode
import com.vlog.app.data.stories.Stories
import com.vlog.app.data.stories.StoriesRepository
import com.vlog.app.data.users.UserDataRepository
import com.vlog.app.data.users.Users
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val storiesRepository: StoriesRepository
    // Other repositories like WatchHistoryRepository, AppUpdateRepository can be added if needed
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = userDataRepository.currentUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), userDataRepository.isLoggedIn())

    val currentUser: StateFlow<Users?> = userDataRepository.currentUser

    // User Stories State
    private val _userStories = MutableStateFlow<List<Stories>>(emptyList())
    val userStories: StateFlow<List<Stories>> = _userStories.asStateFlow()

    private val _storiesLoading = MutableStateFlow(false)
    val storiesLoading: StateFlow<Boolean> = _storiesLoading.asStateFlow()

    private val _storiesError = MutableStateFlow<String?>(null)
    val storiesError: StateFlow<String?> = _storiesError.asStateFlow()

    private var storiesCurrentPage = 1
    private val _hasMoreStories = MutableStateFlow(true)
    val hasMoreStories: StateFlow<Boolean> = _hasMoreStories.asStateFlow()

    // Follower/Following Counts State (Simplified as per subtask)
    private val _followerCount = MutableStateFlow(0)
    val followerCount: StateFlow<Int> = _followerCount.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    init {
        viewModelScope.launch {
            isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    fetchUserStories(refresh = true)
                    // fetchFollowCounts() // Skipped as per subtask instructions for V1 API
                } else {
                    // Clear user-specific data if any was loaded and user logs out
                    _userStories.value = emptyList()
                    storiesCurrentPage = 1
                    _hasMoreStories.value = true
                    _followerCount.value = 0
                    _followingCount.value = 0
                }
            }
        }
    }

    fun fetchUserStories(refresh: Boolean = false) {
        val user = currentUser.value
        val token = userDataRepository.getAccessToken() // userDataRepository.getAccessToken() is likely better

        if (user == null || token == null) {
            _storiesError.value = "User not logged in or token missing."
            // Optionally reset stories list if this state is reached unexpectedly
            // _userStories.value = emptyList()
            // _hasMoreStories.value = false
            return
        }
        val username = user.name ?: run {
            _storiesError.value = "Username is missing."
            return
        }

        if (_storiesLoading.value && !refresh) return

        if (refresh) {
            storiesCurrentPage = 1
            _hasMoreStories.value = true
            _userStories.value = emptyList() // Clear list on refresh
        } else if (!_hasMoreStories.value) {
            return // No more stories to load
        }

        _storiesLoading.value = true
        _storiesError.value = null

        viewModelScope.launch {
            try {
                // Assuming typed = 0 for general user stories, size = 10, orderBy = 0 for latest
                val response = storiesRepository.getUserStoriesList(
                    name = username,
                    typed = 0,
                    page = storiesCurrentPage,
                    size = 10,
                    orderBy = 0,
                    token = token
                )

                if (response.code == ApiResponseCode.SUCCESS && response.data != null) {
                    val newStories = response.data.items
                    if (refresh) {
                        if (newStories != null) {
                            _userStories.value = newStories
                        }
                    } else {
                        if (newStories != null) {
                            _userStories.value = _userStories.value + newStories
                        }
                    }
                    storiesCurrentPage++
                    _hasMoreStories.value = newStories?.size == 10 // Assuming page size is 10
                } else {
                    _storiesError.value = response.message ?: "Failed to load user stories."
                    _hasMoreStories.value = false // Stop pagination on error
                }
            } catch (e: Exception) {
                _storiesError.value = "Error fetching stories: ${e.message}"
                _hasMoreStories.value = false // Stop pagination on exception
                Log.e("ProfileViewModel", "Error fetching user stories", e)
            } finally {
                _storiesLoading.value = false
            }
        }
    }

    fun loadMoreUserStories() {
        if (!storiesLoading.value && hasMoreStories.value) {
            fetchUserStories(refresh = false)
        }
    }

    // fun fetchFollowCounts() { /* Skipped for now due to V1 API concerns */ }

    fun refreshData() {
        if (isLoggedIn.value) {
            fetchUserStories(refresh = true)
            // fetchFollowCounts() // Skipped
        }
    }
}
