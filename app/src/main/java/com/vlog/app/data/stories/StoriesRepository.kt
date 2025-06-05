package com.vlog.app.data.stories

import com.squareup.moshi.Moshi
import com.vlog.app.data.ApiResponse
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoriesRepository @Inject constructor(
    private val storiesService: StoriesService,
    private val moshi: Moshi
) {
    // 获取用户动态列表
    suspend fun getStoriesList(name: String, token: String): ApiResponse<List<Stories>> {
        return storiesService.getStoriesList(name, token)
    }

    // 获取用户动态详情
    suspend fun getStoriesDetail(name: String, id: String, token: String?): ApiResponse<Stories> {
        return storiesService.getStoriesDetail(name, id, token)
    }

    // 发布用户动态
    suspend fun createStories(
        name: String,
        token: String,
        photoFiles: List<MultipartBody.Part>?,
        title: String?,
        description: String?,
        tags: String?,
        shareContent: String? = null,
        shareTyped: Int = 0
    ): ApiResponse<String> {
        return storiesService.createStories(name, token, photoFiles, title, description, tags, shareContent, shareTyped)
    }

}
