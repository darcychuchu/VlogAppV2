package com.vlog.app.screens.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.vlog.app.data.users.UserSessionManager
import com.vlog.app.navigation.NavigationRoutes

/**
 * 通用底部导航栏
 *
 * @param navController 导航控制器
 * @param currentDestination 当前目的地
 * @param userSessionManager 用户会话管理器
 */
@Composable
fun CommonBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    userSessionManager: UserSessionManager // Added UserSessionManager
) {
    NavigationBar {
        NavigationRoutes.bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(stringResource(screen.resourceId)) },
                selected = currentDestination?.hierarchy?.any {
                    it.route?.startsWith(screen.route) == true || it.route == screen.route
                } == true,
                onClick = {
                    if (screen.route == NavigationRoutes.MainRoute.Publish.route) {
                        if (userSessionManager.isLoggedIn()) {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            navController.navigate(NavigationRoutes.OtherRoute.Login.route) {
                                // Optional: popUpTo and launchSingleTop for Login route if needed
                                // popUpTo(navController.graph.findStartDestination().id)
                                // launchSingleTop = true
                            }
                        }
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
