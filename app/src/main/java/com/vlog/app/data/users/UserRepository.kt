package com.vlog.app.data.users

import com.vlog.app.data.ApiResponse
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userService: UserService
) {
    suspend fun checkUsername(username: String): ApiResponse<Boolean> {
        return userService.checkUsername(username)
    }

    suspend fun checkNickname(nickname: String): ApiResponse<Boolean> {
        return userService.checkNickname(nickname)
    }

    suspend fun login(username: String, password: String): ApiResponse<Users> {
        return userService.login(username, password)
    }

    suspend fun register(username: String, password: String, nickname: String): ApiResponse<Users> {
        return userService.register(username, password, nickname)
    }

    suspend fun getUserInfo(name: String, token: String): ApiResponse<Users> {
        return userService.getUserInfo(name, token)
    }

    suspend fun updateUserInfo(
        name: String,
        token: String,
        nickname: String,
        avatarFile: MultipartBody.Part
    ): ApiResponse<Any> {
        return userService.updateUserInfo(name, token, nickname, avatarFile)
    }
}
