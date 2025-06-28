package com.vlog.app.screens.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.categories.CategoryConfigManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategorySettingsViewModel @Inject constructor(
    private val categoryConfigManager: CategoryConfigManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategorySettingsUiState())
    val uiState: StateFlow<CategorySettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val allCategories = categoryConfigManager.getAllAvailableCategories()
            val enabledCategories = categoryConfigManager.getEnabledCategories()
            val enabledIds = enabledCategories.map { it.id }.toSet()
            val loginRequiredIds = categoryConfigManager.getLoginRequiredCategoryIds()
            
            // 按照当前配置的顺序排列分类
            val orderedCategories = enabledCategories + 
                allCategories.filter { !enabledIds.contains(it.id) }
            
            _uiState.update {
                it.copy(
                    allCategories = orderedCategories,
                    enabledCategoryIds = enabledIds,
                    loginRequiredCategoryIds = loginRequiredIds
                )
            }
        }
    }

    /**
     * 切换分类的启用状态
     */
    fun toggleCategoryEnabled(categoryId: String) {
        viewModelScope.launch {
            categoryConfigManager.toggleCategoryEnabled(categoryId)
            
            _uiState.update { state ->
                val newEnabledIds = if (state.enabledCategoryIds.contains(categoryId)) {
                    state.enabledCategoryIds - categoryId
                } else {
                    state.enabledCategoryIds + categoryId
                }
                state.copy(enabledCategoryIds = newEnabledIds)
            }
        }
    }

    /**
     * 切换分类的登录要求
     */
    fun toggleCategoryLoginRequired(categoryId: String) {
        viewModelScope.launch {
            val currentLoginRequired = categoryConfigManager.getLoginRequiredCategoryIds().toMutableSet()
            if (currentLoginRequired.contains(categoryId)) {
                currentLoginRequired.remove(categoryId)
            } else {
                currentLoginRequired.add(categoryId)
            }
            categoryConfigManager.setLoginRequiredCategoryIds(currentLoginRequired)
            
            _uiState.update { state ->
                state.copy(loginRequiredCategoryIds = currentLoginRequired)
            }
        }
    }

    /**
     * 上移分类
     */
    fun moveCategoryUp(index: Int) {
        if (index <= 0) return
        
        viewModelScope.launch {
            val currentCategories = _uiState.value.allCategories.toMutableList()
            val item = currentCategories.removeAt(index)
            currentCategories.add(index - 1, item)
            
            // 更新配置
            categoryConfigManager.setCategoryOrder(currentCategories.map { it.id })
            
            _uiState.update {
                it.copy(allCategories = currentCategories)
            }
        }
    }

    /**
     * 下移分类
     */
    fun moveCategoryDown(index: Int) {
        val currentCategories = _uiState.value.allCategories
        if (index >= currentCategories.size - 1) return
        
        viewModelScope.launch {
            val mutableCategories = currentCategories.toMutableList()
            val item = mutableCategories.removeAt(index)
            mutableCategories.add(index + 1, item)
            
            // 更新配置
            categoryConfigManager.setCategoryOrder(mutableCategories.map { it.id })
            
            _uiState.update {
                it.copy(allCategories = mutableCategories)
            }
        }
    }

    /**
     * 重置为默认设置
     */
    fun resetToDefault() {
        viewModelScope.launch {
            categoryConfigManager.resetToDefault()
            loadSettings()
        }
    }
}

data class CategorySettingsUiState(
    val allCategories: List<FilterItem> = emptyList(),
    val enabledCategoryIds: Set<String> = emptySet(),
    val loginRequiredCategoryIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)