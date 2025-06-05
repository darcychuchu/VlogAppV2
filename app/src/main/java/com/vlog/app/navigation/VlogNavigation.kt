package com.vlog.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vlog.app.screens.favorites.FavoritesScreen
import com.vlog.app.screens.home.HomeScreen
import com.vlog.app.screens.profile.AppUpdateScreen
import com.vlog.app.screens.profile.ProfileScreen
import com.vlog.app.screens.profile.WatchHistoryScreen
import com.vlog.app.screens.search.SearchScreen
import com.vlog.app.screens.users.LoginScreen
import com.vlog.app.screens.users.RegisterScreen
import com.vlog.app.screens.users.UserViewModel
import com.vlog.app.screens.videos.VideoDetailScreen
import com.vlog.app.screens.videos.VideosScreen
import androidx.core.net.toUri
import androidx.navigation.NavGraphBuilder
import com.vlog.app.screens.profile.ProfileNavHost
import com.vlog.app.screens.publish.PublishScreen
import com.vlog.app.screens.users.UserHomeScreen
import com.vlog.app.ui.screens.publish.PhotoPublishScreen

sealed class Screen(val route: String) {
    // 认证相关页面
    object Login : Screen("login")
    object Register : Screen("register")

    // 视频相关页面
    object VideoDetail : Screen("video_detail/{videoId}") {
        fun createRoute(
            videoId: String,
            gatherId: String? = null,
            playerUrl: String? = null,
            episodeTitle: String? = null,
            lastPlayedPosition: Long? = null,
            episodeIndex: Int? = null
        ): String {
            var route = "video_detail/$videoId"
            val params = mutableListOf<String>()
            
            gatherId?.let { params.add("gatherId=$it") }
            playerUrl?.let { params.add("playerUrl=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            episodeTitle?.let { params.add("episodeTitle=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            lastPlayedPosition?.let { params.add("lastPlayedPosition=$it") }
            episodeIndex?.let { params.add("episodeIndex=$it") }
            
            if (params.isNotEmpty()) {
                route += "?" + params.joinToString("&")
            }
            
            return route
        }
    }

    // 视频相关页面
    object WatchHistory : Screen("watch_history/{videoId}")
    
    object Search : Screen("search?query={query}") {
        fun createRoute(query: String = ""): String {
            return if (query.isNotBlank()) {
                "search?query=${java.net.URLEncoder.encode(query, "UTF-8")}"
            } else {
                "search"
            }
        }
    }
    
    object AppUpdate : Screen("app_update")
    // 发布相关页面
    object PhotoPublish : Screen("photo_publish")
    // 发布相关页面
    object UserHome : Screen("user_home/{username}") {
        fun createRoute(username: String) = "user_home/$username"
    }
}

@Composable
fun VlogNavigation() {
    val navController = rememberNavController()
    val userViewModel: UserViewModel = hiltViewModel()
    val isLoggedIn = userViewModel.isLoggedIn()

    // 确定起始目的地
    val startDestination = if (isLoggedIn) {
        BottomNavItem.Home.route
    } else {
        Screen.Login.route
    }

    Scaffold(
        bottomBar = {
            // 只在主要页面显示底部导航栏
            val currentRoute = currentRoute(navController)
            if (currentRoute in listOf(
                    BottomNavItem.Home.route,
                    BottomNavItem.Videos.route,
                    BottomNavItem.Publish.route,
                    BottomNavItem.Favorites.route,
                    BottomNavItem.Profile.route
                )
            ) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {

            // 认证相关页面
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onLoginSuccess = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onRegisterSuccess = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    navController = navController
                )
            }

            composable(BottomNavItem.Videos.route) {
                VideosScreen(
                    navController = navController
                )
            }

            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    navController = navController,
                    onNavigateToWatchHistory = {
                        navController.navigate(Screen.WatchHistory.route)
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search.route)
                    },
                    onNavigateToAppUpdate = {
                        navController.navigate(Screen.AppUpdate.route)
                    }
                )
            }

            composable(BottomNavItem.Favorites.route) {
                FavoritesScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    }
                )
            }


            // 视频详情页面
            composable(
            route = Screen.VideoDetail.route,
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType }
            )
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                
                // 从查询参数中获取可选参数
                val uri = (backStackEntry.destination.route ?: "").toUri()
                val gatherId = uri.getQueryParameter("gatherId")
                val playerUrl = uri.getQueryParameter("playerUrl")?.let { 
                    java.net.URLDecoder.decode(it, "UTF-8")
                }
                val episodeTitle = uri.getQueryParameter("episodeTitle")?.let { 
                    java.net.URLDecoder.decode(it, "UTF-8")
                }
                val lastPlayedPosition = uri.getQueryParameter("lastPlayedPosition")?.toLongOrNull()
                val episodeIndex = uri.getQueryParameter("episodeIndex")?.toIntOrNull()
                
                VideoDetailScreen(
                    videoId = videoId,
                    gatherId = gatherId,
                    playerUrl = playerUrl,
                    episodeTitle = episodeTitle,
                    lastPlayedPosition = lastPlayedPosition,
                    episodeIndex = episodeIndex,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // 观看历史
            composable(Screen.WatchHistory.route) {
                WatchHistoryScreen(navController = navController)
            }
            
            // 搜索页面
            composable(
                route = "search?query={query}",
                arguments = listOf(
                    navArgument("query") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query") ?: ""
                SearchScreen(
                    initialQuery = query,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onVideoClick = { videoId ->
                        navController.navigate(Screen.VideoDetail.createRoute(videoId))
                    }
                )
            }
            
            // 版本更新页面
            composable(Screen.AppUpdate.route) {
                AppUpdateScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }


            // 图文发布页面
            composable(BottomNavItem.Publish.route) {
                PublishScreen(
                    onNavigateToPhotoPublish = {
                        navController.navigate(Screen.PhotoPublish.route)
                    }
                )
            }
            composable(Screen.PhotoPublish.route) {
                PhotoPublishScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }



            // 用户主页
            composable(
                route = Screen.UserHome.route,
                arguments = listOf(
                    navArgument("username") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username") ?: ""
                UserHomeScreen(
                    userName = username,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    navController = navController
                )
            }



            // profileNavigation 自定义二级导航
            profileNavigation(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToStoryDetail = { userName, storyId -> },
                onNavigateToUserProfile = { username ->
                    navController.navigate(Screen.UserHome.createRoute(username))
                },
                onNavigateToFollowers = {  },
                onNavigateToFollowing = { }
            )

        }
    }
}


@Composable
private fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Videos,
        BottomNavItem.Publish,
        BottomNavItem.Favorites,
        BottomNavItem.Profile
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}



/**
 * 将Profile导航添加到主导航图
 */
fun NavGraphBuilder.profileNavigation(
    onNavigateToLogin: () -> Unit,
    onNavigateToStoryDetail: (String, String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToFollowers: () -> Unit = {},
    onNavigateToFollowing: () -> Unit = {}
) {
    composable(BottomNavItem.Profile.route) {
        ProfileNavHost(
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToStoryDetail = onNavigateToStoryDetail,
            onNavigateToUserProfile = onNavigateToUserProfile,
            onNavigateToFollowers = onNavigateToFollowers,
            onNavigateToFollowing = onNavigateToFollowing
        )
    }
}