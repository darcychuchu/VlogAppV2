package com.vlog.app.data.users

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户数据仓库
 * 提供用户数据的访问和修改功能
 * 这个仓库用于在不同的ViewModel之间共享用户数据
 */
@Singleton
class UserDataRepository @Inject constructor(
    private val userSessionManager: UserSessionManager
) {
    // 当前用户
    private val _currentUser = MutableStateFlow<Users?>(userSessionManager.getUser())
    val currentUser: StateFlow<Users?> = _currentUser.asStateFlow()
    
    /**
     * 获取当前用户
     * @return 当前用户，如果未登录则返回null
     */
    fun getCurrentUser(): Users? {
        return _currentUser.value
    }
    
    /**
     * 更新当前用户
     * @param user 用户信息
     */
    fun updateCurrentUser(user: Users?) {
        _currentUser.value = user
        if (user != null) {
            userSessionManager.saveUser(user)
        } else {
            userSessionManager.logout()
        }
    }
    
    /**
     * 检查是否已登录
     * @return 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return userSessionManager.isLoggedIn()
    }
    
    /**
     * 获取用户名
     * @return 用户名，如果未登录则返回null
     */
    fun getUserName(): String? {
        return userSessionManager.getUserName()
    }
    
    /**
     * 获取访问令牌
     * @return 访问令牌，如果未登录则返回null
     */
    fun getAccessToken(): String? {
        return userSessionManager.getAccessToken()
    }
    
    /**
     * 扣除积分
     * @param points 要扣除的积分数量
     * @return 是否扣除成功
     */
    fun deductPoints(points: Int): Boolean {
        val user = _currentUser.value ?: return false
        val currentPoints = user.points ?: 0
        if (currentPoints < points) {
            return false // 积分不足
        }
        
        val updatedUser = user.copy(points = currentPoints - points)
        _currentUser.value = updatedUser
        userSessionManager.saveUser(updatedUser)
        return true
    }
}
