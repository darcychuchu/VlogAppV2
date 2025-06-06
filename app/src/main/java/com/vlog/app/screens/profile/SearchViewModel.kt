package com.vlog.app.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.histories.search.SearchHistoryEntity
import com.vlog.app.data.histories.search.SearchRepository
import com.vlog.app.data.videos.Videos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val isLoading: Boolean = false,
    val searchResults: List<Videos> = emptyList(),
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    val searchHistory = searchRepository.getRecentSearches()
    
    init {
        // 初始加载热门搜索
        loadHotSearches()
    }
    
    private fun loadHotSearches() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val results = searchRepository.searchVideos(null) ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    searchResults = results
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }
    
    fun search(query: String) {
        if (query.isBlank()) {
            loadHotSearches()
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                error = null,
                searchQuery = query
            )
            try {
                // 保存搜索历史
                searchRepository.saveSearchQuery(query)
                
                // 执行搜索
                val results = searchRepository.searchVideos(query) ?: emptyList()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    searchResults = results
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "搜索失败: ${e.message}"
                )
            }
        }
    }
    
    fun deleteSearchHistory(searchHistory: SearchHistoryEntity) {
        viewModelScope.launch {
            try {
                searchRepository.deleteSearchQuery(searchHistory)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除失败: ${e.message}"
                )
            }
        }
    }
    
    fun clearAllSearchHistory() {
        viewModelScope.launch {
            try {
                searchRepository.clearAllSearchHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "清空失败: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}