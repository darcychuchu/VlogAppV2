package com.vlog.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import com.vlog.app.R

/**
 * 导航路由
 * 定义应用中的所有导航路由
 */
object NavigationRoutes {
    // 主导航路由
    sealed class MainRoute(val route: String, val resourceId: Int, val icon: ImageVector) {
        object Home : MainRoute(route = "home",resourceId = R.string.home,icon = Icons.Default.Home)
        object Videos : MainRoute(route = "videos", resourceId = R.string.videos, icon = Icons.Default.Star)
        object Publish : MainRoute(route = "publish", resourceId = R.string.publish, icon = Icons.Default.Add)
        object Favorites : MainRoute(route = "favorites", resourceId = R.string.favorites, icon = Icons.Default.Favorite)
        object Profile : MainRoute(route = "profile", resourceId = R.string.profile, icon = Icons.Default.AccountCircle)
    }

    // 全屏导航路由
    sealed class FullScreenRoute(val route: String) {

        object VideoDetail : FullScreenRoute("video_detail/{videoId}") {
            fun createRoute(videoId: String) = "video_detail/$videoId"
        }

    }

    // URL编码
    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
    }

    // 其他导航路由
    sealed class OtherRoute(val route: String) {
        object Search : OtherRoute("search?query={query}") {
            fun createRoute(query: String = ""): String {
                return if (query.isNotBlank()) {
                    "search?query=${query.encodeUrl()}"
                } else {
                    "search"
                }
            }
        }
        object WatchHistory : OtherRoute("watch_history")
        object AppUpdate : OtherRoute("app_update")
        object PhotoPublish : OtherRoute("photo_publish")

        // 发布相关页面
        object UserHome : OtherRoute("user_home/{username}") {
            fun createRoute(username: String) = "user_home/$username"
        }

        object UserStoryDetail : OtherRoute("user_story_detail/{username}/{storyId}") {
            fun createRoute(username: String, storyId: String) = "user_story_detail/$username/$storyId"
        }

        // 认证相关页面
        object Login : OtherRoute("login")
        object Register : OtherRoute("register")
    }

    // 底部导航项
    val bottomNavItems = listOf(
        MainRoute.Home,
        MainRoute.Videos,
        MainRoute.Publish,
        MainRoute.Favorites,
        MainRoute.Profile
    )

    // 判断路由是否为全屏路由
    fun isFullScreenRoute(route: String?): Boolean {
        if (route == null) return false
        return route.startsWith("video_detail/")
    }
}
