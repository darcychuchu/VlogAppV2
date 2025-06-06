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
import com.vlog.app.navigation.NavigationRoutes

/**
 * 通用底部导航栏
 * 
 * @param navController 导航控制器
 * @param currentDestination 当前目的地
 */
@Composable
fun CommonBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?
) {
    NavigationBar {
        NavigationRoutes.bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(stringResource(screen.resourceId)) },
                selected = currentDestination?.hierarchy?.any {
                    // 判断当前页面是否是底部导航栏项
                    it.route?.startsWith(screen.route) == true || it.route == screen.route
                } == true,
                onClick = {
                    // 当点击底部导航栏时，完全清除导航堆栈，确保返回到正确的页面
                    navController.navigate(screen.route) {
                        // 清除所有导航堆栈，只保留起始页面
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true // 保存状态
                        }
                        launchSingleTop = true // 确保不创建多个实例
                        restoreState = true // 恢复状态
                    }
                }
            )
        }
    }
}
