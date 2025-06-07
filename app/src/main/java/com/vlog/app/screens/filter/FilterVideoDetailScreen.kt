package com.vlog.app.screens.filter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.vlog.app.R
import com.vlog.app.data.videos.PlayList
import com.vlog.app.data.videos.Videos
import com.vlog.app.screens.components.GatherAndPlayerDialog
import com.vlog.app.screens.favorites.FavoriteViewModel
import com.vlog.app.screens.videos.VideoInfoSection


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterVideoDetailScreen(
    videoId: String,
    navController: NavController,
    viewModel: FilterVideoDetailViewModel = hiltViewModel(),
    playerViewModel: VideoPlayerViewModel = hiltViewModel(),
    favoriteViewModel: FavoriteViewModel = hiltViewModel()
) {

    val playerUiState by playerViewModel.uiState.collectAsState()
    val playlistState by playerViewModel.playlistState.collectAsState()

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 显示服务商和播放列表整合对话框
    if (uiState.showGatherAndPlayerDialog) {
        GatherAndPlayerDialog(
            gathers = uiState.gathers,
            players = uiState.players,
            selectedGatherId = uiState.selectedGatherId,
            selectedPlayerUrl = uiState.selectedPlayerUrl,
            isLoadingPlayers = uiState.isLoadingPlayers,
            onGatherSelected = { gatherId ->
                viewModel.loadPlayers(gatherId)
            },
            onPlayerSelected = { playerUrl, playerTitle ->
                viewModel.selectPlayerUrl(playerUrl, playerTitle)
            },
            onDismiss = { viewModel.hideGatherAndPlayerDialog() }
        )
    }

    val videoDetail = uiState.videoDetail

    // 初始化播放列表
    LaunchedEffect(videoDetail) {
        videoDetail?.let { detail ->
            if (!detail.gatherList.isNullOrEmpty()) {
                playerViewModel.initializePlaylist(detail, detail.gatherList!!)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (uiState.isLoading) {
            // 加载状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            videoDetail?.let { detail ->
                if (playerUiState.isFullscreen) {

                    val activity = context as? Activity
                    // 全屏模式：只显示播放器
                    VideoPlayerView(
                        playUrl = playlistState.currentPlayList.getOrNull(playlistState.currentPlayIndex)?.playUrl,
                        isFullscreen = playerUiState.isFullscreen,
                        onFullscreenToggle = {
                            playerViewModel.toggleFullscreen()
                        },
                        onPrevious = { playerViewModel.playPrevious() },
                        onNext = { playerViewModel.playNext() },
                        hasPrevious = playerViewModel.hasPrevious(),
                        hasNext = playerViewModel.hasNext(),
                        currentGatherTitle = playlistState.gatherList.getOrNull(playlistState.currentGatherIndex)?.gatherTitle,
                        currentPlayTitle = playlistState.currentPlayList.getOrNull(playlistState.currentPlayIndex)?.title,
                        onOrientationToggle = {
                            activity?.let {
                                it.requestedOrientation = if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            }
                            playerViewModel.toggleFullscreen()
                            playerViewModel.toggleOrientationFullscreen()
                        },
                        modifier = Modifier.fillMaxSize(),
                        isOrientationFullscreen = playerUiState.isOrientationFullscreen
                    )
                } else {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = uiState.videoDetail?.title ?: stringResource(R.string.video_detail)) },
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
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                        ) {
                            // 正常模式：显示所有内容
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                val activity = context as? Activity
                                // 播放器区域
                                VideoPlayerView(
                                    playUrl = playlistState.currentPlayList.getOrNull(playlistState.currentPlayIndex)?.playUrl,
                                    isFullscreen = playerUiState.isFullscreen,
                                    onFullscreenToggle = {
                                        playerViewModel.toggleFullscreen()
                                    },
                                    onPrevious = { playerViewModel.playPrevious() },
                                    onNext = { playerViewModel.playNext() },
                                    hasPrevious = playerViewModel.hasPrevious(),
                                    hasNext = playerViewModel.hasNext(),
                                    currentGatherTitle = playlistState.gatherList.getOrNull(playlistState.currentGatherIndex)?.gatherTitle,
                                    currentPlayTitle = playlistState.currentPlayList.getOrNull(playlistState.currentPlayIndex)?.title,
                                    onOrientationToggle = {
                                        activity?.let {
                                            it.requestedOrientation = if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } else {
                                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            }
                                        }
                                        playerViewModel.toggleFullscreen()
                                        playerViewModel.toggleOrientationFullscreen()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    isOrientationFullscreen = playerUiState.isOrientationFullscreen
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 视频标题
                                Text(
                                    text = detail.title ?: "未知标题",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 视频信息
                                VideoInfoSection(
                                    detail = detail,
                                    favoriteViewModel = favoriteViewModel
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 当前播放列表
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
                        }
                    }
                }
            }
        }
    }




//    if (isFullScreen) {
//        // 全屏模式下只显示播放器
//        VideoPlayer(
//            exoPlayer = exoPlayer,
//            isFullScreen = isFullScreen,
//            onFullScreenChange = { newValue -> isFullScreen = newValue }
//        )
//    } else {
//        Scaffold(
//            topBar = {
//                TopAppBar(
//                    title = { Text(text = uiState.videoDetail?.title ?: stringResource(R.string.video_detail)) },
//                    navigationIcon = {
//                        IconButton(onClick = { navController.popBackStack() }) {
//                            Icon(Icons.Filled.ArrowBackIosNew, contentDescription = stringResource(R.string.back))
//                        }
//                    },
//                    colors = TopAppBarDefaults.topAppBarColors(
//                        containerColor = MaterialTheme.colorScheme.primaryContainer,
//                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
//                    )
//                )
//            }
//        ) { paddingValues ->
//            Box(modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//            ) {
//                when {
//                    uiState.isLoading -> {
//                        LoadingView(modifier = Modifier.padding(paddingValues))
//                    }
//                    uiState.error != null -> {
//                        ErrorView(
//                            message = uiState.error ?: stringResource(R.string.loading_error),
//                            onRetry = { viewModel.loadVideoDetail() },
//                            modifier = Modifier.padding(paddingValues)
//                        )
//                    }
//                    uiState.videoDetail != null -> {
//
//
//
//                        // 主内容
//                        Column(
//                            modifier = Modifier.fillMaxSize()
//                        ) {
//                            Box(
//                                modifier = Modifier.fillMaxWidth()
//                            ) {
//                                key(currentPlayerUrl) {
//                                    // 视频播放器
//                                    VideoPlayer(
//                                        exoPlayer = exoPlayer,
//                                        isFullScreen = isFullScreen,
//                                        onFullScreenChange = { newValue -> isFullScreen = newValue }
//                                    )
//                                }
//
//
//                            }
//
//
//                            // 播放控制区域
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(horizontal = 16.dp, vertical = 8.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                // 左侧：详情/评论切换
//                                TabRow(
//                                    selectedTabIndex = uiState.selectedTab,
//                                    modifier = Modifier.weight(1f)
//                                ) {
//                                    Tab(
//                                        selected = uiState.selectedTab == 0,
//                                        onClick = { viewModel.selectTab(0) },
//                                        text = { Text("详情") }
//                                    )
//                                    Tab(
//                                        selected = uiState.selectedTab == 1,
//                                        onClick = { viewModel.selectTab(1) },
//                                        text = { Text("评论 (${uiState.comments.size})") }
//                                    )
//                                }
//
//                                Spacer(modifier = Modifier.width(8.dp))
//
//                                // 线路和选集整合按钮
//                                Button(
//                                    onClick = { viewModel.showGatherAndPlayerDialog() },
//                                    modifier = Modifier.wrapContentWidth()
//                                ) {
//                                    Icon(
//                                        imageVector = Icons.AutoMirrored.Filled.List,
//                                        contentDescription = null,
//                                        modifier = Modifier.size(16.dp)
//                                    )
//                                    Spacer(modifier = Modifier.width(4.dp))
//                                    Text("选集/线路")
//                                }
//                            }
//
//                            // 内容区域：详情或评论
//                            when (uiState.selectedTab) {
//                                0 -> {
//                                    // 详情
//                                    VideoDetailContent(videoDetail = videoDetail!!)
//
//                                    // 当前服务商的播放列表
//                                    if (uiState.players.isNotEmpty()) {
//                                        CurrentPlaylist(
//                                            players = uiState.players,
//                                            selectedPlayerUrl = uiState.selectedPlayerUrl,
//                                            gatherName = uiState.gathers.find { it.gatherId == uiState.selectedGatherId }?.gatherTitle,
//                                            onPlayerSelected = { playerUrl, playerTitle ->
//                                                viewModel.selectPlayerUrl(playerUrl, playerTitle)
//                                            }
//                                        )
//                                    }
//                                }
//                                1 -> {
//                                    // 评论
//                                    Box(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .height(400.dp) // 固定高度防止无限约束
//                                    ) {
//                                        CommentSection(
//                                            comments = uiState.comments,
//                                            isLoading = uiState.isLoadingComments,
//                                            onPostComment = { content ->
//                                                viewModel.postComment(content)
//                                            }
//                                        )
//                                    }
//                                }
//                            }
//
//                            // 推荐视频
//                            RecommendedVideos(
//                                videos = uiState.recommendedVideos,
//                                isLoading = uiState.isLoadingRecommendations,
//                                onVideoClick = { videoId ->
//                                    navController.navigate("video/$videoId")
//                                }
//                            )
//
//                            // 底部间距
//                            Spacer(modifier = Modifier.height(16.dp))
//                        }
//                    }
//                }
//            }
//        }
//
//
//    }


}


/**
 * 视频详情内容
 */
@Composable
fun VideoDetailContent(
    videoDetail: Videos
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = videoDetail.title?:"",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

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

/**
 * 当前播放列表
 */
@Composable
fun CurrentPlaylist(
    players: List<PlayList>,
    selectedPlayerUrl: String?,
    gatherName: String?,
    onPlayerSelected: (String, String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "剧集列表" + (gatherName?.let { " - $it" } ?: ""),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 播放列表横向滚动
        // 使用 rememberLazyListState 来控制滚动位置
        val listState = rememberLazyListState()

        // 自动滚动到选中的剧集
        LaunchedEffect(selectedPlayerUrl) {
            selectedPlayerUrl?.let { url ->
                val selectedIndex = players.indexOfFirst { it.playUrl == url }
                if (selectedIndex >= 0) {
                    listState.animateScrollToItem(selectedIndex)
                }
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(players) { play ->
                val isSelected = play.playUrl == selectedPlayerUrl

                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .wrapContentWidth()
                        .clickable { onPlayerSelected(play.playUrl.toString(), play.title) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = play.title?:"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetTextI18n")
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit
) {
    // 跟踪当前媒体项ID的状态
    var currentMediaId by remember { mutableStateOf(exoPlayer.currentMediaItem?.mediaId ?: "未知") }

    // 媒体项变化时的回调
    val onMediaItemChanged = { id: String ->
        currentMediaId = id
    }
    Box(modifier = Modifier
        .fillMaxWidth()
        .then(if (isFullScreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setFullscreenButtonClickListener { onFullScreenChange(!isFullScreen) }
                    setShowPreviousButton(true)
                    setShowNextButton(true)



                    // 添加监听器以便在Compose中更新标题
                    exoPlayer.addListener(object : Player.Listener {
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            // 当媒体项变化时通知Compose更新
                            onMediaItemChanged(mediaItem?.mediaId ?: "未知")
                        }
                    })

                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 添加全屏返回按钮
        if (isFullScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    //.background(Color.Black.copy(alpha = 0.5f)) // 半透明背景
                    .padding(4.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                IconButton(
                    onClick = { onFullScreenChange(false) }
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "退出全屏",
                        tint = Color.White
                    )
                }

                // 标题文本
                Text(
                    text = "当前播放: ${currentMediaId}",
                    color = Color.White,
                    modifier = Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
