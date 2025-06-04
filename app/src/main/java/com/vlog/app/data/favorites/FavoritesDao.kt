package com.vlog.app.data.favorites

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    
    /**
     * 获取所有订阅记录（不包含关联视频信息）
     */
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun getAllFavoriteVideos(): Flow<List<FavoritesEntity>>
    
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    suspend fun getAllFavoriteVideosSync(): List<FavoritesEntity>
    
    /**
     * 获取所有视频类型的订阅记录（包含关联视频信息）
     * 只查询 quoteType = 11 (VIDEO) 的记录
     */
    @Transaction
    @Query("SELECT * FROM favorites WHERE quoteType = 11 ORDER BY createdAt DESC")
    fun getAllFavoriteVideosWithVideo(): Flow<List<FavoritesWithVideo>>
    
    /**
     * 获取指定视频的订阅记录
     */
    @Query("SELECT * FROM favorites WHERE quoteId = :videoId")
    suspend fun getFavoriteVideoById(videoId: String): FavoritesEntity?
    
    /**
     * 检查视频是否已订阅
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE quoteId = :videoId AND quoteType = 11)")
    suspend fun isVideoFavorite(videoId: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE quoteId = :videoId AND quoteType = 11)")
    fun isVideoFavoriteFlow(videoId: String): Flow<Boolean>
    
    /**
     * 插入订阅记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteVideo(favorite: FavoritesEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteVideos(favorite: List<FavoritesEntity>)

    /**
     * 删除订阅记录
     */
    @Delete
    suspend fun deleteFavoriteVideo(favorite: FavoritesEntity)
    
    @Query("DELETE FROM favorites WHERE quoteId = :videoId AND quoteType = 11")
    suspend fun deleteFavoriteVideoById(videoId: String)
    
    @Query("DELETE FROM favorites")
    suspend fun clearAllFavoriteVideos()
    
    /**
     * 获取订阅数量
     */
    @Query("SELECT COUNT(*) FROM favorites WHERE quoteType = 11")
    suspend fun getFavoriteVideoCount(): Int
    
    @Query("SELECT COUNT(*) FROM favorites WHERE quoteType = 11")
    fun getFavoriteVideoCountFlow(): Flow<Int>
    
    /**
     * 按类型获取订阅记录
     */
    @Query("SELECT * FROM favorites WHERE quoteType = :quoteType ORDER BY createdAt DESC")
    fun getFavoritesByType(quoteType: Int): Flow<List<FavoritesEntity>>
}