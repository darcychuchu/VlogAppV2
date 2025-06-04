package com.vlog.app.data.favorites

import com.vlog.app.data.videos.VideoList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFavoriteRepository @Inject constructor(
    private val favoriteVideoDao: FavoriteVideoDao
) {
    
    /**
     * 获取所有收藏视频的Flow
     */
    fun getAllFavoriteVideos(): Flow<List<VideoList>> {
        return favoriteVideoDao.getAllFavoriteVideos().map { entities ->
            entities.map { it.toVideoList() }
        }
    }
    
    /**
     * 获取所有收藏视频（同步方法）
     */
    suspend fun getAllFavoriteVideosSync(): List<VideoList> {
        return favoriteVideoDao.getAllFavoriteVideosSync().map { it.toVideoList() }
    }
    
    /**
     * 检查视频是否已收藏
     */
    suspend fun isVideoFavorite(videoId: String): Boolean {
        return favoriteVideoDao.isVideoFavorite(videoId)
    }

    /**
     * 检查视频是否已收藏（Flow）
     */
    fun isVideoFavoriteFlow(videoId: String): Flow<Boolean> {
        return favoriteVideoDao.isVideoFavoriteFlow(videoId)
    }
    
    /**
     * 添加视频到收藏
     */
    suspend fun addToFavorites(video: VideoList): Result<Unit> {
        return try {
            val favoriteEntity = video.toFavoriteEntity()
            favoriteVideoDao.insertFavoriteVideo(favoriteEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 从收藏中移除视频
     */
    suspend fun removeFromFavorites(videoId: String): Result<Unit> {
        return try {
            favoriteVideoDao.deleteFavoriteVideoById(videoId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(video: VideoList): Result<Boolean> {
        return try {
            val videoId = video.id ?: return Result.failure(IllegalArgumentException("Video ID cannot be null"))
            val isFavorite = isVideoFavorite(videoId)
            
            if (isFavorite) {
                removeFromFavorites(videoId)
                Result.success(false)
            } else {
                addToFavorites(video)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取收藏视频数量
     */
    suspend fun getFavoriteVideoCount(): Int {
        return favoriteVideoDao.getFavoriteVideoCount()
    }
    
    /**
     * 获取收藏视频数量（Flow）
     */
    fun getFavoriteVideoCountFlow(): Flow<Int> {
        return favoriteVideoDao.getFavoriteVideoCountFlow()
    }
    
    /**
     * 清空所有收藏
     */
    suspend fun clearAllFavorites(): Result<Unit> {
        return try {
            favoriteVideoDao.clearAllFavoriteVideos()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}