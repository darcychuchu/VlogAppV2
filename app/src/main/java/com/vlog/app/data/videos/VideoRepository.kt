package com.vlog.app.data.videos

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vlog.app.data.PaginatedResponse
import com.vlog.app.data.database.Resource
import com.vlog.app.data.videos.GatherItemEntity
import com.vlog.app.data.videos.GatherList
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
    
    // 获取筛选后的视频列表******************************************************************
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
                size = pageSize)
                
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


    ////*************************************************************************************
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


    fun getGatherList(videoId: String): Flow<Resource<List<GatherList>>> = flow {
        emit(Resource.Loading())
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val listType = Types.newParameterizedType(List::class.java, GatherList::class.java)
        val gatherListAdapter = moshi.adapter<List<GatherList>>(listType)

        try {
            val cachedEntity = gatherItemDao.getGatherItemSync(videoId)
            val currentTime = System.currentTimeMillis()

            if (cachedEntity != null) {
                if (currentTime - cachedEntity.lastUpdated < 3600000L) { // 1 hour in milliseconds
                    if (!cachedEntity.gatherListJson.isNullOrBlank()) {
                        val gatherList: List<GatherList>? = gatherListAdapter.fromJson(cachedEntity.gatherListJson)
                        if (gatherList != null) {
                            emit(Resource.Success(gatherList))
                            return@flow // Data served from fresh cache
                        } else {
                            // Error deserializing, proceed to fetch
                        }
                    } else { // Cached but no JSON
                        emit(Resource.Success(emptyList())) // Or handle as error/refetch
                        return@flow
                    }
                }
                // Cache is stale, proceed to API call
            }

            val localVersion = cachedEntity?.version ?: 0
            val apiResponse = videoService.getGatherList(videoId, localVersion)

            if (apiResponse.code == 200) {
                val serverGatherItems: List<GatherItem>? = apiResponse.data
                if (!serverGatherItems.isNullOrEmpty()) { // New data from API
                    val firstServerItem = serverGatherItems.first()
                    val newVersion = firstServerItem.version
                    val actualEpisodeList: List<GatherList>? = firstServerItem.gatherList

                    if (actualEpisodeList != null) {
                        val jsonGatherList = gatherListAdapter.toJson(actualEpisodeList)
                        val newEntity = GatherItemEntity(
                            videoId = videoId,
                            version = newVersion,
                            gatherListJson = jsonGatherList,
                            lastUpdated = System.currentTimeMillis()
                        )
                        gatherItemDao.insertItem(newEntity)
                        emit(Resource.Success(actualEpisodeList))
                    } else { // Server returned item but no actual episode list
                        emit(Resource.Error("No episode list in API response", null))
                    }
                } else { // API returned empty list (versions match or no data)
                    if (cachedEntity != null && !cachedEntity.gatherListJson.isNullOrBlank()) {
                        // Update lastUpdated timestamp for the existing cache
                        val updatedCachedEntity = cachedEntity.copy(lastUpdated = System.currentTimeMillis())
                        gatherItemDao.insertItem(updatedCachedEntity)
                        val gatherList: List<GatherList>? = gatherListAdapter.fromJson(cachedEntity.gatherListJson)
                         if (gatherList != null) {
                            emit(Resource.Success(gatherList))
                        } else {
                             emit(Resource.Success(emptyList())) // Error deserializing cached data
                        }
                    } else { // No new data and no valid cache
                        emit(Resource.Success(emptyList()))
                    }
                }
            } else { // API error
                emit(Resource.Error("API error code: ${apiResponse.code} - ${apiResponse.message}", null))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error fetching gather list", null))
        }
    }.flowOn(Dispatchers.IO)
}