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
        object Videos : MainRoute(route = "Videos", resourceId = R.string.videos, icon = Icons.Default.Star)
        object Publish : MainRoute(route = "publish", resourceId = R.string.publish, icon = Icons.Default.Add)
        object Favorites : MainRoute(route = "Favorites", resourceId = R.string.favorites, icon = Icons.Default.Favorite)
        object Profile : MainRoute(route = "profile", resourceId = R.string.profile, icon = Icons.Default.AccountCircle)
    }

    // 全屏导航路由
    sealed class FullScreenRoute(val route: String) {
        object VideoDetail : FullScreenRoute("video_detail/{videoId}") {
            fun createRoute(videoId: String) = "video_detail/$videoId"
        }
        object VideoPlayer : FullScreenRoute("video_player/{videoId}?url={url}&gatherId={gatherId}&episodeTitle={episodeTitle}&lastPlayedPosition={lastPlayedPosition}&episodeIndex={episodeIndex}") {
            fun createRoute(videoId: String, url: String, gatherId: String, episodeTitle: String, lastPlayedPosition: Long = 0, episodeIndex: Int = 0) =
                "videos/player/$videoId?url=${url.encodeUrl()}&gatherId=${gatherId.encodeUrl()}&episodeTitle=${episodeTitle.encodeUrl()}&lastPlayedPosition=$lastPlayedPosition&episodeIndex=$episodeIndex"
        }

        object FilterDetail : FullScreenRoute("filter_detail/{videoId}") {
            fun createRoute(videoId: String) = "filter_detail/$videoId"
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
        return route.startsWith("filter_detail/") || route.startsWith("video_player/") || route.startsWith("video_detail/")
    }
}
