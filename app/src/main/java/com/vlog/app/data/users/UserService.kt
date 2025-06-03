package com.vlog.app.data.users

import com.vlog.app.data.ApiResponse
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface UserService {
    @GET("users/stated-name")
    suspend fun checkUsername(@Query("username") username: String): ApiResponse<Boolean>

    @GET("users/stated-nickname")
    suspend fun checkNickname(@Query("nickname") nickname: String): ApiResponse<Boolean>

    @POST("users/login")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): ApiResponse<Users>

    @POST("users/register")
    suspend fun register(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("nickname") nickname: String
    ): ApiResponse<Users>

    @GET("users/stated-me/{name}/{token}")
    suspend fun getUserInfo(
        @Path("name") name: String,
        @Path("token") token: String
    ): ApiResponse<Users>

    @POST("users/updated/{name}/{token}")
    suspend fun updateUserInfo(
        @Path("name") name: String,
        @Path("token") token: String,
        @Query("nickname") nickname: String,
        @Query("avatar_file") avatarFile: MultipartBody.Part
    ): ApiResponse<Any>
}
