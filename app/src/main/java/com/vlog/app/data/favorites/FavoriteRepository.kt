package com.vlog.app.data.favorites


import com.vlog.app.data.videos.VideoDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteService: FavoriteService
) {
    
    // 订阅列表缓存
    private val _favorites = MutableStateFlow<List<Favorites>>(emptyList())
    val favorites: Flow<List<Favorites>> = _favorites.asStateFlow()
    
    // 订阅视频详细数据缓存
    private val _favoriteVideos = MutableStateFlow<List<VideoDetail>>(emptyList())
    val favoriteVideos: Flow<List<VideoDetail>> = _favoriteVideos.asStateFlow()
    
    // 最后更新时间（用于5分钟限制）
    private var lastUpdateTime = 0L
    private val UPDATE_INTERVAL_MS = 5 * 60 * 1000L // 5分钟
    
    /**
     * 获取用户订阅列表
     */
    suspend fun getFavorites(username: String, token: String): Result<List<Favorites>> {
        return try {
            val response = favoriteService.getFavorites(username, token)
            if (response.code == 200) {
                val favoritesList = response.data ?: emptyList()
                _favorites.value = favoritesList
                Result.success(favoritesList)
            } else {
                Result.failure(Exception(response.message ?: "获取订阅列表失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 订阅视频
     */
    suspend fun createFavorite(videoId: String, name: String, token: String): Result<Unit> {
        return try {
            val response = favoriteService.createFavorite(videoId, name, token)
            if (response.code == 200) {
                // 更新本地缓存
                val currentFavorites = _favorites.value.toMutableList()
                // 检查是否已存在，避免重复添加
                if (!currentFavorites.any { it.quoteId == videoId }) {
                    currentFavorites.add(Favorites(quoteId = videoId, createdBy = name))
                    _favorites.value = currentFavorites
                }
                
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "订阅失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 移除订阅视频
     */
    suspend fun removeFavorite(videoId: String, name: String, token: String): Result<Unit> {
        return try {
            val response = favoriteService.removeFavorite(videoId, name, token)
            if (response.code == 200) {
                // 更新本地缓存
                val currentFavorites = _favorites.value.toMutableList()
                currentFavorites.removeAll { it.quoteId == videoId }
                _favorites.value = currentFavorites
                
                // 同时移除对应的视频详细数据
                val currentVideos = _favoriteVideos.value.toMutableList()
                currentVideos.removeAll { it.id == videoId }
                _favoriteVideos.value = currentVideos
                
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "移除订阅失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新订阅视频数据
     * 限制5分钟才能执行一次
     */
    suspend fun updateFavoriteVideos(username: String, token: String, forceUpdate: Boolean = false): Result<List<VideoDetail>> {
        // 检查更新间隔限制
        val currentTime = System.currentTimeMillis()
        if (!forceUpdate && (currentTime - lastUpdateTime) < UPDATE_INTERVAL_MS) {
            return Result.failure(Exception("请等待 ${(UPDATE_INTERVAL_MS - (currentTime - lastUpdateTime)) / 1000} 秒后再更新"))
        }
        
        return try {
            val response = favoriteService.updateFavoriteVideos(username, token)
            if (response.code == 200) {
                val videosList = response.data ?: emptyList()
                _favoriteVideos.value = videosList
                lastUpdateTime = currentTime
                Result.success(videosList)
            } else {
                Result.failure(Exception(response.message ?: "更新订阅视频数据失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 检查视频是否已订阅
     */
    fun isVideoFavorited(videoId: String): Boolean {
        // 同时检查订阅列表和视频详情列表，确保状态准确
        val inFavorites = _favorites.value.any { it.quoteId == videoId }
        val inVideos = _favoriteVideos.value.any { it.id == videoId }
        return inFavorites || inVideos
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        _favorites.value = emptyList()
        _favoriteVideos.value = emptyList()
        lastUpdateTime = 0L
    }
}