package com.vlog.app.data.favorites

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteVideoDao {
    
    @Query("SELECT * FROM favorite_videos ORDER BY createdAt DESC")
    fun getAllFavoriteVideos(): Flow<List<FavoriteVideoEntity>>
    
    @Query("SELECT * FROM favorite_videos ORDER BY createdAt DESC")
    suspend fun getAllFavoriteVideosSync(): List<FavoriteVideoEntity>
    
    @Query("SELECT * FROM favorite_videos WHERE videoId = :videoId")
    suspend fun getFavoriteVideoById(videoId: String): FavoriteVideoEntity?
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_videos WHERE videoId = :videoId)")
    suspend fun isVideoFavorite(videoId: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_videos WHERE videoId = :videoId)")
    fun isVideoFavoriteFlow(videoId: String): Flow<Boolean>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteVideo(favoriteVideo: FavoriteVideoEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteVideos(favoriteVideos: List<FavoriteVideoEntity>)
    
    @Delete
    suspend fun deleteFavoriteVideo(favoriteVideo: FavoriteVideoEntity)
    
    @Query("DELETE FROM favorite_videos WHERE videoId = :videoId")
    suspend fun deleteFavoriteVideoById(videoId: String)
    
    @Query("DELETE FROM favorite_videos")
    suspend fun clearAllFavoriteVideos()
    
    @Query("SELECT COUNT(*) FROM favorite_videos")
    suspend fun getFavoriteVideoCount(): Int
    
    @Query("SELECT COUNT(*) FROM favorite_videos")
    fun getFavoriteVideoCountFlow(): Flow<Int>
}