package com.vlog.app.screens.users

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.comments.CommentRepository
import com.vlog.app.data.comments.Comments
import com.vlog.app.data.database.Resource
import com.vlog.app.data.stories.Stories
import com.vlog.app.data.stories.StoriesRepository
import com.vlog.app.data.users.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserStoriesDetailViewModel @Inject constructor(
    private val storiesRepository: StoriesRepository,
    private val commentRepository: CommentRepository, // Injected CommentRepository
    private val userSessionManager: UserSessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Retrieve username and storyId from navigation arguments
    val username: String = checkNotNull(savedStateHandle["username"])
    val storyId: String = checkNotNull(savedStateHandle["storyId"])

    private val _storyDetail = MutableStateFlow<Stories?>(null)
    val storyDetail: StateFlow<Stories?> = _storyDetail

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Comment States
    private val _comments = MutableStateFlow<Resource<List<Comments>>>(Resource.Loading())
    val comments: StateFlow<Resource<List<Comments>>> = _comments

    private val _postCommentState = MutableStateFlow<Resource<String>?>(null)
    val postCommentState: StateFlow<Resource<String>?> = _postCommentState

    init {
        fetchStoryDetails()
        loadComments() // Load comments when ViewModel is initialized
    }

    fun fetchStoryDetails() {
        viewModelScope.launch {
            _isLoading.value = true // This isLoading is for story detail itself
            _error.value = null
            try {
                val token = userSessionManager.getAccessToken() ?: ""
                val response = storiesRepository.getStoriesDetail(name = username, id = storyId, token = token)
                if (response.code == com.vlog.app.data.ApiResponseCode.SUCCESS && response.data != null) {
                    _storyDetail.value = response.data
                } else {
                    _error.value = response.message ?: "Failed to load story details."
                }
            } catch (e: Exception) {
                _error.value = "An error occurred while fetching story details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadComments(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Not setting _isLoading here as it might conflict with story detail loading.
            // _comments itself has a Resource.Loading state.
            commentRepository.getComments(
                quoteId = storyId,
                commentType = CommentRepository.TYPE_STORY,
                forceRefresh = forceRefresh
            ).collect {
                _comments.value = it
            }
        }
    }

    fun postComment(title: String?, description: String) {
        if (description.isBlank()) {
            // Optionally handle blank comment error locally
            // _postCommentState.value = Resource.Error("Comment cannot be empty.", null)
            return
        }
        viewModelScope.launch {
            commentRepository.postComment(
                quoteId = storyId,
                commentType = CommentRepository.TYPE_STORY,
                title = title,
                description = description
            ).collect { resource ->
                _postCommentState.value = resource
                if (resource is Resource.Success) {
                    loadComments(forceRefresh = true) // Refresh comments after successful post
                }
            }
        }
    }

    fun clearPostCommentState() {
        _postCommentState.value = null
    }

    fun isUserLoggedIn(): Boolean = userSessionManager.isLoggedIn()
}
