package com.vlog.app.data.videos

import android.annotation.SuppressLint
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.vlog.app.data.PaginatedResponse
import com.vlog.app.data.cache.FilterUrlCacheDao
import com.vlog.app.data.cache.FilterUrlCacheEntity
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
    //private val gatherItemDao: GatherItemDao,
    //private val filterUrlCacheDao: FilterUrlCacheDao, // Added
    private val moshi: Moshi // Added
) {

    //private val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

//    // 获取所有视频的Flow
//    fun getAllVideos(): Flow<List<Videos>> {
//        return videoDao.getAllVideos().map { entities ->
//            entities.map { it.toVideos() }
//        }
//    }
    
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
//        // 1. Construct Cache Key
//        val cacheKeyBuilder = StringBuilder("filter_videos_v2")
//        cacheKeyBuilder.append("?typed=").append(typed)
//        cacheKeyBuilder.append("&categoryId=").append(categoryId ?: "null")
//        cacheKeyBuilder.append("&year=").append(year)
//        cacheKeyBuilder.append("&sort=").append(sort)
//        cacheKeyBuilder.append("&page=").append(page)
//        cacheKeyBuilder.append("&pageSize=").append(pageSize)
//        val cacheKey = cacheKeyBuilder.toString()
//
//        val responseType = Types.newParameterizedType(PaginatedResponse::class.java, Videos::class.java)
//        val adapter = moshi.adapter<PaginatedResponse<Videos>>(responseType)
//
//        try {
//            // 2. Delete Expired Cache
//            filterUrlCacheDao.deleteExpiredCache(System.currentTimeMillis() - CACHE_DURATION_MS)
//
//            // 3. Check Cache
//            val cachedEntity = filterUrlCacheDao.getCache(cacheKey)
//            if (cachedEntity != null) {
//                try {
//                    val cachedResponse = adapter.fromJson(cachedEntity.responseDataJson)
//                    if (cachedResponse != null) {
//                        Log.d("VideoRepository", "Returning cached response for $cacheKey")
//                        return Result.success(cachedResponse)
//                    } else {
//                        Log.w("VideoRepository", "Failed to deserialize cached JSON for $cacheKey. Fetching from network.")
//                    }
//                } catch (e: Exception) {
//                    Log.e("VideoRepository", "Error deserializing cached JSON for $cacheKey: ${e.message}. Fetching from network.", e)
//                }
//            }
//        } catch (cacheReadEx: Exception) {
//            Log.e("VideoRepository", "Error accessing filter URL cache: ${cacheReadEx.message}", cacheReadEx)
//            // Proceed to network fetch
//        }
//
//        // 4. Fetch from Network (if cache miss or error)
//        Log.d("VideoRepository", "No valid cache for $cacheKey. Fetching from network.")
        return try {
            val response = videoService.getVideosFilter(
                typed = typed,
                cate = categoryId,
                year = year,
                orderBy = sort,
                page = page,
                size = pageSize
            )

            if (response.code == 200 && response.data != null) {
                val apiResponseData = response.data
//                // Update local DB (existing logic)
                val entities = apiResponseData.items?.map { it.toEntity() } ?: emptyList()
                videoDao.updateVideosWithVersionCheck(entities)
//
//                // Save to Cache
//                try {
//                    val jsonResponse = adapter.toJson(apiResponseData)
//                    val newCacheEntity = FilterUrlCacheEntity(cacheKey, System.currentTimeMillis(), jsonResponse)
//                    filterUrlCacheDao.insertCache(newCacheEntity)
//                    Log.d("VideoRepository", "Saved response to cache for $cacheKey")
//                } catch (cacheWriteEx: Exception) {
//                    Log.e("VideoRepository", "Error saving response to cache for $cacheKey: ${cacheWriteEx.message}", cacheWriteEx)
//                }
                Result.success(apiResponseData)
            } else {
                Result.failure(Exception(response.message ?: "获取视频列表失败 Code: ${response.code}"))
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

//    fun getLocalVideoDetail(videoId: String): Flow<VideoEntity?> {
//        return videoDao.getVideoByIdFlow(videoId)
//    }
//
//
//    ////*************************************************************************************
    fun fetchAndCacheVideoDetail(videoId: String): Flow<Resource<Videos>> = flow {
        emit(Resource.Loading())
        // Use the existing suspend fun getVideoDetail(id: String, token: String?)
        // which already handles the API call and returns Result<Videos>.
        val result: Result<Videos> = getVideoDetail(id = videoId, token = null) // Call existing repo method

        result.fold(
            onSuccess = { videoDto ->
                videoDao.deleteVideoById(videoId)
                val videoEntity = videoDto.toEntity()
                videoEntity.lastRefreshed = System.currentTimeMillis()
                videoDao.insertVideo(videoEntity)
//                try {
//                    //emit(Resource.Success(videoEntity))
//                    //
//                } catch (dbException: Exception) {
//                    emit(Resource.Error("Failed to save video detail to DB: ${dbException.message}", null))
//                }
                emit(Resource.Success(videoDto))
                return@flow
            },
            onFailure = { exception ->
                emit(Resource.Error("Failed to fetch video detail: ${exception.message}", null))
            }
        )
    }.flowOn(Dispatchers.IO)
//
//
    fun getGatherList(videoId: String): Flow<Resource<List<GatherList>>> = flow {
        emit(Resource.Loading())
        // Using injected moshi instance now
        //val listType = Types.newParameterizedType(List::class.java, GatherList::class.java)
        //val gatherListAdapter = moshi.adapter<List<GatherList>>(listType)

        try {
//            val cachedEntity = gatherItemDao.getGatherItemSync(videoId)
//            val currentTime = System.currentTimeMillis()
//
//            if (cachedEntity != null) {
//                if (currentTime - cachedEntity.lastUpdated < 3600000L) { // 1 hour in milliseconds
//                    if (!cachedEntity.gatherListJson.isNullOrBlank()) {
//                        val gatherList: List<GatherList>? = gatherListAdapter.fromJson(cachedEntity.gatherListJson?:"""{}""")
//                        if (gatherList != null) {
//                            emit(Resource.Success(gatherList))
//                        }
//                    } else {
//                        emit(Resource.Success(emptyList()))
//                    }
//                    return@flow
//                }
//            }
//
//            val gatherListVersion = cachedEntity?.version ?: -1
            val apiResponse = videoService.getGatherList(videoId, -1)
            if (apiResponse.code == 200) {
                val serverGatherItems: GatherItem? = apiResponse.data
                if (serverGatherItems != null && serverGatherItems.gatherList?.isNotEmpty() == true) {
                    //val newVersion = serverGatherItems.version
                    val actualEpisodeList: List<GatherList> = serverGatherItems.gatherList!!
//                    try {
//                        val jsonGatherList = gatherListAdapter.toJson(actualEpisodeList)
//                        val newEntity = GatherItemEntity(
//                            videoId = videoId,
//                            version = newVersion,
//                            gatherListJson = jsonGatherList,
//                            lastUpdated = System.currentTimeMillis()
//                        )
//                        gatherItemDao.insertItem(newEntity)
//                        emit(Resource.Success(actualEpisodeList))
//
//                    } catch (dbException: Exception) {
//                        emit(Resource.Error("Failed to save video detail to DB: ${dbException.message}", null))
//                    }


                    emit(Resource.Success(actualEpisodeList))
                }
                return@flow
            } else { // API error
                emit(Resource.Error("API error code: ${apiResponse.code} - ${apiResponse.message}", null))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error fetching gather list", null))
        }
    }.flowOn(Dispatchers.IO)

    fun getYouLikeMoreVideos(
        categoryId: String,
        tagsCsv: String?,
        regionCsv: String?,
        limit: Int,
        currentVideoId: String
    ): Flow<Resource<List<Videos>>> = flow {
        emit(Resource.Loading())
        try {
            val parsedTags = tagsCsv?.split('/')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val parsedRegions = regionCsv?.split('/')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val videoEntities = videoDao.getSimilarVideos(
                categoryId = categoryId,
                tags = parsedTags,
                regions = parsedRegions,
                limit = limit,
                excludeId = currentVideoId
            )

            val videos = videoEntities.map { it.toVideos() }
            emit(Resource.Success(videos))
        } catch (e: Exception) {
            // Consider logging the exception e
            emit(Resource.Error("Failed to load similar videos: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}