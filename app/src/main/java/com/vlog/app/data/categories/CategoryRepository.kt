package com.vlog.app.data.categories

import android.util.Log
import com.vlog.app.data.database.BaseRepository
import com.vlog.app.data.database.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分类数据仓库，用于处理分类数据的获取和更新
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryService: CategoryService,
    private val categoryDao: CategoryDao
) : BaseRepository {
    companion object {
        private const val TAG = "CategoryRepository"
        private const val MIN_UPDATE_INTERVAL = 48 * 60 * 60 * 1000 // 48小时，单位：毫秒
    }

    /**
     * 获取所有主分类
     */
    fun getMainCategories(): Flow<List<CategoriesEntity>> {
        return categoryDao.getMainCategories()
    }

    /**
     * 获取指定分类的子分类
     */
    fun getSubCategories(parentId: String): Flow<List<CategoriesEntity>> {
        return categoryDao.getSubCategories(parentId)
    }

    /**
     * 根据ID获取分类
     */
    suspend fun getCategoryById(id: String): CategoriesEntity? {
        return categoryDao.getCategoryById(id)
    }

    /**
     * 获取所有分类
     */
    fun getAllCategories(): Flow<List<CategoriesEntity>> {
        return categoryDao.getAllCategories()
    }

    /**
     * 根据类型获取分类
     */
    fun getCategoriesByTyped(typed: Int): Flow<List<CategoriesEntity>> {
        return categoryDao.getAllCategories().map { categories ->
            categories.filter { it.modelTyped == typed }
        }
    }

    /**
     * 检查是否需要更新分类数据
     * @return true 如果需要更新，false 如果不需要更新
     */
    suspend fun shouldUpdateCategories(): Boolean {
        try {
            val lastUpdateTime = categoryDao.getLastUpdateTime() ?: 0
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - lastUpdateTime

            // 如果距离上次更新时间超过最小更新间隔，或者数据库中没有分类数据，则需要更新
            return timeDiff > MIN_UPDATE_INTERVAL || lastUpdateTime == 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if categories should be updated", e)
            return true // 出错时默认需要更新
        }
    }

    /**
     * 从服务器获取并更新分类数据
     * @return 更新是否成功
     */
    suspend fun refreshCategories(): Result<Boolean> {
        // 使用safeApiCall获取分类数据
        val categoriesResult = safeApiCall {
            categoryService.getCategoryList().data
        }

        return if (categoriesResult.isSuccess) {
            try {
                val categories = categoriesResult.getOrNull() ?: emptyList()
                val categoryEntities = processCategoryTree(categories)
                categoryDao.refreshCategories(categoryEntities)
                Result.success(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing or saving categories", e)
                Result.failure(e)
            }
        } else {
            val error = categoriesResult.exceptionOrNull() ?: Exception("Unknown error")
            Log.e(TAG, "Failed to fetch categories", error)
            Result.failure(error)
        }
    }

    /**
     * 获取并同步分类数据
     * 如果本地数据过期或为空，则从网络获取
     * @return 分类数据流
     */
    fun getCategoriesWithSync(): Flow<Resource<List<CategoriesEntity>>> {
        return getDataWithCache(
            localDataSource = { categoryDao.getAllCategoriesList() },
            remoteDataSource = {
                val result = safeApiCall { categoryService.getCategoryList().data }
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }
                processCategoryTree(result.getOrNull() ?: emptyList())
            },
            saveRemoteData = { categoryEntities ->
                categoryDao.refreshCategories(categoryEntities)
            },
            shouldFetch = { localData ->
                localData.isEmpty() || withContext(Dispatchers.IO) { shouldUpdateCategories() }
            }
        )
    }

    /**
     * 处理分类树，将服务器返回的分类树转换为平铺的分类实体列表
     */
    private fun processCategoryTree(categories: List<Categories>): List<CategoriesEntity> {
        val result = mutableListOf<CategoriesEntity>()

        // 递归处理分类树
        fun processCategory(categories: Categories, parentId: String?) {

            // 创建分类实体
            val categoriesEntity = CategoriesEntity(
                id = categories.id,
                title = categories.title,
                parentId = parentId,
                modelId = categories.modelId,
                modelTyped = categories.modelTyped,
                orderSort = categories.orderSort,
                lastUpdated = System.currentTimeMillis()
            )

            result.add(categoriesEntity)

            // 递归处理子分类
            categories.categoryList.forEach { childCategory ->
                processCategory(childCategory, categories.id)
            }
        }

        // 处理所有主分类
        categories.forEach { category ->
            processCategory(category, null)
        }

        return result
    }
}