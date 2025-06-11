package com.vlog.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.vlog.app.data.users.UserSessionManager
import com.vlog.app.screens.components.CommonBottomBar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ViewModel to hold UserSessionManager, making it easier to inject into Composable
@HiltViewModel
class NavigationLayoutViewModel @Inject constructor(
    val userSessionManager: UserSessionManager
) : ViewModel()

@Composable
fun NavigationLayout(
    viewModel: NavigationLayoutViewModel = hiltViewModel() // Inject ViewModel
) {

    // 创建导航状态
    val navigationState = rememberNavigationState()
    val navController = navigationState.navController

    // 获取当前导航类型
    val navigationType by NavigationManager.currentNavigationType.collectAsState()

    // 获取当前路由
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // 检查当前路由是否是全屏路由
    val isFullScreenRoute = NavigationRoutes.isFullScreenRoute(currentRoute)

    // 如果是全屏路由，设置导航类型为全屏
    if (isFullScreenRoute && navigationType != NavigationType.FULLSCREEN) {
        NavigationManager.setNavigationType(NavigationType.FULLSCREEN)
    }

    // 根据导航类型显示不同的界面
    when (navigationType) {
        NavigationType.MAIN -> {
            // 主导航界面，包含底部导航栏
            ScaffoldLayout(
                navController = navController,
                currentDestination = currentDestination,
                userSessionManager = viewModel.userSessionManager
            )
        }
        NavigationType.FULLSCREEN -> {
            // 全屏导航界面，不包含底部导航栏
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary)) {
                MainNavGraph(navController = navController)
            }
        }
        NavigationType.OVERLAY -> {
            // 覆盖导航界面，显示在当前页面上方
            Box(modifier = Modifier.fillMaxSize()) {
                MainNavGraph(navController = navController)
                // 视频播放器覆盖层已移除
            }
        }
    }

    // 如果当前路由不是全屏路由，但导航类型是FULLSCREEN，重置为主导航
    LaunchedEffect(currentRoute) {
        if (!isFullScreenRoute && navigationType == NavigationType.FULLSCREEN) {
            NavigationManager.resetToMainNavigation()
        }
    }
}

@Composable
fun ScaffoldLayout(
    navController: NavHostController,
    currentDestination: NavDestination?,
    userSessionManager: UserSessionManager // Pass UserSessionManager
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            CommonBottomBar(
                navController = navController,
                currentDestination = currentDestination,
                userSessionManager = userSessionManager // Pass to CommonBottomBar
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            MainNavGraph(navController = navController)
        }
    }
}