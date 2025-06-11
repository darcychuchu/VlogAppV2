package com.vlog.app.navigation

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vlog.app.screens.favorites.FavoritesScreen
import com.vlog.app.screens.filter.FilterScreen
import com.vlog.app.screens.filter.VideoDetailScreen
import com.vlog.app.screens.home.HomeScreen
import com.vlog.app.screens.profile.AppUpdateScreen
import com.vlog.app.screens.profile.WatchHistoryScreen
import com.vlog.app.screens.publish.PublishScreen
import com.vlog.app.screens.search.SearchScreen
import com.vlog.app.screens.users.LoginScreen
import com.vlog.app.screens.users.RegisterScreen
import com.vlog.app.screens.users.UserHomeScreen
import com.vlog.app.screens.users.UserStoriesDetailScreen // Added import for the new screen
// Import for Text if PlaceholderUserStoriesDetailScreen will use it
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

// PlaceholderUserStoriesDetailScreen is no longer needed, it can be removed.
// @Composable
// fun PlaceholderUserStoriesDetailScreen(navController: NavHostController, username: String?, storyId: String?) {
//    Text("User Story Detail for Username: $username, Story ID: $storyId")
// }

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
            navController = navController,
            onNavigateBack = {
                navController.popBackStack()
            },
            onVideoClick = { videoId ->
                navController.navigate(NavigationRoutes.FullScreenRoute.FilterDetail.createRoute(videoId))
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
        route = "${NavigationRoutes.FullScreenRoute.FilterDetail.route}?videoId={videoId}",
        arguments = listOf(
            navArgument("videoId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
        VideoDetailScreen(
            videoId = videoId,
            navController = navController
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
            navController = navController,
            initialQuery = query,
            onNavigateBack = {
                navController.popBackStack()
            },
            onVideoClick = { videoId ->
                navController.navigate(NavigationRoutes.FullScreenRoute.FilterDetail.createRoute(videoId))
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
                navController.popBackStack() // Changed as per subtask requirement
            }
        )
    }

    composable(NavigationRoutes.OtherRoute.Register.route) {
        RegisterScreen(
            onNavigateBack = { // This should ideally go back to Login screen or handle stack appropriately
                navController.popBackStack()
            },
            onRegisterSuccess = {
                // Navigate to Home and clear the auth stack (Login and Register)
                navController.navigate(NavigationRoutes.MainRoute.Home.route) {
                    popUpTo(NavigationRoutes.OtherRoute.Login.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }

    composable(
        route = NavigationRoutes.OtherRoute.UserStoryDetail.route,
        arguments = listOf(
            navArgument("username") { type = NavType.StringType; nullable = true }, // Made username nullable to handle potential nulls gracefully
            navArgument("storyId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val username = backStackEntry.arguments?.getString("username")
        val storyId = backStackEntry.arguments?.getString("storyId")
        // It's good practice to handle cases where arguments might be unexpectedly null,
        // though NavType.StringType without nullable = true should ensure storyId is present.
        // Username is explicitly nullable as per navArgument, UserStoriesDetailScreen takes nullable username.
        // StoryId should be non-null as it's a mandatory part of the path and NavType.StringType (not nullable).
        if (storyId != null) {
            UserStoriesDetailScreen(
                navController = navController,
                username = username,
                storyId = storyId
            )
        } else {
            // This case should ideally not be reached if storyId is a mandatory path parameter.
            // If it can be reached due to deep linking or other issues, provide fallback.
            Text("Error: Story ID is missing. Cannot display details.")
        }
    }
}
