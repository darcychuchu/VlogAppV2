package com.vlog.app.data.videos

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY orderSort ASC, title ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories ORDER BY orderSort ASC, title ASC")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)
    
    @Transaction
    suspend fun replaceAllCategories(categories: List<CategoryEntity>) {
        clearAllCategories()
        insertCategories(categories)
    }
    
    @Query("SELECT MAX(lastUpdated) FROM categories")
    suspend fun getLastUpdatedTime(): Long?
    
    // 检查是否需要更新分类数据（24小时后更新）
    suspend fun shouldUpdateCategories(): Boolean {
        val lastUpdated = getLastUpdatedTime() ?: 0
        val currentTime = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L
        return (currentTime - lastUpdated) > twentyFourHours
    }
}