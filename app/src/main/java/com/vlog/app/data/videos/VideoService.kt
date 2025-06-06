package com.vlog.app.data.videos

import com.vlog.app.data.ApiResponse
import com.vlog.app.data.PaginatedResponse
import com.vlog.app.di.Constants
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VideoService {

    @GET(Constants.ENDPOINT_VIDEO_LIST)
    suspend fun getVideos(
        @Query("typed") typed: Int = 0,
        @Query("cate") cate: String? = null,
        @Query("year") year: Int = 0,
        @Query("order_by") orderBy: Int = 0,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 24,
        @Query("token") token: String? = null
    ): ApiResponse<PaginatedResponse<Videos>>

    @GET(Constants.ENDPOINT_VIDEO_SEARCH)
    suspend fun searchVideos(
        @Query("key") searchKey: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 24,
        @Query("token") token: String? = null
    ): ApiResponse<PaginatedResponse<Videos>>

    @GET(Constants.ENDPOINT_VIDEO_DETAIL)
    suspend fun getVideoDetail(
        @Path("videoId") videoId: String,
        @Query("token") token: String? = null
    ): ApiResponse<Videos>

    // Assuming GatherItem is at com.vlog.app.data.videos.GatherItem
    // Assuming ApiResponse is at com.vlog.app.data.ApiResponse
    // The actual API endpoint path needs to be defined with @GET, e.g., @GET("videos/{id}/gatherlist")
    @GET("videos/{id}/gatherlist") // Example endpoint
    suspend fun getGatherList(
        @Path("id") videoId: String, // Or @Query("videoId") depending on API design
        @Query("version") version: String? // Local version, nullable if client has no version yet
    ): ApiResponse<List<GatherItem>>
}