package com.vlog.app.data.categories

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.vlog.app.screens.filter.FilterItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分类配置管理器
 * 负责管理分类的显示、隐藏和排序配置
 */
@Singleton
class CategoryConfigManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
    private val categoryRepository: CategoryRepository
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("category_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENABLED_CATEGORIES = "enabled_categories"
        private const val KEY_CATEGORY_ORDER = "category_order"
        private const val KEY_LOGIN_REQUIRED_CATEGORIES = "login_required_categories"

        // 默认的分类配置
        val DEFAULT_CATEGORIES = listOf(
            FilterItem("1", "电影"),
            FilterItem("2", "电视剧"),
            FilterItem("3", "动漫"),
            FilterItem("4", "综艺"),
            FilterItem("5", "体育赛事"),
            FilterItem("8", "影视解说"),
            FilterItem("9", "预告片")
        )

        // 需要登录的分类ID
        val DEFAULT_LOGIN_REQUIRED_CATEGORIES = setOf("8", "9") // 影视解说和预告片
    }

    /**
     * 获取启用的分类列表
     */
    suspend fun getEnabledCategories(): List<FilterItem> {
        val enabledIds = getEnabledCategoryIds()
        val categoryOrder = getCategoryOrder()

        // 使用默认分类作为基础，然后从数据库获取最新的分类信息进行补充
        val baseCategories = DEFAULT_CATEGORIES.toMutableList()

        try {
            // 从数据库获取分类信息，用于更新分类名称
            val dbCategories = categoryRepository.getMainCategories().first()
            val dbCategoryMap = dbCategories.associateBy { it.id }

            // 更新基础分类的名称（如果数据库中有对应的分类）
            baseCategories.forEachIndexed { index, category ->
                dbCategoryMap[category.id]?.let { dbCategory ->
                    baseCategories[index] = FilterItem(dbCategory.id, dbCategory.title)
                }
            }
        } catch (e: Exception) {
            // 如果数据库读取失败，使用默认分类
        }

        // 根据启用的ID筛选分类
        val categoriesToShow = baseCategories.filter { enabledIds.contains(it.id) }

        return if (categoryOrder.isEmpty()) {
            categoriesToShow
        } else {
            categoriesToShow.sortedBy { categoryOrder.indexOf(it.id).takeIf { it >= 0 } ?: Int.MAX_VALUE }
        }
    }

    /**
     * 获取启用的分类ID集合
     */
    private fun getEnabledCategoryIds(): Set<String> {
        val json = sharedPreferences.getString(KEY_ENABLED_CATEGORIES, null)
        return if (json != null) {
            try {
                val type = Types.newParameterizedType(Set::class.java, String::class.java)
                moshi.adapter<Set<String>>(type).fromJson(json) ?: DEFAULT_CATEGORIES.map { it.id }.toSet()
            } catch (e: Exception) {
                // 如果解析失败，返回默认分类
                DEFAULT_CATEGORIES.map { it.id }.toSet()
            }
        } else {
            // 如果没有配置，返回默认分类
            DEFAULT_CATEGORIES.map { it.id }.toSet()
        }
    }

    /**
     * 设置启用的分类ID集合
     */
    fun setEnabledCategoryIds(categoryIds: Set<String>) {
        val type = Types.newParameterizedType(Set::class.java, String::class.java)
        val json = moshi.adapter<Set<String>>(type).toJson(categoryIds)
        sharedPreferences.edit {
            putString(KEY_ENABLED_CATEGORIES, json)
        }
    }

    /**
     * 获取分类排序
     */
    private fun getCategoryOrder(): List<String> {
        val json = sharedPreferences.getString(KEY_CATEGORY_ORDER, null)
        return if (json != null) {
            try {
                val type = Types.newParameterizedType(List::class.java, String::class.java)
                moshi.adapter<List<String>>(type).fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * 设置分类排序
     */
    fun setCategoryOrder(categoryIds: List<String>) {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val json = moshi.adapter<List<String>>(type).toJson(categoryIds)
        sharedPreferences.edit {
            putString(KEY_CATEGORY_ORDER, json)
        }
    }

    /**
     * 获取需要登录的分类ID集合
     */
    fun getLoginRequiredCategoryIds(): Set<String> {
        val json = sharedPreferences.getString(KEY_LOGIN_REQUIRED_CATEGORIES, null)
        return if (json != null) {
            try {
                val type = Types.newParameterizedType(Set::class.java, String::class.java)
                moshi.adapter<Set<String>>(type).fromJson(json) ?: DEFAULT_LOGIN_REQUIRED_CATEGORIES
            } catch (e: Exception) {
                DEFAULT_LOGIN_REQUIRED_CATEGORIES
            }
        } else {
            DEFAULT_LOGIN_REQUIRED_CATEGORIES
        }
    }

    /**
     * 设置需要登录的分类ID集合
     */
    fun setLoginRequiredCategoryIds(categoryIds: Set<String>) {
        val type = Types.newParameterizedType(Set::class.java, String::class.java)
        val json = moshi.adapter<Set<String>>(type).toJson(categoryIds)
        sharedPreferences.edit {
            putString(KEY_LOGIN_REQUIRED_CATEGORIES, json)
        }
    }

    /**
     * 检查分类是否需要登录
     */
    fun isCategoryLoginRequired(categoryId: String): Boolean {
        return getLoginRequiredCategoryIds().contains(categoryId)
    }

    /**
     * 切换分类的启用状态
     */
    fun toggleCategoryEnabled(categoryId: String) {
        val currentEnabled = getEnabledCategoryIds().toMutableSet()
        if (currentEnabled.contains(categoryId)) {
            currentEnabled.remove(categoryId)
        } else {
            currentEnabled.add(categoryId)
        }
        setEnabledCategoryIds(currentEnabled)
    }

    /**
     * 获取所有可用的分类（用于设置界面）
     */
    suspend fun getAllAvailableCategories(): List<FilterItem> {
        // 使用默认分类作为基础，然后从数据库获取最新的分类信息进行补充
        val baseCategories = DEFAULT_CATEGORIES.toMutableList()

        try {
            // 从数据库获取分类信息，用于更新分类名称
            val dbCategories = categoryRepository.getMainCategories().first()
            val dbCategoryMap = dbCategories.associateBy { it.id }

            // 更新基础分类的名称（如果数据库中有对应的分类）
            baseCategories.forEachIndexed { index, category ->
                dbCategoryMap[category.id]?.let { dbCategory ->
                    baseCategories[index] = FilterItem(dbCategory.id, dbCategory.title)
                }
            }
        } catch (e: Exception) {
            // 如果数据库读取失败，使用默认分类
        }

        return baseCategories
    }

    /**
     * 重置为默认配置
     */
    fun resetToDefault() {
        sharedPreferences.edit {
            remove(KEY_ENABLED_CATEGORIES)
            remove(KEY_CATEGORY_ORDER)
            remove(KEY_LOGIN_REQUIRED_CATEGORIES)
        }
    }
}