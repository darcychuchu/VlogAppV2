package com.vlog.app.data.users

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.java

/**
 * 用户会话管理器
 * 负责存储和管理用户登录状态和用户信息
 */
@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * 保存用户信息
     * @param user 用户信息
     */
    fun saveUser(user: Users) {
        val userJson = moshi.adapter(Users::class.java).toJson(user)
        sharedPreferences.edit {
            putString(KEY_USER, userJson)
                .putBoolean(KEY_IS_LOGGED_IN, true)
        }
    }

    /**
     * 获取当前登录用户信息
     * @return 用户信息，如果未登录则返回null
     */
    fun getUser(): Users? {
        val userJson = sharedPreferences.getString(KEY_USER, null) ?: return null
        return try {
            moshi.adapter(Users::class.java).fromJson(userJson)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查用户是否已登录
     * @return 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * 获取用户访问令牌
     * @return 访问令牌，如果未登录则返回null
     */
    fun getAccessToken(): String? {
        return getUser()?.accessToken
    }

    /**
     * 获取用户名
     * @return 用户名，如果未登录则返回null
     */
    fun getUserName(): String? {
        return getUser()?.name
    }

    /**
     * 获取用户昵称
     * @return 用户昵称，如果未登录则返回null
     */
    fun getNickName(): String? {
        return getUser()?.nickName
    }

    /**
     * 获取用户头像
     * @return 用户头像ID，如果未登录则返回null
     */
    fun getUserAvatar(): String? {
        return getUser()?.avatar
    }

    /**
     * 退出登录
     */
    fun logout() {
        sharedPreferences.edit {
            remove(KEY_USER)
                .putBoolean(KEY_IS_LOGGED_IN, false)
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "user_session"
        private const val KEY_USER = "user"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
}