package com.vlog.app.screens.users

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vlog.app.data.users.UserDataRepository
import com.vlog.app.data.users.UserRepository
import com.vlog.app.data.users.Users
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.isBlank

/**
 * 用户视图模型
 * 处理用户相关的业务逻辑，如登录、注册等
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userDataRepository: UserDataRepository
) : ViewModel() {

    // 登录状态
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    // 注册状态
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState

    // 用户名验证状态
    private val _usernameValidationState = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val usernameValidationState: StateFlow<ValidationState> = _usernameValidationState

    // 昵称验证状态
    private val _nicknameValidationState = MutableStateFlow<ValidationState>(ValidationState.Idle)
    val nicknameValidationState: StateFlow<ValidationState> = _nicknameValidationState

    // 当前用户
    val currentUser = userDataRepository.currentUser

    init {
        // 初始化时加载当前用户信息
        viewModelScope.launch {
            userDataRepository.currentUser.collectLatest { user ->
                // 可以在这里处理用户状态变化
            }
        }
    }

    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     */
    fun login(username: String, password: String) {
        // 输入验证
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("用户名和密码不能为空")
            return
        }

        _loginState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val response = userRepository.login(username, password)
                if (response.code == 200 && response.data != null) {
                    // 登录成功，保存用户信息
                    userDataRepository.updateCurrentUser(response.data)
                    _loginState.value = LoginState.Success(response.data)
                } else {
                    // 登录失败
                    _loginState.value = LoginState.Error(response.message ?: "登录失败")
                }
            } catch (e: Exception) {
                // 网络错误或其他异常
                _loginState.value = LoginState.Error("登录失败: ${e.message}")
            }
        }
    }

    /**
     * 注册
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称
     */
    fun register(username: String, password: String, nickname: String) {
        // 输入验证
        if (username.isBlank() || password.isBlank() || nickname.isBlank()) {
            _registerState.value = RegisterState.Error("所有字段都不能为空")
            return
        }

        _registerState.value = RegisterState.Loading
        viewModelScope.launch {
            try {
                val response = userRepository.register(username, password, nickname)
                Log.d("register", response.toString())
                if (response.code == 200 && response.data != null) {
                    // 注册成功，保存用户信息
                    userDataRepository.updateCurrentUser(response.data)
                    _registerState.value = RegisterState.Success(response.data)
                } else {
                    // 注册失败
                    _registerState.value = RegisterState.Error(response.message ?: "注册失败")
                }
            } catch (e: Exception) {
                // 网络错误或其他异常
                _registerState.value = RegisterState.Error("注册失败: ${e.message}")
            }
        }
    }

    /**
     * 检查用户名是否可用
     * @param username 用户名
     */
    fun checkUsername(username: String) {
        if (username.isBlank()) {
            _usernameValidationState.value = ValidationState.Error("用户名不能为空")
            return
        }

        _usernameValidationState.value = ValidationState.Loading
        viewModelScope.launch {
            try {
                val response = userRepository.checkUsername(username)
                Log.d("UserViewModel", "Username check response: code=${response.code}, data=${response.data}, message=${response.message}")
                // 处理可能的null值
                if (response.data != null) {
                    val isExist = response.data == true // API返回true表示用户名已存在
                    if (isExist) {
                        _usernameValidationState.value = ValidationState.Error("用户名已被使用")
                    } else {
                        _usernameValidationState.value = ValidationState.Valid
                    }
                } else {
                    // 如果data为null，我们假设用户名可用
                    _usernameValidationState.value = ValidationState.Valid
                }
            } catch (e: Exception) {
                _usernameValidationState.value = ValidationState.Error("验证失败: ${e.message}")
            }
        }
    }

    /**
     * 检查昵称是否可用
     * @param nickname 昵称
     */
    fun checkNickname(nickname: String) {
        if (nickname.isBlank()) {
            _nicknameValidationState.value = ValidationState.Error("昵称不能为空")
            return
        }

        _nicknameValidationState.value = ValidationState.Loading
        viewModelScope.launch {
            try {
                val response = userRepository.checkNickname(nickname)
                Log.d("UserViewModel", "Nickname check response: code=${response.code}, data=${response.data}, message=${response.message}")
                // 处理可能的null值
                if (response.data != null) {
                    val isExist = response.data == true // API返回true表示昵称已存在
                    if (isExist) {
                        _nicknameValidationState.value = ValidationState.Error("昵称已被使用")
                    } else {
                        _nicknameValidationState.value = ValidationState.Valid
                    }
                } else {
                    // 如果data为null，我们假设昵称可用
                    _nicknameValidationState.value = ValidationState.Valid
                }
            } catch (e: Exception) {
                _nicknameValidationState.value = ValidationState.Error("验证失败: ${e.message}")
            }
        }
    }

    /**
     * 获取用户信息
     */
    fun getUserInfo() {
        val name = userDataRepository.getUserName() ?: return
        val token = userDataRepository.getAccessToken() ?: return

        viewModelScope.launch {
            try {
                val response = userRepository.getUserInfo(name, token)
                if (response.code == 200 && response.data != null) {
                    // 更新用户信息
                    userDataRepository.updateCurrentUser(response.data)
                }
            } catch (e: Exception) {
                // 处理错误，但不影响当前用户状态
            }
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        userDataRepository.updateCurrentUser(null)
        _loginState.value = LoginState.Idle
        _registerState.value = RegisterState.Idle
    }

    /**
     * 重置登录状态
     */
    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    /**
     * 重置注册状态
     */
    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }

    /**
     * 重置用户名验证状态
     */
    fun resetUsernameValidationState() {
        _usernameValidationState.value = ValidationState.Idle
    }

    /**
     * 重置昵称验证状态
     */
    fun resetNicknameValidationState() {
        _nicknameValidationState.value = ValidationState.Idle
    }

    /**
     * 检查是否已登录
     * @return 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return userDataRepository.isLoggedIn()
    }

    /**
     * 获取当前用户
     * @return 当前用户，如果未登录则返回null
     */
    fun getCurrentUser(): Users? {
        return userDataRepository.getCurrentUser()
    }

    /**
     * 扣除积分
     * @param points 要扣除的积分数量
     * @return 是否扣除成功
     */
    fun deductPoints(points: Int): Boolean {
        return userDataRepository.deductPoints(points)
    }
}

/**
 * 登录状态
 */
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: Users) : LoginState()
    data class Error(val message: String) : LoginState()
}

/**
 * 注册状态
 */
sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val user: Users) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

/**
 * 验证状态
 */
sealed class ValidationState {
    object Idle : ValidationState()
    object Loading : ValidationState()
    object Valid : ValidationState()
    data class Error(val message: String) : ValidationState()
}
