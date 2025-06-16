package com.vlog.app.screens.filter

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.vlog.app.R
import com.vlog.app.data.videos.Videos
import com.vlog.app.navigation.NavigationRoutes // Added for login navigation
import com.vlog.app.screens.components.CommentSection
import com.vlog.app.screens.components.RecommendedVideos
import com.vlog.app.screens.favorites.FavoriteViewModel
import com.vlog.app.screens.users.UserViewModel // Added UserViewModel import
import kotlinx.coroutines.flow.map // Added for map operator


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    videoId: String,
    navController: NavController,
    viewModel: VideoDetailViewModel = hiltViewModel(),
    playerViewModel: VideoPlayerViewModel = hiltViewModel(),
    favoriteViewModel: FavoriteViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel() // Instantiated UserViewModel
) {

    val playerUiState by playerViewModel.uiState.collectAsState()
    val playlistState by playerViewModel.playlistState.collectAsState()

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val loginRequiredForFavorite by favoriteViewModel.loginRequiredEvent.collectAsState() // Renamed for clarity
    val isLoggedIn by userViewModel.currentUser.map { it != null }.collectAsState(initial = userViewModel.isLoggedIn())

    // Handle login required event for favoriting from this screen
    LaunchedEffect(loginRequiredForFavorite) {
        if (loginRequiredForFavorite) {
            navController.navigate(NavigationRoutes.OtherRoute.Login.route)
            favoriteViewModel.consumeLoginRequiredEvent()
        }
    }

    // Effect to consume pending subscription after login
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val pendingVideoId = userViewModel.consumePendingSubscription()
            if (pendingVideoId != null) {
                favoriteViewModel.addToFavorites(pendingVideoId) { success, message ->
                    Log.d("VideoDetailScreen", "Processed pending subscription for $pendingVideoId: Success=$success, Msg=$message")
                    // Optionally show a brief toast or snackbar here
                }
            }
        }
    }

    // 显示服务商和播放列表整合对话框
    if (uiState.showGatherAndPlayerDialog) {
        GatherListDialog(
            gatherList = uiState.gathers,
            currentGatherIndex = playlistState.currentGatherIndex,
            currentPlayIndex = playlistState.currentPlayIndex,
            onGatherSelected = { gatherIndex, playIndex ->
                playerViewModel.selectGather(gatherIndex, playIndex)
                viewModel.hideGatherAndPlayerDialog()
            },
            onPlaySourceSelected = { playIndex ->
                playerViewModel.selectPlaySource(playIndex)
                viewModel.hideGatherAndPlayerDialog()
            },
            onDismiss = {viewModel.hideGatherAndPlayerDialog() }
        )
    }

    val videoDetail = uiState.videoDetail

    LaunchedEffect(videoDetail) {
        videoDetail?.let { detail ->
            if (detail.gatherList?.isNotEmpty() == true) {
                playerViewModel.initializePlaylist(detail, detail.gatherList!!)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (uiState.error != null) {
            Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error) // Replace "Error: " with stringResource
            Button(onClick = { viewModel.retryFetch() }) {
                Text("Retry") // Replace "Retry" with stringResource
            }
        } else {
            videoDetail?.let { detail ->
                if (playerUiState.isFullscreen) {
                    VideoPlayerView(
                        isFullscreen = playerUiState.isFullscreen,
                        onFullscreenToggle = {
                            playerViewModel.toggleFullscreen()
                            val activity = context as? Activity
                            activity?.let {
                                it.requestedOrientation = if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            }
                        },
                        onPrevious = { playerViewModel.playPrevious() },
                        onNext = { playerViewModel.playNext() },
                        hasPrevious = playerViewModel.hasPrevious(),
                        hasNext = playerViewModel.hasNext(),
                        currentGatherTitle = playlistState.gatherList.getOrNull(playlistState.currentGatherIndex)?.gatherTitle,
                        currentPlayTitle = playlistState.currentPlayList.getOrNull(playlistState.currentPlayIndex)?.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = videoDetail.title ?: stringResource(R.string.video_detail)) },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = stringResource(R.string.back))
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    ) { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            ) {
                                VideoPlayerView(
                                    isFullscreen = playerUiState.isFullscreen,
                                    onFullscreenToggle = {
                                        playerViewModel.toggleFullscreen()
                                        val activity = context as? Activity
                                        activity?.let {
                                            it.requestedOrientation = if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } else {
                                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            }
                                        }
                                    },
                                    onPrevious = { playerViewModel.playPrevious() },
                                    onNext = { playerViewModel.playNext() },
                                    hasPrevious = playerViewModel.hasPrevious(),
                                    hasNext = playerViewModel.hasNext(),
                                    currentGatherTitle = playlistState.gatherList.getOrNull(playlistState.currentGatherIndex)?.gatherTitle,
                                    currentPlayTitle = playlistState.currentPlayList.getOrNull(playlistState.currentPlayIndex)?.title,
                                    modifier = Modifier.fillMaxWidth()
                                )


                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp)
                                ) {

                                    // 播放控制区域
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 左侧：详情/评论切换
                                        TabRow(
                                            selectedTabIndex = uiState.selectedTab,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Tab(
                                                selected = uiState.selectedTab == 0,
                                                onClick = { viewModel.selectTab(0) },
                                                text = { Text("详情") }
                                            )
                                            Tab(
                                                selected = uiState.selectedTab == 1,
                                                onClick = { viewModel.selectTab(1) },
                                                text = { Text("评论 (${uiState.comments.size})") }
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // 线路和选集整合按钮
                                        Button(
                                            onClick = { viewModel.showGatherAndPlayerDialog() },
                                            modifier = Modifier.wrapContentWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.List,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("选集/线路")
                                        }
                                    }

                                    // 内容区域：详情或评论
                                    when (uiState.selectedTab) {
                                        0 -> {
                                            // 详情
                                            VideoDetailContent(videoDetail = videoDetail,favoriteViewModel)

                                            // 当前服务商的播放列表
                                            if (playlistState.currentPlayList.isNotEmpty()) {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(16.dp)
                                                    ) {
                                                        Text(
                                                            text = "选择集数 (${playlistState.currentPlayList.size} 集)",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(bottom = 8.dp)
                                                        )

                                                        LazyRow(
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            itemsIndexed(playlistState.currentPlayList) { index, playItem ->
                                                                Card(
                                                                    modifier = Modifier
                                                                        .clickable {
                                                                            playerViewModel.selectPlaySource(index)
                                                                        },
                                                                    colors = CardDefaults.cardColors(
                                                                        containerColor = if (index == playlistState.currentPlayIndex) {
                                                                            MaterialTheme.colorScheme.primary
                                                                        } else {
                                                                            MaterialTheme.colorScheme.surface
                                                                        }
                                                                    )
                                                                ) {
                                                                    Text(
                                                                        text = playItem.title ?: "第${index + 1}集",
                                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                                        color = if (index == playlistState.currentPlayIndex) {
                                                                            MaterialTheme.colorScheme.onPrimary
                                                                        } else {
                                                                            MaterialTheme.colorScheme.onSurface
                                                                        },
                                                                        style = MaterialTheme.typography.bodyMedium
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        1 -> {
                                            // 评论
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(400.dp) // 固定高度防止无限约束
                                            ) {
                                                CommentSection(
                                                    comments = uiState.comments,
                                                    isLoading = uiState.isLoadingComments,
                                                    onPostComment = { content ->
                                                        if (viewModel.isUserLoggedIn()) {
                                                            viewModel.postComment(content)
                                                        } else {
                                                            navController.navigate(NavigationRoutes.OtherRoute.Login.route)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // 推荐视频
                                    RecommendedVideos(
                                        videos = uiState.recommendedVideos,
                                        isLoading = uiState.isLoadingRecommendations,
                                        onVideoClick = { videoId ->
                                            navController.navigate("filter_detail/$videoId")
                                        }
                                    )

                                    // 底部间距
                                    Spacer(modifier = Modifier.height(16.dp))
                                }



                            }
                        }
                    }
                }
            }


        }
    }
}


/**
 * 视频详情内容
 */
@Composable
fun VideoDetailContent(
    videoDetail: Videos,
    favoriteViewModel: FavoriteViewModel
) {
    val favoriteUiState by favoriteViewModel.uiState.collectAsState()
    val isFavorite by favoriteViewModel.isVideoFavoriteFlow(videoDetail.id!!).collectAsState(initial = false)
    var showMessage by remember { mutableStateOf<String?>(null) }

    // 显示操作结果消息
    LaunchedEffect(favoriteUiState.error, favoriteUiState.lastUpdateMessage) {
        favoriteUiState.error?.let { error ->
            showMessage = error
            favoriteViewModel.clearError()
        }
        favoriteUiState.lastUpdateMessage?.let { message ->
            showMessage = message
            favoriteViewModel.clearUpdateMessage()
        }
    }

    showMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2000)
            showMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        // 订阅按钮
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = videoDetail.title?:"",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = {
                    if (isFavorite) {
                        favoriteViewModel.removeFromFavorites(videoDetail.id!!) { success, message ->
                            showMessage = message
                        }
                    } else {
                        favoriteViewModel.addToFavorites(videoDetail.id!!) { success, message ->
                            showMessage = message
                        }
                    }
                },
                enabled = !favoriteUiState.isLoading
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "取消订阅" else "订阅",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 显示操作消息
        showMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.contains("成功") || message.contains("已移除"))
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 基本信息
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "评分: ${videoDetail.score}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            videoDetail.region?.let {
                Text(
                    text = "地区: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            videoDetail.releasedAt?.let {
                Text(
                    text = "年份: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 详细描述
        Text(
            text = "简介",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = videoDetail.description ?: "",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

