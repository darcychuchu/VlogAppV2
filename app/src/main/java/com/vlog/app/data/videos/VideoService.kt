package com.vlog.app.data.videos

import com.vlog.app.data.ApiResponse
import com.vlog.app.data.PaginatedResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface VideoService {
    
    @GET("videos/categories/{typed}")
    suspend fun getCategories(
        @Path("typed") typed: Int = 0
    ): ApiResponse<List<Categories>>
    
    @GET("videos/list")
    suspend fun getVideoList(
        @Query("typed") typed: Int = 0,
        @Query("cate") cate: String? = null,
        @Query("year") year: Int = 0,
        @Query("order_by") orderBy: Int = 0,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 24,
        @Query("token") token: String? = null
    ): ApiResponse<PaginatedResponse<VideoList>>
    
    @GET("videos/search")
    suspend fun searchVideos(
        @Query("key") searchKey: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 24,
        @Query("token") token: String? = null
    ): ApiResponse<PaginatedResponse<VideoList>>
    
    @GET("videos/detail/{id}")
    suspend fun getVideoDetail(
        @Path("id") id: String,
        @Query("token") token: String? = null
    ): ApiResponse<VideoDetail>
}