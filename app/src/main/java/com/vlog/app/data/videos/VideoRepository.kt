package com.vlog.app.data.videos

import com.vlog.app.data.PaginatedResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val videoService: VideoService,
    private val videoDao: VideoDao
) {

    companion object {
        private const val VIDEO_UPDATE_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
    }
    
    // 获取所有视频的Flow
    fun getAllVideos(): Flow<List<Videos>> {
        return videoDao.getAllVideos().map { entities ->
            entities.map { it.toVideos() }
        }
    }
    
    // 获取筛选后的视频列表
    suspend fun getFilteredVideos(
        typed: Int = 0,
        categoryId: String? = null,
        year: Int = 0,
        sort: Int = 0,
        page: Int = 1,
        pageSize: Int = 24,
        forceRefresh: Boolean = false
    ): Result<PaginatedResponse<Videos>> {
        return try {
            // 如果强制刷新或本地没有数据或者是分页请求，从API获取
            if (forceRefresh || shouldUpdateVideos(typed, categoryId, year) || page > 1) {
                val response = videoService.getVideos(
                    typed = typed,
                    cate = categoryId,
                    year = year,
                    orderBy = sort,
                    page = page,
                    size = pageSize
                )
                
                if (response.code == 200 && response.data != null) {
                    // 更新本地数据库
                    val entities = response.data.items?.map { it.toEntity() } ?: emptyList()
                    videoDao.updateVideosWithVersionCheck(entities)
                    
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取视频列表失败"))
                }
            } else {
                // 从本地数据库获取
                val offset = (page - 1) * pageSize
                val entities = videoDao.getFilteredVideos(
                    type = typed,
                    categoryId = categoryId,
                    year = year,
                    sort = sort,
                    limit = pageSize,
                    offset = offset
                )
                val total = videoDao.getFilteredVideosCount(typed, categoryId, year)
                
                val Videos = entities.map { it.toVideos() }
                val paginatedResponse = PaginatedResponse(
                    items = Videos,
                    total = total,
                    page = page,
                    pageSize = pageSize
                )
                
                Result.success(paginatedResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 搜索视频
    suspend fun searchVideos(
        searchKey: String,
        page: Int = 1,
        pageSize: Int = 24,
        forceRefresh: Boolean = false
    ): Result<PaginatedResponse<Videos>> {
        return try {
            val response = videoService.searchVideos(
                searchKey = searchKey,
                page = page,
                size = pageSize
            )
            
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "搜索失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 获取视频详情
    suspend fun getVideoDetail(
        id: String,
        token: String? = null
    ): Result<Videos> {
        return try {
            val response = videoService.getVideoDetail(
                videoId = id,
                token = token
            )
            
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "获取视频详情失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 判断是否需要更新数据
    private suspend fun shouldUpdateVideos(
        type: Int?,
        categoryId: String?,
        year: Int?
    ): Boolean {
        val minLastRefreshed = videoDao.getMinLastRefreshedTimestamp(type, categoryId, year)
        val currentTime = System.currentTimeMillis()

        return if (minLastRefreshed == null) {
            true // No data, should update
        } else {
            (currentTime - minLastRefreshed) > VIDEO_UPDATE_INTERVAL
        }
    }

}