package com.vlog.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 导航类型
 * MAIN - 主导航，包含底部导航栏
 * FULLSCREEN - 全屏导航，不包含底部导航栏
 * OVERLAY - 覆盖导航，显示在当前页面上方
 */
enum class NavigationType {
    MAIN,
    FULLSCREEN,
    OVERLAY
}

/**
 * 导航管理器
 * 用于管理应用的导航状态
 */
object NavigationManager {
    // 当前导航类型
    private val _currentNavigationType = MutableStateFlow(NavigationType.MAIN)
    val currentNavigationType: StateFlow<NavigationType> = _currentNavigationType.asStateFlow()

    // 设置导航类型
    fun setNavigationType(type: NavigationType) {
        _currentNavigationType.value = type
    }

    // 重置为主导航
    fun resetToMainNavigation() {
        _currentNavigationType.value = NavigationType.MAIN
    }
}

/**
 * 导航状态
 * 包含导航控制器和导航类型
 */
@Composable
fun rememberNavigationState(
    navController: NavHostController = rememberNavController(),
    navigationType: NavigationType = NavigationManager.currentNavigationType.value
): NavigationState {
    return remember(navController, navigationType) {
        NavigationState(navController, navigationType)
    }
}

/**
 * 导航状态类
 */
class NavigationState(
    val navController: NavHostController,
    val navigationType: NavigationType
)
