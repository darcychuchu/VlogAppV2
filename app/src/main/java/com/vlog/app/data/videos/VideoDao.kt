package com.vlog.app.data.videos

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    
    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>
    
    @Query("""
        SELECT * FROM videos 
        WHERE (:type IS NULL OR isTyped = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:year IS NULL OR releasedAt = :year)
        ORDER BY 
        CASE WHEN :sort = 'latest' THEN publishedAt END DESC,
        CASE WHEN :sort = 'hot' THEN CAST(orderSort AS REAL) END DESC,
        CASE WHEN :sort = 'rating' THEN CAST(score AS REAL) END DESC,
        CASE WHEN :sort = 'recommend' THEN CAST(isRecommend AS REAL) END DESC,
        createdAt DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getFilteredVideos(
        type: Int?,
        categoryId: String?,
        year: Int?,
        sort: Int?,
        limit: Int,
        offset: Int
    ): List<VideoEntity>
    
    @Query("""
        SELECT COUNT(*) FROM videos 
        WHERE (:type IS NULL OR isTyped = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:year IS NULL OR releasedAt = :year)
    """)
    suspend fun getFilteredVideosCount(
        type: Int?,
        categoryId: String?,
        year: Int?
    ): Int

    @Query("""
        SELECT MIN(lastRefreshed) FROM videos
        WHERE (:type IS NULL OR isTyped = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:year IS NULL OR releasedAt = :year)
    """)
    suspend fun getMinLastRefreshedTimestamp(
        type: Int?,
        categoryId: String?,
        year: Int?
    ): Long?
    
    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)
    
    @Update
    suspend fun updateVideo(video: VideoEntity)


    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)
    
    @Transaction
    suspend fun replaceAllVideos(videos: List<VideoEntity>) {
        updateVideosWithVersionCheck(videos)
    }
    
    // 根据version更新视频数据
    @Query("SELECT version FROM videos WHERE id = :id")
    suspend fun getVideoVersion(id: String): Int?
    
    @Transaction
    suspend fun updateVideosWithVersionCheck(videos: List<VideoEntity>) {
        videos.forEach { newVideo ->
            val existingVersion = getVideoVersion(newVideo.id)
            if (existingVersion == null) {
                insertVideo(newVideo)
            }else if (newVideo.version > existingVersion){
                updateVideo(newVideo)
            }
        }
    }
}