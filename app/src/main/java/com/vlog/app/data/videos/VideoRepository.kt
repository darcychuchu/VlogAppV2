package com.vlog.app.data.videos

import com.vlog.app.data.PaginatedResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val videoService: VideoService,
    private val videoDao: VideoDao,
    private val categoryDao: CategoryDao
) {
    
    // 获取所有视频的Flow
    fun getAllVideos(): Flow<List<VideoList>> {
        return videoDao.getAllVideos().map { entities ->
            entities.map { it.toVideoList() }
        }
    }
    
    // 获取筛选后的视频列表
    suspend fun getFilteredVideos(
        type: Int = 0,
        categoryId: String? = null,
        year: Int = 0,
        sort: Int = 0,
        page: Int = 1,
        pageSize: Int = 24,
        forceRefresh: Boolean = false
    ): Result<PaginatedResponse<VideoList>> {
        return try {
            // 如果强制刷新或本地没有数据或者是分页请求，从API获取
            if (forceRefresh || shouldFetchFromApi(type, categoryId, year) || page > 1) {
                val response = videoService.getVideoList(
                    typed = type,
                    cate = categoryId,
                    year = year,
                    orderBy = sort,
                    page = page,
                    size = pageSize
                )
                
                if (response.code == 200 && response.data != null) {
                    // 更新本地数据库
                    val entities = response.data.items?.map { it.toEntity() } ?: emptyList()
                    if (page == 1) {
                        // 第一页时，根据version更新数据
                        videoDao.updateVideosWithVersionCheck(entities)
                    } else {
                        // 后续页面直接插入
                        videoDao.insertVideos(entities)
                    }
                    
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取视频列表失败"))
                }
            } else {
                // 从本地数据库获取
                val offset = (page - 1) * pageSize
                val entities = videoDao.getFilteredVideos(
                    type = type,
                    categoryId = categoryId,
                    year = year,
                    sort = sort,
                    limit = pageSize,
                    offset = offset
                )
                val total = videoDao.getFilteredVideosCount(type, categoryId, year)
                
                val videoList = entities.map { it.toVideoList() }
                val paginatedResponse = PaginatedResponse(
                    items = videoList,
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
    
    // 获取分类列表
    suspend fun getCategories(typed: Int, forceRefresh: Boolean = false): Result<List<Categories>> {
        return try {
            // 检查是否需要更新分类数据（24小时后更新）
            if (forceRefresh || categoryDao.shouldUpdateCategories()) {
                val response = videoService.getCategories(typed)
                
                if (response.code == 200 && response.data != null) {
                    // 直接覆盖本地分类数据
                    val entities = response.data.map { it.toEntity() }
                    categoryDao.replaceAllCategories(entities)
                    
                    Result.success(response.data)
                } else {
                    Result.failure(Exception(response.message ?: "获取分类列表失败"))
                }
            } else {
                // 从本地数据库获取
                val entities = categoryDao.getAllCategoriesSync()
                val categories = entities.map { it.toCategories() }
                Result.success(categories)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // 获取分类列表的Flow
    fun getCategoriesFlow(): Flow<List<Categories>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toCategories() }
        }
    }
    
    // 搜索视频
    suspend fun searchVideos(
        searchKey: String,
        page: Int = 1,
        pageSize: Int = 24,
        forceRefresh: Boolean = false
    ): Result<PaginatedResponse<VideoList>> {
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
    ): Result<VideoDetail> {
        return try {
            val response = videoService.getVideoDetail(
                id = id,
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
    
    // 判断是否需要从API获取数据
    private suspend fun shouldFetchFromApi(
        type: Int?,
        categoryId: String?,
        year: Int?
    ): Boolean {
        // 检查本地是否有对应筛选条件的数据
        val count = videoDao.getFilteredVideosCount(type, categoryId, year)
        return count == 0
    }
    
    // 清除所有视频数据
    suspend fun clearAllVideos() {
        videoDao.clearAllVideos()
    }
    
    // 清除所有分类数据
    suspend fun clearAllCategories() {
        categoryDao.clearAllCategories()
    }
}