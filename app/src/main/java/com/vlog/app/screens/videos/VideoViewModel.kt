package com.vlog.app.screens.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.videos.Categories
import com.vlog.app.data.videos.VideoList
import com.vlog.app.data.videos.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val videoRepository: VideoRepository
) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()
    
    // 分类数据
    private val _categories = MutableStateFlow<List<Categories>>(emptyList())
    val categories: StateFlow<List<Categories>> = _categories.asStateFlow()
    
    // 筛选条件
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    
    // 视频列表数据
    private val _videos = MutableStateFlow<List<VideoList>>(emptyList())
    val videos: StateFlow<List<VideoList>> = _videos.asStateFlow()
    
    // 分页状态
    private val _paginationState = MutableStateFlow(PaginationState())
    val paginationState: StateFlow<PaginationState> = _paginationState.asStateFlow()
    
    init {
        loadCategories(1)
        loadVideos()
    }
    
    // 加载分类数据
    fun loadCategories(typed: Int, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingCategories = true)
            
            videoRepository.getCategories(typed, forceRefresh)
                .onSuccess { categoriesList ->
                    _categories.value = categoriesList
                    _uiState.value = _uiState.value.copy(
                        isLoadingCategories = false,
                        categoriesError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingCategories = false,
                        categoriesError = error.message
                    )
                }
        }
    }
    
    // 加载视频列表
    fun loadVideos(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentFilter = _filterState.value
            val currentPagination = _paginationState.value
            
            if (forceRefresh) {
                _paginationState.value = currentPagination.copy(
                    currentPage = 1,
                    hasMorePages = true
                )
                _videos.value = emptyList()
            }
            
            _uiState.value = _uiState.value.copy(isLoadingVideos = true)
            
            val page = if (forceRefresh) 1 else _paginationState.value.currentPage
            
            videoRepository.getFilteredVideos(
                type = currentFilter.selectedType,
                categoryId = currentFilter.selectedCategoryId,
                year = currentFilter.selectedYear,
                sort = currentFilter.selectedSort,
                page = page,
                pageSize = 24,
                forceRefresh = forceRefresh
            )
                .onSuccess { paginatedResponse ->
                    val newVideos = paginatedResponse.items ?: emptyList()
                    
                    _videos.value = if (forceRefresh || page == 1) {
                        newVideos
                    } else {
                        _videos.value + newVideos
                    }
                    
                    _paginationState.value = _paginationState.value.copy(
                        currentPage = page,
                        totalItems = paginatedResponse.total,
                        hasMorePages = page < paginatedResponse.total,
                        isLoadingMore = false
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isLoadingVideos = false,
                        videosError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingVideos = false,
                        videosError = error.message
                    )
                    _paginationState.value = _paginationState.value.copy(
                        isLoadingMore = false
                    )
                }
        }
    }
    
    // 加载更多视频
    fun loadMoreVideos() {
        val currentPagination = _paginationState.value
        
        if (currentPagination.isLoadingMore || !currentPagination.hasMorePages) {
            return
        }
        
        viewModelScope.launch {
            _paginationState.value = currentPagination.copy(
                isLoadingMore = true,
                currentPage = currentPagination.currentPage + 1
            )
            
            loadVideos(forceRefresh = false)
        }
    }
    
    // 更新筛选条件
    fun updateFilter(
        type: Int = 0,
        categoryId: String? = null,
        year: Int = 0,
        sort: Int = 0
    ) {
        val currentFilter = _filterState.value
        val typeChanged = currentFilter.selectedType != type
        
        _filterState.value = currentFilter.copy(
            selectedType = type,
            selectedCategoryId = if (typeChanged) null else categoryId, // 类型改变时重置分类选择
            selectedYear = year,
            selectedSort = sort
        )
        
        // 如果类型改变，重新加载对应类型的分类数据
        if (typeChanged && type > 0) {
            loadCategories(type, forceRefresh = true)
        }
        
        // 重新加载视频列表
        loadVideos(forceRefresh = true)
    }
    
    // 重置筛选条件
    fun resetFilters() {
        _filterState.value = FilterState()
        loadVideos(forceRefresh = true)
    }
    
    // 下拉刷新
    fun refresh() {
        val currentType = _filterState.value.selectedType
        val typeToLoad = if (currentType > 0) currentType else 1
        loadCategories(typeToLoad, forceRefresh = true)
        loadVideos(forceRefresh = true)
    }
    
    // 搜索视频
    fun searchVideos(keyword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingVideos = true)
            
            videoRepository.searchVideos(keyword, page = 1, pageSize = 24)
                .onSuccess { paginatedResponse ->
                    _videos.value = paginatedResponse.items ?: emptyList()
                    _paginationState.value = PaginationState(
                        currentPage = 1,
                        totalItems = paginatedResponse.total,
                        hasMorePages = 1 < paginatedResponse.total
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoadingVideos = false,
                        videosError = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingVideos = false,
                        videosError = error.message
                    )
                }
        }
    }
}

// UI状态数据类
data class VideoUiState(
    val isLoadingVideos: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val videosError: String? = null,
    val categoriesError: String? = null
)

// 筛选状态数据类
data class FilterState(
    val selectedType: Int = 1, // 1=电影, 2=连续剧, 3=动漫, 4=综艺
    val selectedCategoryId: String? = null,
    val selectedYear: Int = 0, // 0=全部, 具体年份如2025
    val selectedSort: Int = 0 // 0=按上映时间, 1=按评分, 2=按热度, 3=按推荐
)

// 分页状态数据类
data class PaginationState(
    val currentPage: Int = 1,
    val totalItems: Int = 0,
    val hasMorePages: Boolean = true,
    val isLoadingMore: Boolean = false
)