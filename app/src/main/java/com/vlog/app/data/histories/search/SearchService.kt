package com.vlog.app.data.histories.search

import com.vlog.app.data.ApiResponse
import com.vlog.app.data.videos.VideoList
import com.vlog.app.di.Constants
import retrofit2.http.*

interface SearchService {
    /**
     * 搜索视频
     * @param key 搜索关键词，为空时返回热门搜索
     * @return 搜索结果列表
     */
    @GET(Constants.ENDPOINT_SEARCH)
    suspend fun searchVideos(
        @Query("key") key: String? = null
    ): ApiResponse<List<VideoList>>
}
