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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vlog.app.screens.home.HomeScreen
import com.vlog.app.screens.users.LoginScreen
import com.vlog.app.screens.users.RegisterScreen
import com.vlog.app.screens.users.UserViewModel

sealed class Screen(val route: String) {
    // 认证相关页面
    object Login : Screen("login")
    object Register : Screen("register")

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
                    BottomNavItem.Subscribe.route,
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
        BottomNavItem.Subscribe,
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
