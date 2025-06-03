package com.vlog.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home",
        title = "首页",
        icon = Icons.Default.Home
    )

    object Videos : BottomNavItem(
        route = "Videos",
        title = "影视",
        icon = Icons.Default.Star
    )

//    object Subscript : BottomNavItem(
//        route = "Subscript",
//        title = "小程序",
//        icon = Icons.Default.Star
//    )

    object Publish : BottomNavItem(
        route = "publish",
        title = "发布",
        icon = Icons.Default.Add
    )

    object Subscribe : BottomNavItem(
        route = "subscribe",
        title = "订阅",
        icon = Icons.Default.Favorite
    )

    object Profile : BottomNavItem(
        route = "profile",
        title = "我",
        icon = Icons.Default.AccountCircle
    )
}
