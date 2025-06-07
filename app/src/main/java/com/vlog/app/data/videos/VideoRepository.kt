package com.vlog.app.data.videos

import android.annotation.SuppressLint
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vlog.app.data.PaginatedResponse
import com.vlog.app.data.database.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    @SuppressLint("SuspiciousIndentation")
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
                try {
                    videoEntity.lastRefreshed = System.currentTimeMillis()
                    videoDao.insertVideo(videoEntity)
                    emit(Resource.Success(videoEntity))
                    return@flow
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
                        }
                    } else {
                        emit(Resource.Success(emptyList()))
                    }
                    return@flow
                }
            }

            val gatherListVersion = cachedEntity?.version ?: -1
            val apiResponse = videoService.getGatherList(videoId, gatherListVersion)
            if (apiResponse.code == 200) {
                val serverGatherItems: GatherItem? = apiResponse.data
                if (serverGatherItems != null && serverGatherItems.gatherList?.isNotEmpty() == true) {
                    val newVersion = serverGatherItems.version
                    val actualEpisodeList: List<GatherList> = serverGatherItems.gatherList!!
                    try {
                        val jsonGatherList = gatherListAdapter.toJson(actualEpisodeList)
                        val newEntity = GatherItemEntity(
                            videoId = videoId,
                            version = newVersion,
                            gatherListJson = jsonGatherList,
                            lastUpdated = System.currentTimeMillis()
                        )
                        gatherItemDao.insertItem(newEntity)
                        emit(Resource.Success(actualEpisodeList))

                    } catch (dbException: Exception) {
                        emit(Resource.Error("Failed to save video detail to DB: ${dbException.message}", null))
                    }
                }
                return@flow
            } else { // API error
                emit(Resource.Error("API error code: ${apiResponse.code} - ${apiResponse.message}", null))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error fetching gather list", null))
        }
    }.flowOn(Dispatchers.IO)
}