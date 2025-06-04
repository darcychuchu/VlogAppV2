package com.vlog.app.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.histories.watch.WatchHistoryEntity
import com.vlog.app.data.histories.watch.WatchHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 观看历史 ViewModel
 */

@HiltViewModel
class WatchHistoryViewModel @Inject constructor(
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {


    
    // UI 状态
    private val _uiState = MutableStateFlow(WatchHistoryUiState())
    val uiState: StateFlow<WatchHistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadWatchHistory()
    }
    
    /**
     * 加载观看历史
     */
    fun loadWatchHistory() {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                watchHistoryRepository.getAllWatchHistory().collect { historyList ->
                    _uiState.update {
                        it.copy(
                            watchHistory = historyList,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load watch history"
                    )
                }
            }
        }
    }
    
    /**
     * 删除观看历史
     */
    fun deleteWatchHistory(videoId: String) {
        viewModelScope.launch {
            try {
                watchHistoryRepository.deleteWatchHistory(videoId)
                // 不需要重新加载，Flow 会自动更新
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to delete watch history")
                }
            }
        }
    }
    
    /**
     * 清空所有观看历史
     */
    fun clearAllWatchHistory() {
        viewModelScope.launch {
            try {
                watchHistoryRepository.clearAllWatchHistory()
                // 不需要重新加载，Flow 会自动更新
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to clear watch history")
                }
            }
        }
    }
}

/**
 * 观看历史 UI 状态
 */
data class WatchHistoryUiState(
    val watchHistory: List<WatchHistoryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

