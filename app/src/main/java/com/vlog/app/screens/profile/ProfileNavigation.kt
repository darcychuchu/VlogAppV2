package com.vlog.app.screens.profile

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vlog.app.navigation.NavigationRoutes.MainRoute
import com.vlog.app.screens.search.SearchScreen

/**
 * Profile模块路由
 */
sealed class ProfileScreenRoute(val route: String) {
    // 个人主页
    object Main : ProfileScreenRoute("profile_main")

    // 设置页面
    object Settings : ProfileScreenRoute("profile_settings")

    // 粉丝列表
    object Followers : ProfileScreenRoute("profile_followers")

    // 关注列表
    object Following : ProfileScreenRoute("profile_following")

    // 消息列表
    object Messages : ProfileScreenRoute("profile_messages")

    // 消息详情
    object MessageDetail : ProfileScreenRoute("profile_message_detail/{messageId}") {
        fun createRoute(messageId: String): String = "profile_message_detail/$messageId"
    }

    // 评论列表
    object Comments : ProfileScreenRoute("profile_comments")

    // 编辑个人资料
    object EditProfile : ProfileScreenRoute("profile_edit")


    object Search : ProfileScreenRoute("search?query={query}") {
        fun createRoute(query: String = ""): String {
            return if (query.isNotBlank()) {
                "search?query=${java.net.URLEncoder.encode(query, "UTF-8")}"
            } else {
                "search"
            }
        }
    }
    object WatchHistory : ProfileScreenRoute("watch_history")
    object AppUpdate : ProfileScreenRoute("app_update")
}

/**
 * Profile导航宿主
 */
@Composable
fun ProfileNavHost(
    onNavigateToLogin: () -> Unit = {},
    onNavigateToVideoDetail: (videoId: String) -> Unit = {},
    onNavigateToStoryDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToFollowers: () -> Unit = {},
    onNavigateToFollowing: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    startDestination: String = ProfileScreenRoute.Main.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 个人主页
        composable(ProfileScreenRoute.Main.route) {
            ProfileUserScreen(
                navController = navController,
                onNavigateToLogin = onNavigateToLogin
            )
        }

        composable(ProfileScreenRoute.WatchHistory.route) {
            WatchHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onVideoClick = onNavigateToVideoDetail)
        }

        composable(
            route = "${ProfileScreenRoute.Search.route}?query={query}",
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
                onVideoClick = onNavigateToVideoDetail
            )
        }

        // 版本更新页面
        composable(ProfileScreenRoute.AppUpdate.route) {
            AppUpdateScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }



//        // 设置页面
//        composable(ProfileScreenRoute.Settings.route) {
//            ProfileSettingsScreen(
//                navController = navController, // Pass the NavController
//                onNavigateBack = {
//                    navController.popBackStack()
//                }
//            )
//        }

//        // 粉丝列表
//        composable(ProfileScreen.Followers.route) {
//            ProfileFollowersScreen(
//                onNavigateBack = {
//                    navController.popBackStack()
//                },
//                onNavigateToUserProfile = onNavigateToUserProfile
//            )
//        }

//        // 关注列表
//        composable(ProfileScreen.Following.route) {
//            ProfileFollowingScreen(
//                onNavigateBack = {
//                    navController.popBackStack()
//                },
//                onNavigateToUserProfile = onNavigateToUserProfile
//            )
//        }

//        // 消息列表
//        composable(ProfileScreen.Messages.route) {
//            ProfileMessagesScreen(
//                onNavigateBack = {
//                    navController.popBackStack()
//                },
//                onNavigateToMessageDetail = { messageId ->
//                    navController.navigate(ProfileScreen.MessageDetail.createRoute(messageId))
//                }
//            )
//        }

//        // 消息详情
//        composable(
//            route = ProfileScreen.MessageDetail.route,
//            arguments = listOf(
//                navArgument("messageId") { type = NavType.StringType }
//            )
//        ) { backStackEntry ->
//            val messageId = backStackEntry.arguments?.getString("messageId") ?: ""
//
//            ProfileMessageDetailScreen(
//                messageId = messageId,
//                onNavigateBack = {
//                    navController.popBackStack()
//                }
//            )
//        }

//        // 评论列表
//        composable(ProfileScreen.Comments.route) {
//            ProfileCommentsScreen(
//                onNavigateBack = {
//                    navController.popBackStack()
//                }
//            )
//        }

//        // 编辑个人资料
//        composable(ProfileScreenRoute.EditProfile.route) {
//            ProfileEditScreen(
//                onNavigateBack = {
//                    navController.popBackStack()
//                }
//            )
//        }



    }
}

/**
 * 将Profile导航添加到主导航图
 */
fun NavGraphBuilder.profileNavigation(
    onNavigateToLogin: () -> Unit,
    onNavigateToVideoDetail: (videoId: String) -> Unit,
    onNavigateToStoryDetail: (String, String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToFollowers: () -> Unit = {},
    onNavigateToFollowing: () -> Unit = {}
) {
    composable(MainRoute.Profile.route) {
        ProfileNavHost(
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToVideoDetail = onNavigateToVideoDetail,
            onNavigateToStoryDetail = onNavigateToStoryDetail,
            onNavigateToUserProfile = onNavigateToUserProfile,
            onNavigateToFollowers = onNavigateToFollowers,
            onNavigateToFollowing = onNavigateToFollowing
        )
    }
}
//
///**
// * 设置页面（占位）
// */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProfileSettingsScreen(
//    navController: NavController, // Added NavController parameter
//    onNavigateBack: () -> Unit
//) {
//    Scaffold(
//        topBar = {
//            ProfileTopBar(
//                title = "设置", // Settings
//                onNavigateBack = onNavigateBack
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(16.dp) // Add padding for items
//        ) {
//            // Watch History Item
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable { navController.navigate(ProfileScreen.WatchHistory.route) }
//                    .padding(vertical = 12.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    imageVector = Icons.Outlined.History,
//                    contentDescription = "观看历史",
//                    modifier = Modifier.size(24.dp)
//                )
//                Spacer(modifier = Modifier.width(16.dp))
//                Text("观看历史", style = MaterialTheme.typography.bodyLarge)
//            }
//
//            // Search History Item
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable { navController.navigate(ProfileScreen.Search.createRoute()) }
//                    .padding(vertical = 12.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    imageVector = Icons.Outlined.Search,
//                    contentDescription = "搜索历史",
//                    modifier = Modifier.size(24.dp)
//                )
//                Spacer(modifier = Modifier.width(16.dp))
//                Text("搜索历史", style = MaterialTheme.typography.bodyLarge)
//            }
//
//            // App Update Item
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .clickable { navController.navigate(ProfileScreen.AppUpdate.route) }
//                    .padding(vertical = 12.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    imageVector = Icons.Outlined.SystemUpdate,
//                    contentDescription = "版本更新",
//                    modifier = Modifier.size(24.dp)
//                )
//                Spacer(modifier = Modifier.width(16.dp))
//                Text("版本更新", style = MaterialTheme.typography.bodyLarge)
//            }
//        }
//    }
//}
//
///**
// * 粉丝列表页面
// */
//@Composable
//fun ProfileFollowersScreen(
//    onNavigateBack: () -> Unit,
//    onNavigateToUserProfile: (String) -> Unit,
//    userViewModel: UserViewModel = hiltViewModel()
//) {
//    // 设置页面的实现将在后续添加
//    Scaffold(
//        topBar = {
//            ProfileTopBar(
//                title = "粉丝",
//                onNavigateBack = onNavigateBack
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(modifier = Modifier.height(32.dp))
//            Text(
//                text = "暂无粉丝",
//                style = MaterialTheme.typography.titleLarge
//            )
//        }
//    }
//}
//
///**
// * 关注列表页面
// */
//@Composable
//fun ProfileFollowingScreen(
//    onNavigateBack: () -> Unit,
//    onNavigateToUserProfile: (String) -> Unit,
//    userViewModel: UserViewModel = hiltViewModel()
//) {
//    // 设置页面的实现将在后续添加
//    Scaffold(
//        topBar = {
//            ProfileTopBar(
//                title = "关注",
//                onNavigateBack = onNavigateBack
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(modifier = Modifier.height(32.dp))
//            Text(
//                text = "暂无关注",
//                style = MaterialTheme.typography.titleLarge
//            )
//        }
//    }
//}
//
///**
// * 消息列表页面（占位）
// */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProfileMessagesScreen(
//    onNavigateBack: () -> Unit,
//    onNavigateToMessageDetail: (String) -> Unit
//) {
//    // 消息列表页面的实现将在后续添加
//    Scaffold(
//        topBar = {
//            ProfileTopBar(
//                title = "消息",
//                onNavigateBack = onNavigateBack
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(modifier = Modifier.height(32.dp))
//            Text(
//                text = "暂无消息",
//                style = MaterialTheme.typography.titleLarge
//            )
//        }
//    }
//}
//
///**
// * 消息详情页面（占位）
// */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProfileMessageDetailScreen(
//    messageId: String,
//    onNavigateBack: () -> Unit
//) {
//    // 消息详情页面的实现将在后续添加
//    Scaffold(
//        topBar = {
//            ProfileTopBar(
//                title = "消息详情",
//                onNavigateBack = onNavigateBack
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(modifier = Modifier.height(32.dp))
//            Text(
//                text = "暂无消息详情",
//                style = MaterialTheme.typography.titleLarge
//            )
//        }
//    }
//}
//
///**
// * 评论列表页面（占位）
// */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProfileCommentsScreen(
//    onNavigateBack: () -> Unit
//) {
//    // 评论列表页面的实现将在后续添加
//    Scaffold(
//        topBar = {
//            ProfileTopBar(
//                title = "评论",
//                onNavigateBack = onNavigateBack
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(modifier = Modifier.height(32.dp))
//            Text(
//                text = "暂无评论",
//                style = MaterialTheme.typography.titleLarge
//            )
//        }
//    }
//}
//
///**
// * 编辑个人资料页面（占位）
// */
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProfileEditScreen(
//    onNavigateBack: () -> Unit
//) {
//    // 编辑个人资料页面的实现将在后续添加
//    Scaffold(
//        topBar = {
//            ProfileTopBar(
//                title = "编辑个人资料",
//                onNavigateBack = onNavigateBack
//            )
//        }
//    ) { paddingValues ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(modifier = Modifier.height(32.dp))
//            Text(
//                text = "暂无个人资料",
//                style = MaterialTheme.typography.titleLarge
//            )
//        }
//    }
//}
