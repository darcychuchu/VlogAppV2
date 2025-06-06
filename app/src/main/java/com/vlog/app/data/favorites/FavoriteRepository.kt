package com.vlog.app.data.favorites

import android.util.Log
import com.vlog.app.data.users.UserSessionManager
import com.vlog.app.data.videos.VideoDao
import com.vlog.app.data.videos.VideoEntity
import com.vlog.app.data.videos.Videos
import com.vlog.app.data.videos.toEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 订阅仓库
 * 基于 Room 数据库作为单一数据源，参考观看历史的实现模式
 */
@Singleton
class FavoriteRepository @Inject constructor(
    private val favoriteService: FavoriteService,
    private val favoritesDao: FavoritesDao,
    private val userSessionManager: UserSessionManager,
    private val videoDao: VideoDao
) {
    
    // 最后更新时间（用于5分钟限制）
    private var lastUpdateTime = 0L
    private val UPDATE_INTERVAL_MS = 5 * 60 * 1000L // 5分钟
    
    /**
     * 获取所有视频订阅记录（包含关联视频信息）
     */
    fun getAllFavoriteVideosWithVideo(): Flow<List<FavoritesWithVideo>> {
        return favoritesDao.getAllFavoriteVideosWithVideo()
    }
    
    /**
     * 获取所有订阅记录（不包含关联视频信息）
     */
    fun getAllFavoriteVideos(): Flow<List<FavoritesEntity>> {
        return favoritesDao.getAllFavoriteVideos()
    }
    
    /**
     * 检查视频是否已订阅
     */
    suspend fun isVideoFavorite(videoId: String): Boolean {
        return favoritesDao.isVideoFavorite(videoId)
    }
    
    /**
     * 检查视频是否已订阅（Flow版本）
     */
    fun isVideoFavoriteFlow(videoId: String): Flow<Boolean> {
        return favoritesDao.isVideoFavoriteFlow(videoId)
    }
    
    /**
     * 获取订阅数量
     */
    fun getFavoriteVideoCountFlow(): Flow<Int> {
        return favoritesDao.getFavoriteVideoCountFlow()
    }
    
    /**
     * 从服务器同步订阅列表到本地数据库
     */
    suspend fun syncFavoritesFromServer(username: String, token: String): Result<List<Favorites>> {
        return try {
            val response = favoriteService.getFavorites(username, token)
            if (response.code == 200) {
                val favoritesList = response.data ?: emptyList()
                // 将服务器数据转换为本地实体并保存
                val entities = favoritesList.map { it.toEntity() }
                favoritesDao.insertFavoriteVideos(entities)

                Log.d("3---",entities.toString())
                // 同步视频详细信息到VideoEntity表
                val videoDetailsResponse = favoriteService.updateFavoriteVideos(username, token)
                Log.d("4---",videoDetailsResponse.toString())
                if (videoDetailsResponse.code == 200) {
                    val videoDetailsList = videoDetailsResponse.data ?: emptyList()
                    val videoEntities = videoDetailsList.map { it.toEntity() }
                    videoDao.updateVideosWithVersionCheck(videoEntities)
                }
                
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
                // 将服务器返回的数据保存到本地数据库
                response.data?.let { favorite ->
                    val entity = favorite.toEntity()
                    favoritesDao.insertFavoriteVideo(entity)
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
                // 从本地数据库删除
                favoritesDao.deleteFavoriteVideoById(videoId)
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "移除订阅失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 同步订阅视频数据
     * 适合老用户，用原账户登录，同步功能
     * 返回的List<VideoDetail> --> 比对Videos的本地表数据，写入没有的video Id数据
     * 限制5分钟才能执行一次
     */
    suspend fun updateFavoriteVideos(username: String, token: String, forceUpdate: Boolean = false): Result<List<Videos>> {
        // 检查更新间隔限制
        val currentTime = System.currentTimeMillis()
//        if (!forceUpdate && (currentTime - lastUpdateTime) < UPDATE_INTERVAL_MS) {
//            return Result.failure(Exception("请等待 ${(UPDATE_INTERVAL_MS - (currentTime - lastUpdateTime)) / 1000} 秒后再更新"))
//        }
        
        return try {
            val response = favoriteService.updateFavoriteVideos(username, token)
            Log.d("3----",response.toString())
            if (response.code == 200) {
                val videoDetailsList = response.data ?: emptyList()
                
                // 比对本地Videos表数据，写入缺失的视频数据
                val newVideoEntities = mutableListOf<VideoEntity>()
                val newFavoriteEntities = mutableListOf<FavoritesEntity>()
                
                videoDetailsList.forEach { videoDetail ->
                    // 检查本地是否已存在该视频
                    val videoId = videoDetail.id ?: ""
                    if (videoId.isNotEmpty()) {
                        val existingVideo = videoDao.getVideoById(videoId)
                        if (existingVideo == null) {
                            // 如果本地不存在，添加到待插入列表
                            newVideoEntities.add(videoDetail.toEntity())
                        }
                        
                        // 检查是否已订阅该视频
                        val existingFavorite = favoritesDao.getFavoriteVideoById(videoId)
                        if (existingFavorite == null) {
                            // 如果未订阅，创建订阅记录
                            val favoriteEntity = FavoritesEntity(
                                id = "", // Room会自动生成ID
                                quoteId = videoId,
                                quoteType = 11, // 视频类型
                                createdBy = username,
                                createdAt = System.currentTimeMillis()
                            )
                            newFavoriteEntities.add(favoriteEntity)
                        }
                    }
                }
                
                // 批量插入新视频数据
                if (newVideoEntities.isNotEmpty()) {
                    videoDao.updateVideosWithVersionCheck(newVideoEntities)
                }
                
                // 批量插入新订阅记录
                if (newFavoriteEntities.isNotEmpty()) {
                    favoritesDao.insertFavoriteVideos(newFavoriteEntities)
                }
                
                lastUpdateTime = currentTime
                Result.success(videoDetailsList)
            } else {
                Result.failure(Exception(response.message ?: "同步订阅视频数据失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        lastUpdateTime = 0L
    }
}



fun Favorites.toEntity(): FavoritesEntity {
    return FavoritesEntity(
        id = this.id ?: "",
        quoteId = this.quoteId,
        quoteType = 11, // 视频类型
        createdBy = this.createdBy,
        createdAt = this.createdAt
    )
}

fun FavoritesEntity.toFavorites(): Favorites {
    return Favorites(
        id = this.id,
        quoteId = this.quoteId,
        createdBy = this.createdBy,
        createdAt = this.createdAt
    )
}