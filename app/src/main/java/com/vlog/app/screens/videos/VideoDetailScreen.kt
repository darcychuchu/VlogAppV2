package com.vlog.app.screens.videos

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.vlog.app.data.videos.GatherList
import com.vlog.app.data.videos.VideoDetail
import com.vlog.app.screens.favorites.FavoriteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    videoId: String,
    gatherId: String? = null,
    playerUrl: String? = null,
    episodeTitle: String? = null,
    lastPlayedPosition: Long? = null,
    episodeIndex: Int? = null,
    onNavigateBack: () -> Unit,
    viewModel: VideoDetailViewModel = hiltViewModel(),
    playerViewModel: VideoPlayerViewModel = hiltViewModel(),
    favoriteViewModel: FavoriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val videoDetail by viewModel.videoDetail.collectAsState()
    
    val playerUiState by playerViewModel.uiState.collectAsState()
    val playlistState by playerViewModel.playlistState.collectAsState()
    
    var showPlaylistDialog by remember { mutableStateOf(false) }

    // 加载视频详情
    LaunchedEffect(videoId) {
        viewModel.loadVideoDetail(videoId)
    }
    
    // 处理观看历史参数
    LaunchedEffect(gatherId, playerUrl, episodeTitle, lastPlayedPosition, episodeIndex, videoDetail) {
        if (gatherId != null && playerUrl != null && episodeTitle != null && lastPlayedPosition != null && episodeIndex != null && videoDetail != null) {
            // 等待视频详情加载完成后再设置播放参数
            playerViewModel.setCurrentGather(gatherId)
            playerViewModel.setCurrentEpisode(episodeIndex)
            playerViewModel.setPlayPosition(lastPlayedPosition)
        }
    }
    
    // 初始化播放列表
    LaunchedEffect(videoDetail) {
        videoDetail?.let { detail ->
            if (!detail.gatherList.isNullOrEmpty()) {
                playerViewModel.initializePlaylist(detail, detail.gatherList!!)
            }
        }
    }

    // 错误提示
    uiState.error?.let { message ->
        LaunchedEffect(message) {
            // 可以在这里显示 SnackBar 或其他错误提示
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("视频详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 播放列表按钮
                    IconButton(
                        onClick = { showPlaylistDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "播放列表"
                        )
                    }
                    
                    // 刷新按钮
                    IconButton(
                        onClick = { viewModel.loadVideoDetail(videoId) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        // 全屏模式：只显示播放器
                        VideoPlayerView(
                            playUrl = playlistState.currentPlayList.getOrNull(playlistState.currentPlayIndex)?.playUrl,
                            isFullscreen = playerUiState.isFullscreen,
                            onFullscreenToggle = { playerViewModel.toggleFullscreen() },
                            onPrevious = { playerViewModel.playPrevious() },
                            onNext = { playerViewModel.playNext() },
                            hasPrevious = playerViewModel.hasPrevious(),
                            hasNext = playerViewModel.hasNext(),
                            currentGatherTitle = playlistState.gatherList.getOrNull(playlistState.currentGatherIndex)?.gatherTitle,
                            currentPlayTitle = playlistState.currentPlayList.getOrNull(playlistState.currentPlayIndex)?.title,
                            onOrientationToggle = {
                                val activity = context as? Activity
                                activity?.let {
                                    it.requestedOrientation = if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // 正常模式：显示所有内容
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // 播放器区域
                            VideoPlayerView(
                                playUrl = playlistState.currentPlayList.getOrNull(playlistState.currentPlayIndex)?.playUrl,
                                isFullscreen = playerUiState.isFullscreen,
                                onFullscreenToggle = { playerViewModel.toggleFullscreen() },
                                onPrevious = { playerViewModel.playPrevious() },
                                onNext = { playerViewModel.playNext() },
                                hasPrevious = playerViewModel.hasPrevious(),
                                hasNext = playerViewModel.hasNext(),
                                currentGatherTitle = playlistState.gatherList.getOrNull(playlistState.currentGatherIndex)?.gatherTitle,
                                currentPlayTitle = playlistState.currentPlayList.getOrNull(playlistState.currentPlayIndex)?.title,
                                onOrientationToggle = {
                                    val activity = context as? Activity
                                    activity?.let {
                                        it.requestedOrientation = if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
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


    
    // 播放列表对话框
    if (showPlaylistDialog) {
        PlaylistDialog(
            gatherList = playlistState.gatherList,
            currentGatherIndex = playlistState.currentGatherIndex,
            currentPlayIndex = playlistState.currentPlayIndex,
            onGatherSelected = { gatherIndex, playIndex ->
                playerViewModel.selectGather(gatherIndex, playIndex)
                showPlaylistDialog = false
            },
            onPlaySourceSelected = { playIndex ->
                playerViewModel.selectPlaySource(playIndex)
                showPlaylistDialog = false
            },
            onDismiss = { showPlaylistDialog = false }
        )
    }
}

@Composable
fun VideoInfoSection(
    detail: VideoDetail,
    favoriteViewModel: FavoriteViewModel
) {
    val favoriteUiState by favoriteViewModel.uiState.collectAsState()
    val isFavorited by favoriteViewModel.isVideoFavoriteFlow(detail.id!!).collectAsState(initial = false)
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
    
    // 显示消息的Snackbar效果
    showMessage?.let { message ->
        LaunchedEffect(message) {
            // 这里可以添加Snackbar显示逻辑
            kotlinx.coroutines.delay(2000)
            showMessage = null
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 订阅按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        if (isFavorited) {
                            favoriteViewModel.removeFromFavorites(detail.id!!) { success, message ->
                                showMessage = message
                            }
                        } else {
                            favoriteViewModel.addToFavorites(detail.id!!) { success, message ->
                                showMessage = message
                            }
                        }
                    },
                    enabled = !favoriteUiState.isLoading
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorited) "取消订阅" else "订阅",
                        tint = if (isFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
            // 评分
            detail.score?.let { score ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "评分：",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = score,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 备注
            detail.remarks?.let { remarks ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "状态：",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = remarks,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 标签
            if (!detail.tags.isNullOrEmpty()) {
                Text(
                    text = "标签：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    detail.tags!!.forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { tag },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 发布时间
            detail.publishedAt?.let { time ->
                Text(
                    text = "发布时间：$time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}