package com.vlog.app.navigation

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vlog.app.screens.favorites.FavoritesScreen
import com.vlog.app.screens.filter.FilterScreen
import com.vlog.app.screens.home.HomeScreen
import com.vlog.app.screens.profile.AppUpdateScreen
import com.vlog.app.screens.profile.WatchHistoryScreen
import com.vlog.app.screens.publish.PublishScreen
import com.vlog.app.screens.search.SearchScreen
import com.vlog.app.screens.users.LoginScreen
import com.vlog.app.screens.users.RegisterScreen
import com.vlog.app.screens.users.UserHomeScreen
import com.vlog.app.screens.videos.VideoDetailScreen
import com.vlog.app.screens.videos.VideosScreen
import com.vlog.app.ui.screens.publish.PhotoPublishScreen

/**
 * 主导航图
 * 包含应用的所有导航路由
 */
@Composable
fun MainNavGraph(
    navController: NavHostController,
    startDestination: String = NavigationRoutes.MainRoute.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        addMainRoutes(navController)

        addFullScreenRoutes(navController)

        addOtherRoutes(navController)
    }
}

/**
 * 添加主导航路由
 */
private fun NavGraphBuilder.addMainRoutes(navController: NavHostController) {
    // 首页
    composable(NavigationRoutes.MainRoute.Home.route) {
        HomeScreen(
            navController = navController
        )
    }

    composable(NavigationRoutes.MainRoute.Videos.route) {
        FilterScreen(
            navController = navController
        )
    }

    composable(NavigationRoutes.MainRoute.Publish.route) {
        PublishScreen(
            onNavigateToPhotoPublish = {
                navController.navigate(NavigationRoutes.OtherRoute.PhotoPublish.route)
            }
        )
    }

    composable(NavigationRoutes.MainRoute.Favorites.route) {
        FavoritesScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onVideoClick = { videoId ->
                navController.navigate(NavigationRoutes.FullScreenRoute.VideoDetail.createRoute(videoId))
            }
        )
    }

    composable(
        route = "${NavigationRoutes.MainRoute.Profile.route}?username={username}",
        arguments = listOf(
            navArgument("username") {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            }
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
}

/**
 * 添加全屏导航路由
 */
@OptIn(UnstableApi::class)
private fun NavGraphBuilder.addFullScreenRoutes(navController: NavHostController) {

    composable(
        route = "${NavigationRoutes.FullScreenRoute.VideoDetail.route}?videoId={videoId}",
        arguments = listOf(
            navArgument("videoId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
        VideoDetailScreen(
            videoId = videoId,
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    // 视频播放器页面
    composable(
        route = NavigationRoutes.FullScreenRoute.VideoPlayer.route,
        arguments = listOf(
            navArgument("videoId") {
                type = NavType.StringType
                nullable = false
            },
            navArgument("url") {
                type = NavType.StringType
                nullable = false
                defaultValue = ""
            },
            navArgument("gatherId") {
                type = NavType.StringType
                nullable = false
                defaultValue = ""
            },
            navArgument("episodeTitle") {
                type = NavType.StringType
                nullable = true
                defaultValue = ""
            },
            navArgument("lastPlayedPosition") {
                type = NavType.LongType
                defaultValue = 0L
            },
            navArgument("episodeIndex") {
                type = NavType.IntType
                defaultValue = 0
            }


        )
    ) { backStackEntry ->
        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
        val url = backStackEntry.arguments?.getString("url")
        val gatherId = backStackEntry.arguments?.getString("gatherId")
        val episodeTitle = backStackEntry.arguments?.getString("episodeTitle")
        val lastPlayedPosition = backStackEntry.arguments?.getLong("lastPlayedPosition") ?: 0L
        val episodeIndex = backStackEntry.arguments?.getInt("episodeIndex") ?: 0

        // 设置为全屏导航
        NavigationManager.setNavigationType(NavigationType.FULLSCREEN)

        // 解码URL
        val decodedUrl = java.net.URLDecoder.decode(url, "UTF-8")
        val decodedTitle = java.net.URLDecoder.decode(episodeTitle, "UTF-8")

        VideoDetailScreen(
            videoId = videoId,
            gatherId = gatherId,
            playerUrl = decodedUrl,
            episodeTitle = decodedTitle,
            lastPlayedPosition = lastPlayedPosition,
            episodeIndex = episodeIndex,
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }




}

/**
 * 添加其他导航路由
 */
private fun NavGraphBuilder.addOtherRoutes(navController: NavHostController) {
    // 观看历史
    composable(NavigationRoutes.OtherRoute.WatchHistory.route) {
        WatchHistoryScreen(navController = navController)
    }

    composable(
        route = "${NavigationRoutes.OtherRoute.Search.route}?query={query}",
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
                navController.navigate(NavigationRoutes.FullScreenRoute.VideoDetail.createRoute(videoId))
            }
        )
    }

    // 版本更新页面
    composable(NavigationRoutes.OtherRoute.AppUpdate.route) {
        AppUpdateScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    composable(NavigationRoutes.OtherRoute.PhotoPublish.route) {
        PhotoPublishScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }


    // 认证相关页面
    composable(NavigationRoutes.OtherRoute.Login.route) {
        LoginScreen(
            onNavigateToRegister = {
                navController.navigate(NavigationRoutes.OtherRoute.Register.route)
            },
            onLoginSuccess = {
                navController.navigate(NavigationRoutes.MainRoute.Home.route) {
                    popUpTo(NavigationRoutes.OtherRoute.Login.route) { inclusive = true }
                }
            }
        )
    }

    composable(NavigationRoutes.OtherRoute.Register.route) {
        RegisterScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onRegisterSuccess = {
                navController.navigate(NavigationRoutes.MainRoute.Home.route) {
                    popUpTo(NavigationRoutes.OtherRoute.Register.route) { inclusive = true }
                }
            }
        )
    }
}
