package com.vlog.app.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vlog.app.navigation.BottomNavItem
import com.vlog.app.screens.users.UserViewModel
import com.vlog.app.ui.screens.profile.ProfileTopBar

/**
 * Profile模块路由
 */
sealed class ProfileScreen(val route: String) {
    // 个人主页
    object Main : ProfileScreen("profile_main")

    // 设置页面
    object Settings : ProfileScreen("profile_settings")

    // 粉丝列表
    object Followers : ProfileScreen("profile_followers")

    // 关注列表
    object Following : ProfileScreen("profile_following")

    // 消息列表
    object Messages : ProfileScreen("profile_messages")

    // 消息详情
    object MessageDetail : ProfileScreen("profile_message_detail/{messageId}") {
        fun createRoute(messageId: String): String = "profile_message_detail/$messageId"
    }

    // 评论列表
    object Comments : ProfileScreen("profile_comments")

    // 编辑个人资料
    object EditProfile : ProfileScreen("profile_edit")
}

/**
 * Profile导航宿主
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileNavHost(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToStoryDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToFollowers: () -> Unit = {},
    onNavigateToFollowing: () -> Unit = {},
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ProfileScreen.Main.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 个人主页
        composable(ProfileScreen.Main.route) {
            ProfileUserScreen(
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToSettings = {
                    navController.navigate(ProfileScreen.Settings.route)
                },
                onNavigateToFollowers = {
                    navController.navigate(ProfileScreen.Followers.route)
                },
                onNavigateToFollowing = {
                    navController.navigate(ProfileScreen.Following.route)
                },
                onNavigateToMessages = {
                    navController.navigate(ProfileScreen.Messages.route)
                },
                onNavigateToComments = {
                    navController.navigate(ProfileScreen.Comments.route)
                },
                onNavigateToEditProfile = {
                    navController.navigate(ProfileScreen.EditProfile.route)
                },
                onNavigateToStoryDetail = onNavigateToStoryDetail,
                navController = navController
            )
        }

        // 设置页面
        composable(ProfileScreen.Settings.route) {
            ProfileSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 粉丝列表
        composable(ProfileScreen.Followers.route) {
            ProfileFollowersScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUserProfile = onNavigateToUserProfile
            )
        }

        // 关注列表
        composable(ProfileScreen.Following.route) {
            ProfileFollowingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUserProfile = onNavigateToUserProfile
            )
        }

        // 消息列表
        composable(ProfileScreen.Messages.route) {
            ProfileMessagesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMessageDetail = { messageId ->
                    navController.navigate(ProfileScreen.MessageDetail.createRoute(messageId))
                }
            )
        }

        // 消息详情
        composable(
            route = ProfileScreen.MessageDetail.route,
            arguments = listOf(
                navArgument("messageId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val messageId = backStackEntry.arguments?.getString("messageId") ?: ""

            ProfileMessageDetailScreen(
                messageId = messageId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 评论列表
        composable(ProfileScreen.Comments.route) {
            ProfileCommentsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 编辑个人资料
        composable(ProfileScreen.EditProfile.route) {
            ProfileEditScreen(
                onNavigateBack = {
                    navController.popBackStack()
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

/**
 * 设置页面（占位）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onNavigateBack: () -> Unit
) {
    // 设置页面的实现将在后续添加
    Scaffold(
        topBar = {
            ProfileTopBar(
                title = "设置",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "暂无设置",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

/**
 * 粉丝列表页面
 */
@Composable
fun ProfileFollowersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    userViewModel: UserViewModel = hiltViewModel()
) {
    // 设置页面的实现将在后续添加
    Scaffold(
        topBar = {
            ProfileTopBar(
                title = "粉丝",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "暂无粉丝",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

/**
 * 关注列表页面
 */
@Composable
fun ProfileFollowingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    userViewModel: UserViewModel = hiltViewModel()
) {
    // 设置页面的实现将在后续添加
    Scaffold(
        topBar = {
            ProfileTopBar(
                title = "关注",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "暂无关注",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

/**
 * 消息列表页面（占位）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMessagesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMessageDetail: (String) -> Unit
) {
    // 消息列表页面的实现将在后续添加
    Scaffold(
        topBar = {
            ProfileTopBar(
                title = "消息",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "暂无消息",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

/**
 * 消息详情页面（占位）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMessageDetailScreen(
    messageId: String,
    onNavigateBack: () -> Unit
) {
    // 消息详情页面的实现将在后续添加
    Scaffold(
        topBar = {
            ProfileTopBar(
                title = "消息详情",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "暂无消息详情",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

/**
 * 评论列表页面（占位）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCommentsScreen(
    onNavigateBack: () -> Unit
) {
    // 评论列表页面的实现将在后续添加
    Scaffold(
        topBar = {
            ProfileTopBar(
                title = "评论",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "暂无评论",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

/**
 * 编辑个人资料页面（占位）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onNavigateBack: () -> Unit
) {
    // 编辑个人资料页面的实现将在后续添加
    Scaffold(
        topBar = {
            ProfileTopBar(
                title = "编辑个人资料",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "暂无个人资料",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
