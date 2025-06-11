package com.vlog.app.data.stories

import com.vlog.app.data.ApiResponse
import com.vlog.app.data.PaginatedResponse
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoriesRepository @Inject constructor(
    private val storiesService: StoriesService
) {

    // 获取全局动态列表
    suspend fun getGlobalStoriesList(typed: Int, page: Int, size: Int, orderBy: Int, token: String?): ApiResponse<PaginatedResponse<Stories>> {
        return storiesService.getGlobalStoriesList(typed, page, size, orderBy, token)
    }



    // 获取用户动态列表
    suspend fun getUserStoriesList(name: String, typed: Int, page: Int, size: Int, orderBy: Int, token: String?): ApiResponse<PaginatedResponse<Stories>> {
        return storiesService.getUserStoriesList(name, typed, page, size, orderBy, token)
    }

    // 获取用户动态详情
    suspend fun getStoriesDetail(name: String, id: String, token: String?): ApiResponse<Stories> {
        return storiesService.getUserStoriesDetail(name, id, token)
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
