package com.vlog.app.data.categories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据访问对象，用于操作分类数据
 */
@Dao
interface CategoryDao {
    /**
     * 插入分类，如果已存在则替换
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertCategories(categories: List<CategoriesEntity>)

    /**
     * 获取所有主分类（parentId为null的分类）
     */
    @Query("SELECT * FROM categories WHERE parentId IS NULL ORDER BY orderSort DESC")
    fun getMainCategories(): Flow<List<CategoriesEntity>>

    /**
     * 获取指定分类的子分类
     */
    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY orderSort DESC")
    fun getSubCategories(parentId: String): Flow<List<CategoriesEntity>>

    /**
     * 根据ID获取分类
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoriesEntity?

    /**
     * 获取所有分类（Flow）
     */
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoriesEntity>>

    /**
     * 获取所有分类（List）
     */
    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<CategoriesEntity>

    /**
     * 获取最后更新时间
     */
    @Query("SELECT MAX(lastUpdated) FROM categories")
    suspend fun getLastUpdateTime(): Long?

    /**
     * 清空分类表
     */
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    /**
     * 事务：清空表并插入新数据
     */
    @Transaction
    suspend fun refreshCategories(categories: List<CategoriesEntity>) {
        deleteAllCategories()
        insertCategories(categories)
    }
}