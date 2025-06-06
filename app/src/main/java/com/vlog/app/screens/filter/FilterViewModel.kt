package com.vlog.app.screens.filter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.R
import com.vlog.app.data.categories.CategoryRepository
import com.vlog.app.data.videos.VideoRepository
import com.vlog.app.data.videos.Videos
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilterViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val TAG = "FilterViewModel"

    private val _uiState = MutableStateFlow(FilterUiState())
    val uiState: StateFlow<FilterUiState> = _uiState.asStateFlow()

    init {
        // 检查并更新分类数据
        checkAndUpdateCategories()

        // 加载默认分类的子分类
        val defaultCategoryId = _uiState.value.selectedCategory.id
        loadSubCategories(defaultCategoryId)

        // 加载筛选列表
        loadFilteredVideos(forceRefresh = false)
    }

    /**
     * 检查并更新分类数据
     */
    private fun checkAndUpdateCategories() {
        viewModelScope.launch {
            try {
                if (categoryRepository.shouldUpdateCategories()) {
                    categoryRepository.refreshCategories()
                }

                // 加载主分类
                loadMainCategories()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update categories")
                }
            }
        }
    }

    /**
     * 加载主分类
     */
    private fun loadMainCategories() {
        viewModelScope.launch {
            try {
                // 从数据库获取主分类
                val mainCategories = categoryRepository.getMainCategories().first()

                // 将主分类转换为 FilterItem 列表
                val filterItems = mainCategories.map {
                    FilterItem(it.id, it.title)
                }

                // 更新 UI 状态
                _uiState.update {
                    it.copy(
                        mainCategories = filterItems,
                        // 如果当前选中的分类不在新的分类列表中，则选择第一个分类
                        selectedCategory = if (filterItems.any { item -> item.id == it.selectedCategory.id }) {
                            it.selectedCategory
                        } else {
                            filterItems.firstOrNull() ?: it.selectedCategory
                        }
                    )
                }

                // 加载选中分类的子分类
                loadSubCategories(_uiState.value.selectedCategory.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load main categories")
                }
            }
        }
    }

    fun updateFilter(section: FilterSection, item: FilterItem) {
        _uiState.update { state ->
            when (section.param) {
                "typed" -> {
                    // 当选择了新的分类时，加载子分类
                    loadSubCategories(item.id)
                    state.copy(selectedCategory = item, selectedSubCategory = null)
                }
                "year" -> state.copy(selectedYear = item)
                "order_by" -> state.copy(selectedOrderBy = item)
                "cate" -> state.copy(selectedSubCategory = item)
                else -> state
            }
        }

        // 自动应用筛选条件
        loadFilteredVideos(forceRefresh = false)
    }

    /**
     * 加载子分类
     */
    private fun loadSubCategories(parentId: String) {
        _uiState.update { it.copy(isLoadingCategories = true) }

        viewModelScope.launch {
            try {
                // 从数据库获取子分类
                val subCategories = categoryRepository.getSubCategories(parentId).first()
                // 将子分类转换为 FilterItem 列表
                val filterItems = mutableListOf<FilterItem>()

                // 只有当有子分类时才添加“全部”选项
                if (subCategories.isNotEmpty()) {
                    filterItems.addAll(subCategories.map { FilterItem(it.id, it.title) })
                }

                _uiState.update {
                    it.copy(
                        subCategories = filterItems,
                        isLoadingCategories = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingCategories = false,
                        error = e.message ?: "Failed to load sub categories"
                    )
                }
            }
        }
    }

    fun resetFilters() {
        _uiState.update { state ->
            state.copy(
                selectedCategory = state.mainCategories.firstOrNull() ?: DefaultFilterConfig.categories.items.first(),
                selectedYear = DefaultFilterConfig.years.items.first(),
                selectedOrderBy = DefaultFilterConfig.orderBy.items.first(),
                selectedSubCategory = null
            )
        }

        // 加载选中分类的子分类
        loadSubCategories(_uiState.value.selectedCategory.id)

        // 加载筛选列表
        loadFilteredVideos(forceRefresh = false)
    }

    fun applyFilters() {
        loadFilteredVideos(forceRefresh = false)
    }

    /**
     * 刷新数据
     * 同时刷新分类数据和视频列表
     */
    fun refreshData() {
        _uiState.update { it.copy(isRefreshing = true) }

        viewModelScope.launch {
            try {
                // 刷新分类数据
                categoryRepository.refreshCategories()

                // 重新加载主分类
                loadMainCategories()

                // 重新加载视频列表
                loadFilteredVideos(forceRefresh = true)

                _uiState.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Failed to refresh data"
                    )
                }
            }
        }
    }

    private fun loadFilteredVideos(forceRefresh: Boolean = false) {
        _uiState.update { it.copy(
            isLoading = true,
            error = null,
            currentPage = 1, // 重置当前页码
            // canLoadMore = true // Reset by repository response
        ) }

        viewModelScope.launch {
            // 获取选中分类的 ID
            val currentUiState = _uiState.value
            val categoryId = currentUiState.selectedCategory.id

            // 从数据库获取分类实体，以获取 isTyped 字段
            val categoryEntity = categoryRepository.getCategoryById(categoryId)
            // 使用 isTyped 字段作为 typed 参数，如果为空则使用 ID
            val typed = categoryEntity?.modelTyped ?: categoryId.toIntOrNull()
            val year = currentUiState.selectedYear.id.toIntOrNull()
            val orderBy = currentUiState.selectedOrderBy.id.toIntOrNull()
            val cate = currentUiState.selectedSubCategory?.id

            val result = videoRepository.getFilteredVideos(
                typed = typed ?: 1, // Default to 1 if typed is null
                categoryId = cate,
                year = year ?: 0, // Default to 0 if year is null
                sort = orderBy ?: 0, // Default to 0 if sort is null
                page = 1, // Always page 1 for initial load
                forceRefresh = forceRefresh
            )

            result.fold(
                onSuccess = { responseData ->
                    _uiState.update {
                        it.copy(
                            videos = responseData.items ?: emptyList(),
                            isLoading = false,
                            canLoadMore = (responseData.items?.size ?: 0) == responseData.pageSize && responseData.total > (responseData.items?.size ?:0),
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load videos",
                            canLoadMore = false,
                            videos = emptyList() // Clear videos on error
                        )
                    }
                }
            )
        }
    }

    /**
     * 加载更多视频（下一页）
     */
    fun loadMoreVideos() {
        // 如果已经在加载中或者不能加载更多，则直接返回
        if (_uiState.value.isLoadingMore || !_uiState.value.canLoadMore) {
            return
        }

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            try {
                val nextPage = _uiState.value.currentPage + 1

                // 获取选中分类的 ID
                val categoryId = _uiState.value.selectedCategory.id

                // 从数据库获取分类实体，以获取 isTyped 字段
                val categoryEntity = categoryRepository.getCategoryById(categoryId)

                // 使用 isTyped 字段作为 typed 参数，如果为空则使用 ID
                val typed = categoryEntity?.modelTyped ?: categoryId.toIntOrNull()

                val year = _uiState.value.selectedYear.id.toIntOrNull()
                val orderBy = _uiState.value.selectedOrderBy.id.toIntOrNull()
                val cate = _uiState.value.selectedSubCategory?.id // Already handles "0" by being null if not selected

                val result = videoRepository.getFilteredVideos(
                    typed = typed ?: 1, // Default to 1 if typed is null
                    categoryId = cate,
                    year = year ?: 0, // Default to 0 if year is null
                    sort = orderBy ?: 0, // Default to 0 if sort is null
                    page = nextPage,
                    forceRefresh = false // Pagination should not force refresh
                )

                result.fold(
                    onSuccess = { responseData ->
                        _uiState.update {
                            it.copy(
                                videos = it.videos + (responseData.items ?: emptyList()),
                                currentPage = nextPage,
                                isLoadingMore = false,
                                canLoadMore = (responseData.items?.size ?: 0) == responseData.pageSize && it.videos.size + (responseData.items?.size ?: 0) < responseData.total,
                                error = null
                            )
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isLoadingMore = false,
                                error = exception.message ?: "Failed to load more videos",
                                canLoadMore = false
                            )
                        }
                    }
                )
            }
        }
    }
}

data class FilterUiState(
    val videos: List<Videos> = emptyList(),
    val mainCategories: List<FilterItem> = DefaultFilterConfig.categories.items,
    val selectedCategory: FilterItem = DefaultFilterConfig.categories.items.first(),
    val selectedYear: FilterItem = DefaultFilterConfig.years.items.first(),
    val selectedOrderBy: FilterItem = DefaultFilterConfig.orderBy.items.first(),
    val selectedSubCategory: FilterItem? = null,
    val subCategories: List<FilterItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true
)

data class FilterSection(
    val title: String,
    val items: List<FilterItem>,
    val param: String
)

data class FilterItem(
    val id: String,
    val name: String
)

object DefaultFilterConfig {
    val categories = FilterSection(
        title = "分类",
        items = listOf(
            FilterItem("1", "电影"),
            FilterItem("2", "电视剧"),
            FilterItem("3", "动漫"),
            FilterItem("4", "综艺"),
            FilterItem("5", "体育赛事"),
            FilterItem("9", "预告片")
        ),
        param = "typed"
    )

    val years = FilterSection(
        title = "年份",
        items = listOf(
            FilterItem("0", "年份"),
            FilterItem("2025", "2025"),
            FilterItem("2024", "2024"),
            FilterItem("2023", "2023"),
            FilterItem("2022", "2022")
        ),
        param = "year"
    )

    val orderBy = FilterSection(
        title = "排序",
        items = listOf(
            FilterItem("0", "创建时间"),
            FilterItem("1", "更新时间"),
            FilterItem("2", "热度"),
            FilterItem("3", "推荐")
        ),
        param = "order_by"
    )
}
