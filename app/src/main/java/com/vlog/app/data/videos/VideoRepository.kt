package com.vlog.app.data.videos

import com.vlog.app.data.PaginatedResponse
import com.vlog.app.data.PaginatedResponse
import com.vlog.app.utils.Resource // Assuming Resource is in com.vlog.app.utils
import com.vlog.app.data.PaginatedResponse
import com.vlog.app.utils.Resource // Assuming Resource is in com.vlog.app.utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val videoService: VideoService,
    private val videoDao: VideoDao,
    private val gatherItemDao: GatherItemDao
) {

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
        pageSize: Int = 24
    ): Result<PaginatedResponse<Videos>> {
        return try {
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
            // Local fetching logic removed - always fetch from API for this function
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

    fun getLocalVideoDetail(videoId: String): Flow<VideoEntity?> {
        return videoDao.getVideoByIdFlow(videoId)
    }

    fun fetchAndCacheVideoDetail(videoId: String): Flow<Resource<VideoEntity>> = flow {
        emit(Resource.Loading())
        // Use the existing suspend fun getVideoDetail(id: String, token: String?)
        // which already handles the API call and returns Result<Videos>.
        val result: Result<Videos> = getVideoDetail(id = videoId, token = null) // Call existing repo method

        result.fold(
            onSuccess = { videoDto ->
                val videoEntity = videoDto.toEntity()
                videoEntity.lastRefreshed = System.currentTimeMillis()
                try {
                    videoDao.insertVideo(videoEntity) // This is a suspend fun
                    // After inserting, we might want to emit the entity from the DB via a new fetch
                    // or trust that the inserted entity is what we want to show immediately.
                    // For simplicity here, emitting the transformed entity directly.
                    emit(Resource.Success(videoEntity))
                } catch (dbException: Exception) {
                    emit(Resource.Error("Failed to save video detail to DB: ${dbException.message}", null))
                }
            },
            onFailure = { exception ->
                emit(Resource.Error("Failed to fetch video detail: ${exception.message}", null))
            }
        )
    }.flowOn(Dispatchers.IO)

    fun getGatherList(videoId: String, currentLocalVersion: String?): Flow<Resource<List<GatherItem>>> = flow {
        emit(Resource.Loading())
        try {
            val apiResponse = videoService.getGatherList(videoId, currentLocalVersion)

            if (apiResponse.code == 200) {
                val gatherItemsDto = apiResponse.data

                if (gatherItemsDto != null && gatherItemsDto.isNotEmpty()) {
                    // New data from API
                    val gatherEntities = gatherItemsDto.map { it.toEntity() } // Assumes GatherItem.toEntity() exists

                    // Save new items (potentially clear old ones first for this videoId)
                    gatherItemDao.deleteGatherItemsForVideo(videoId) // Clear old episodes
                    gatherItemDao.insertAll(gatherEntities)         // Insert new ones

                    // Update the VideoEntity's gatherListVersion
                    // Assuming the API response includes the new version, or we generate one.
                    // For now, let's assume API response doesn't directly give a new version string.
                    // We could use a timestamp or hash of content if needed, or the API *should* provide it.
                    // Let's make a simplifying assumption: if new data is sent, we update version to a new timestamp.
                    // A more robust solution would be for the API to return the new version string.
                    val newVersion = System.currentTimeMillis().toString() // Placeholder for actual versioning
                    videoDao.updateGatherListVersion(videoId, newVersion)

                    emit(Resource.Success(gatherItemsDto))
                } else {
                    // API returned empty list or null, meaning no changes or no data for this version.
                    // Load local data from DAO.
                    // This branch means the server has confirmed currentLocalVersion is up-to-date OR there are no items.
                    val localGatherEntities = gatherItemDao.getGatherItemsForVideo(videoId).firstOrNull() ?: emptyList()
                    val localGatherDtos = localGatherEntities.map { it.toDto() } // Assumes GatherItemEntity.toDto() exists
                    emit(Resource.Success(localGatherDtos))
                }
            } else {
                // API error
                emit(Resource.Error("Failed to fetch gather list: ${apiResponse.message} (Code: ${apiResponse.code})"))
            }
        } catch (e: Exception) {
            // Network or other exception
            emit(Resource.Error("Failed to fetch gather list: ${e.message ?: "Unknown error"}"))
        }
    }.flowOn(Dispatchers.IO)
}