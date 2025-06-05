package com.vlog.app.data.followers

//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.flow
//import javax.inject.Inject
//import javax.inject.Singleton
//
///**
// * 关注相关的仓库
// */
//@Singleton
//class FollowersRepository @Inject constructor(
//    private val followersService: FollowersService
//) {
//    /**
//     * 获取我关注的用户列表
//     */
//    fun getFollowing(name: String, token: String?): Flow<ApiResponse<List<Followers>>> = flow {
//        emit(AppResponse.Loading())
//        try {
//            val response = followersService.getFollowing(name, token)
//            if (response.code == ApiResponseCode.SUCCESS) {
//                emit(AppResponse.Success(response.data ?: emptyList()))
//            } else {
//                emit(AppResponse.Error(response.message ?: "获取关注列表失败"))
//            }
//        } catch (e: Exception) {
//            emit(AppResponse.Error(e.message ?: "获取关注列表失败"))
//        }
//    }
//
//    /**
//     * 获取关注我的用户列表（粉丝）
//     */
//    fun getFollowers(name: String, token: String?): Flow<AppResponse<List<Followers>>> = flow {
//        emit(AppResponse.Loading())
//        try {
//            val response = followersService.getFollowers(name, token)
//            if (response.code == ApiResponseCode.SUCCESS) {
//                emit(AppResponse.Success(response.data ?: emptyList()))
//            } else {
//                emit(AppResponse.Error(response.message ?: "获取粉丝列表失败"))
//            }
//        } catch (e: Exception) {
//            emit(AppResponse.Error(e.message ?: "获取粉丝列表失败"))
//        }
//    }
//
//    /**
//     * 关注用户
//     */
//    fun followUser(name: String, token: String, userId: String): Flow<AppResponse<Boolean>> = flow {
//        emit(AppResponse.Loading())
//        try {
//            val response = followersService.followUser(name, token, userId)
//            if (response.code == ApiResponseCode.SUCCESS) {
//                emit(AppResponse.Success(true))
//            } else {
//                emit(AppResponse.Error(response.message ?: "关注用户失败"))
//            }
//        } catch (e: Exception) {
//            emit(AppResponse.Error(e.message ?: "关注用户失败"))
//        }
//    }
//
//    /**
//     * 取消关注用户
//     */
//    fun unfollowUser(name: String, token: String, userId: String): Flow<AppResponse<Boolean>> = flow {
//        emit(AppResponse.Loading())
//        try {
//            val response = followersService.unfollowUser(name, token, userId)
//            if (response.code == ApiResponseCode.SUCCESS) {
//                emit(AppResponse.Success(true))
//            } else {
//                emit(AppResponse.Error(response.message ?: "取消关注失败"))
//            }
//        } catch (e: Exception) {
//            emit(AppResponse.Error(e.message ?: "取消关注失败"))
//        }
//    }
//}
