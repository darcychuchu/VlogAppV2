package com.vlog.app.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vlog.app.data.stories.Stories
import com.vlog.app.navigation.NavigationRoutes
import com.vlog.app.screens.components.StoryItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSubScripts: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    navController: NavController, // Changed to non-null as it's essential for navigation
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("发现", "关注")
    val snackBarHostState = remember { SnackbarHostState() }

    // 获取全局动态和作品列表
    val storiesList by homeViewModel.storiesList.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()
    val error by homeViewModel.error.collectAsState()
    val hasMoreData by homeViewModel.hasMoreData.collectAsState()

    // 处理错误信息
    error?.let { errorMsg ->
        androidx.compose.runtime.LaunchedEffect(errorMsg) {
            scope.launch {
                snackBarHostState.showSnackbar(errorMsg)
                homeViewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧汉堡菜单
                        IconButton(
                            onClick = {  }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "菜单"
                            )
                        }

                        // 中间的标签页
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Row {
                                tabs.forEachIndexed { index, title ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .clickable { selectedTabIndex = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .let {
                                                    if (selectedTabIndex == index) {
                                                        it
                                                    } else {
                                                        it
                                                    }
                                                },
                                            textAlign = TextAlign.Center,
                                            color = if (selectedTabIndex == index) {
                                                androidx.compose.ui.graphics.Color.Black
                                            } else {
                                                androidx.compose.ui.graphics.Color.Gray
                                            }
                                        )
                                    }

                                    if (index < tabs.size - 1) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                }
                            }
                        }

                        // 右侧占位，保持对称
                        IconButton(
                            onClick = {  }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 内容区域
            when (selectedTabIndex) {
                0 -> DiscoverContent(
                    storiesList = storiesList,
                    isLoading = isLoading,
                    isRefreshing = isRefreshing,
                    hasMoreData = hasMoreData,
                    onRefresh = { homeViewModel.refresh() },
                    onLoadMore = { homeViewModel.loadMore() },
                    onNavigateToUserProfile = { username ->
                        // Ensure username is not empty, though createRoute should handle it.
                        // Using a fallback if necessary, though the calling site (StoryItem) will provide it.
                        navController.navigate(NavigationRoutes.OtherRoute.UserHome.createRoute(username = username))
                    },
                    onNavigateToStoryDetail = { username, storyId ->
                        navController.navigate(NavigationRoutes.OtherRoute.UserStoryDetail.createRoute(username = username, storyId = storyId))
                    },
                    onNavigateToArtworkDetail = { userName, artworkId, isPlayerMode -> }
                )
                1 -> FollowContent(
                    storiesList = homeViewModel.followingStoriesList.collectAsState().value,
                    isLoading = homeViewModel.isFollowingLoading.collectAsState().value,
                    isRefreshing = homeViewModel.isFollowingRefreshing.collectAsState().value,
                    hasMoreData = homeViewModel.followingHasMoreData.collectAsState().value,
                    onRefresh = {  },
                    onLoadMore = {  },
                    onNavigateToUserProfile = { username ->
                        navController.navigate(NavigationRoutes.OtherRoute.UserHome.createRoute(username = username))
                    },
                    onNavigateToStoryDetail = { username, storyId ->
                        navController.navigate(NavigationRoutes.OtherRoute.UserStoryDetail.createRoute(username = username, storyId = storyId))
                    },
                    onNavigateToArtworkDetail = { userName, artworkId, isPlayerMode -> },
                    isLoggedIn = true
                )
            }
        }
    }
}

@Composable
fun DiscoverContent(
    storiesList: List<Stories>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    hasMoreData: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToStoryDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToArtworkDetail: (String, String, Boolean) -> Unit = { _, _, _ -> }
) {
    val listState = rememberLazyListState()



    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 列表内容
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            items(storiesList) { story ->
                StoryItem(
                    story = story,
                    onClick = {


                        // 根据isTyped值决定导航到哪个页面
                        // Null checks for id are important.
                        // Using nickName as primary for username, fallback to createdBy, then "unknown".
                        val storyId = story.id
                        val username = story.nickName ?: story.createdBy ?: "unknown"
                        if (storyId != null) {
                            onNavigateToStoryDetail(username, storyId)
                        }
                        // If storyId is null, onClick does nothing, which is reasonable.
                    },
                    onUserClick = {
                        // Using nickName as primary for username, fallback to createdBy, then "unknown".
                        val username = story.nickName ?: story.createdBy ?: "unknown"
                        onNavigateToUserProfile(username)
                    }
                )
            }
        }

    }
}

