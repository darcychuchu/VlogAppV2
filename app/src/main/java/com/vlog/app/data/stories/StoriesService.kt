package com.vlog.app.data.stories

import com.vlog.app.data.ApiResponse
import com.vlog.app.di.Constants
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface StoriesService {
    // 获取用户动态列表
    @GET(Constants.ENDPOINT_STORY_LIST)
    suspend fun getStoriesList(
        @Path("name") name: String,
        @Query("token") token: String
    ): ApiResponse<List<Stories>>

    // 获取用户动态详情
    @GET(Constants.ENDPOINT_STORY_DETAIL)
    suspend fun getStoriesDetail(
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
        @Part("photo_files") photoFiles: List<MultipartBody.Part>?,
        @Query("title") title: String?,
        @Query("description") description: String?,
        @Query("tags") tags: String?,
        @Query("shareContent") shareContent: String?,
        @Query("shareTyped") shareTyped: Int
    ): ApiResponse<String>

}
