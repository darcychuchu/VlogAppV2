package com.vlog.app.screens.search

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

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<Videos>>(emptyList())
    val searchResults: StateFlow<List<Videos>> = _searchResults.asStateFlow()
    
    private val _searchHistory = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryEntity>> = _searchHistory.asStateFlow()
    
    private val _hotSearches = MutableStateFlow<List<Videos>>(emptyList())
    val hotSearches: StateFlow<List<Videos>> = _hotSearches.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadHotSearches()
        loadSearchHistory()
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun search(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = searchRepository.searchVideos(query)
                _searchResults.value = results ?: emptyList()
                
                // 保存搜索历史
                searchRepository.saveSearchQuery(query)
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteSearchHistory(searchHistory: SearchHistoryEntity) {
        viewModelScope.launch {
            searchRepository.deleteSearchQuery(searchHistory)
        }
    }
    
    fun clearAllSearchHistory() {
        viewModelScope.launch {
            searchRepository.clearAllSearchHistory()
        }
    }
    
    private fun loadHotSearches() {
        viewModelScope.launch {
            try {
                // 获取热门搜索（传入null获取热门内容）
                val hotSearches = searchRepository.searchVideos(null)
                _hotSearches.value = hotSearches ?: emptyList()
            } catch (e: Exception) {
                _hotSearches.value = emptyList()
            }
        }
    }
    
    private fun loadSearchHistory() {
        viewModelScope.launch {
            searchRepository.getRecentSearches().collect {
                _searchHistory.value = it
            }
        }
    }
}