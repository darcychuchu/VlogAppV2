package com.vlog.app.data.stories

import com.vlog.app.data.ApiResponse
import com.vlog.app.data.PaginatedResponse
import com.vlog.app.di.Constants
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface StoriesService {
    // 获取全局动态列表
    @GET(Constants.ENDPOINT_GLOBAL_STORY_LIST)
    suspend fun getGlobalStoriesList(
        @Query("typed") typed: Int,  // typed = 0
        @Query("page") page: Int,    // page = 1
        @Query("size") size: Int,    // 12>= size <=24
        @Query("order_by") orderBy: Int,    // 0 = ORDER BY CREATED AT  || 1 = ORDER BY COMMENTED  || 2 = ORDER BY HOT || 3 = ORDER BY RECOMMEND
        @Query("token") token: String?
    ): ApiResponse<PaginatedResponse<Stories>>


    // 获取用户动态列表
    @GET(Constants.ENDPOINT_STORY_LIST)
    suspend fun getUserStoriesList(
        @Path("name") name: String,
        @Query("typed") typed: Int,  // typed = 0
        @Query("page") page: Int,    // page = 1
        @Query("size") size: Int,    // 12>= size <=24
        @Query("order_by") orderBy: Int,    // 0 = ORDER BY CREATED AT  || 1 = ORDER BY COMMENTED  || 2 = ORDER BY HOT || 3 = ORDER BY RECOMMEND
        @Query("token") token: String?
    ): ApiResponse<PaginatedResponse<Stories>>


    // 获取用户动态详情
    @GET(Constants.ENDPOINT_STORY_DETAIL)
    suspend fun getUserStoriesDetail(
        @Path("name") name: String,
        @Path("storyId") storyId: String,
        @Query("token") token: String?
    ): ApiResponse<Stories>


    // 发布用户动态
    @Multipart
    @POST(Constants.ENDPOINT_STORY_CREATE)
    suspend fun createStories(
        @Path("name") name: String,
        @Query("token") token: String,
        @Part photoFiles: List<MultipartBody.Part>?,
        @Query("title") title: String?,
        @Query("description") description: String?,
        @Query("tags") tags: String?,
        @Query("shareContent") shareContent: String?,
        @Query("shareTyped") shareTyped: Int
    ): ApiResponse<String>

}
