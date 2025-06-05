package com.vlog.app.data.users

import com.vlog.app.data.ApiResponse
import com.vlog.app.di.Constants
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface UserService {

    @GET(Constants.ENDPOINT_USER_CHECK_NAME)
    suspend fun checkUsername(@Query("username") username: String): ApiResponse<Boolean>

    @GET(Constants.ENDPOINT_USER_CHECK_NICKNAME)
    suspend fun checkNickname(@Query("nickname") nickname: String): ApiResponse<Boolean>

    @POST(Constants.ENDPOINT_USER_LOGIN)
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): ApiResponse<Users>

    @POST(Constants.ENDPOINT_USER_REGISTER)
    suspend fun register(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("nickname") nickname: String
    ): ApiResponse<Users>

    @GET(Constants.ENDPOINT_USER_INFO)
    suspend fun getUserInfo(
        @Path("name") name: String,
        @Path("token") token: String
    ): ApiResponse<Users>

    @POST(Constants.ENDPOINT_USER_UPDATE)
    suspend fun updateUserInfo(
        @Path("name") name: String,
        @Path("token") token: String,
        @Query("nickname") nickname: String,
        @Part("avatar_file") avatarFile: MultipartBody.Part?
    ): ApiResponse<Any>
}
