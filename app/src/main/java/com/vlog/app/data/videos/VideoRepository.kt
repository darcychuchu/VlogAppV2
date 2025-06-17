package com.vlog.app.data.videos

import android.annotation.SuppressLint
import com.squareup.moshi.Moshi
import com.vlog.app.data.PaginatedResponse
import com.vlog.app.data.database.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val videoService: VideoService,
    private val videoDao: VideoDao,
    //private val filterUrlCacheDao: FilterUrlCacheDao, // Added
    private val moshi: Moshi // Added
) {

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
                val entities = apiResponseData.items?.map { it.toEntity() } ?: emptyList()
                videoDao.updateVideosWithVersionCheck(entities)
                Result.success(apiResponseData)
            } else {
                Result.failure(Exception(response.message ?: "获取视频列表失败 Code: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

    fun fetchAndCacheVideoDetail(videoId: String): Flow<Resource<Videos>> = flow {
        emit(Resource.Loading())
        val result: Result<Videos> = getVideoDetail(id = videoId, token = null) // Call existing repo method

        result.fold(
            onSuccess = { videoDto ->
                videoDao.deleteVideoById(videoId)
                val videoEntity = videoDto.toEntity()
                videoEntity.lastRefreshed = System.currentTimeMillis()
                videoDao.insertVideo(videoEntity)
                emit(Resource.Success(videoDto))
                return@flow
            },
            onFailure = { exception ->
                emit(Resource.Error("Failed to fetch video detail: ${exception.message}", null))
            }
        )
    }.flowOn(Dispatchers.IO)

    fun getGatherList(videoId: String): Flow<Resource<List<GatherList>>> = flow {
        emit(Resource.Loading())
        try {
            val apiResponse = videoService.getGatherList(videoId, -1)
            if (apiResponse.code == 200) {
                if (apiResponse.data != null && apiResponse.data.gatherList?.isNotEmpty() == true) {
                    val actualEpisodeList: List<GatherList> = apiResponse.data.gatherList!!
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
            emit(Resource.Error("Failed to load similar videos: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}