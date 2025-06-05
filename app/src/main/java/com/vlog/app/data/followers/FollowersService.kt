package com.vlog.app.data.followers

//import com.vlog.app.data.ApiResponse
//import retrofit2.http.GET
//import retrofit2.http.Path
//import retrofit2.http.Query
//
///**
// * 关注相关的API服务接口
// */
//interface FollowersService {
//    /**
//     * 获取我关注的用户列表
//     */
//    @GET("{name}/following")
//    suspend fun getFollowing(
//        @Path("name") name: String,
//        @Query("token") token: String? = null
//    ): ApiResponse<List<Followers>>
//
//    /**
//     * 获取关注我的用户列表（粉丝）
//     */
//    @GET("{name}/followers")
//    suspend fun getFollowers(
//        @Path("name") name: String,
//        @Query("token") token: String? = null
//    ): ApiResponse<List<Followers>>
//
//    /**
//     * 关注用户
//     */
//    @GET("{name}/followers-created")
//    suspend fun followUser(
//        @Path("name") name: String,
//        @Query("token") token: String,
//        @Query("userId") userId: String
//    ): ApiResponse<Any>
//
//    /**
//     * 取消关注用户
//     */
//    @GET("{name}/followers-removed")
//    suspend fun unfollowUser(
//        @Path("name") name: String,
//        @Query("token") token: String,
//        @Query("userId") userId: String
//    ): ApiResponse<Any>
//}
