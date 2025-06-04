package com.vlog.app.data.favorites

import com.vlog.app.data.ApiResponse
import com.vlog.app.data.videos.VideoDetail
import com.vlog.app.di.Constants
import retrofit2.http.*

interface FavoriteService {
    
    /**
     * 获取用户订阅列表
     * @param username 用户名
     * @param token 用户token
     */
    @GET(Constants.ENDPOINT_FAVORITES_LIST)
    suspend fun getFavorites(
        @Path("username") username: String,
        @Query("token") token: String
    ): ApiResponse<List<Favorites>>
    
    /**
     * 订阅视频
     * @param videoId 视频ID
     * @param name 用户名
     * @param token 用户token
     */
    @POST(Constants.ENDPOINT_FAVORITES_CREATE)
    suspend fun createFavorite(
        @Path("videoId") videoId: String,
        @Query("name") name: String,
        @Query("token") token: String
    ): ApiResponse<Favorites>
    
    /**
     * 移除订阅视频
     * @param videoId 视频ID
     * @param name 用户名
     * @param token 用户token
     */
    @POST(Constants.ENDPOINT_FAVORITES_REMOVE)
    suspend fun removeFavorite(
        @Path("videoId") videoId: String,
        @Query("name") name: String,
        @Query("token") token: String
    ): ApiResponse<String>
    
    /**
     * 更新订阅视频数据
     * @param username 用户名
     * @param token 用户token
     */
    @POST(Constants.ENDPOINT_FAVORITES_UPDATE)
    suspend fun updateFavoriteVideos(
        @Path("username") username: String,
        @Query("token") token: String
    ): ApiResponse<List<VideoDetail>>
}