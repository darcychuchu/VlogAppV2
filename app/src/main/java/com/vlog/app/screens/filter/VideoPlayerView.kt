package com.vlog.app.screens.filter

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    playUrl: String?,
    isFullscreen: Boolean,
    isOrientationFullscreen: Boolean,
    onFullscreenToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
    currentGatherTitle: String?,
    currentPlayTitle: String?,
    onOrientationToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var bufferedPosition by remember { mutableStateOf(0L) }
    
    // 创建ExoPlayer实例
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                    
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        // 处理播放状态变化
                    }
                })
            }
    }
    
    // 更新播放URL
    LaunchedEffect(playUrl) {
        playUrl?.let { url ->
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }
    
    // 定期更新播放进度
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
            bufferedPosition = exoPlayer.bufferedPosition
            delay(100)
        }
    }
    
    // 控制栏自动隐藏
    LaunchedEffect(showControls) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }
    
    // 注意：移除了Activity方向设置，避免整个页面横屏
    // 全屏模式现在只影响播放器组件本身的布局
    
    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isFullscreen) {
                    Modifier.fillMaxHeight()
                } else {
                    Modifier.aspectRatio(16f / 9f)
                }
            )
            .background(Color.Black)
            .clickable {
                showControls = !showControls
            }
    ) {
        // ExoPlayer视图
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false // 使用自定义控制器
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 自定义控制栏
        if (showControls) {
            VideoPlayerControls(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                bufferedPosition = bufferedPosition,
                isFullscreen = isFullscreen,
                onPlayPause = {
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                onSeekTo = { position ->
                    exoPlayer.seekTo(position)
                },
                onPrevious = onPrevious,
                onNext = onNext,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                onFullscreenToggle = onFullscreenToggle,
                onFastForward = {
                    val newPosition = (currentPosition + 10000).coerceAtMost(duration)
                    exoPlayer.seekTo(newPosition)
                },
                onFastRewind = {
                    val newPosition = (currentPosition - 10000).coerceAtLeast(0)
                    exoPlayer.seekTo(newPosition)
                },
                currentGatherTitle = currentGatherTitle,
                currentPlayTitle = currentPlayTitle,
                onOrientationToggle = onOrientationToggle,
                modifier = Modifier.fillMaxSize(),
                isOrientationFullscreen = isOrientationFullscreen
            )
        }
        
        // 加载指示器
        if (duration == 0L && playUrl != null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun VideoPlayerControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    isFullscreen: Boolean,
    isOrientationFullscreen: Boolean,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onFullscreenToggle: () -> Unit,
    onFastForward: () -> Unit,
    onFastRewind: () -> Unit,
    currentGatherTitle: String?,
    currentPlayTitle: String?,
    onOrientationToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // 顶部信息栏
        if (!isFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    currentGatherTitle?.let { title ->
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    currentPlayTitle?.let { title ->
                        Text(
                            text = title,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        
        // 中央控制按钮
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 快退按钮
            IconButton(
                onClick = onFastRewind,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = "快退10秒",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 上一集按钮
            IconButton(
                onClick = onPrevious,
                enabled = hasPrevious,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = if (hasPrevious) 0.5f else 0.3f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "上一集",
                    tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 播放/暂停按钮
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // 下一集按钮
            IconButton(
                onClick = onNext,
                enabled = hasNext,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = if (hasNext) 0.5f else 0.3f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "下一集",
                    tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // 快进按钮
            IconButton(
                onClick = onFastForward,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.FastForward,
                    contentDescription = "快进10秒",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // 底部进度条和控制栏
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // 进度条
            VideoProgressBar(
                currentPosition = currentPosition,
                duration = duration,
                bufferedPosition = bufferedPosition,
                onSeekTo = onSeekTo,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 底部控制栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 时间显示
                Text(
                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                    color = Color.White,
                    fontSize = 12.sp
                )
                
                // 右侧按钮组
                Row {
                    if (isFullscreen == isOrientationFullscreen) {
                        // 横竖屏切换按钮
                        IconButton(
                            onClick = { onOrientationToggle() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ScreenRotation,
                                contentDescription = "横竖屏切换",
                                tint = Color.White
                            )
                        }
                    }
                    if (!isOrientationFullscreen) {
                        // 全屏按钮
                        IconButton(
                            onClick = { onFullscreenToggle() }
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                                tint = Color.White
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun VideoProgressBar(
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableStateOf(0f) }
    
    val progress = if (duration > 0) {
        if (isDragging) dragPosition else currentPosition.toFloat() / duration.toFloat()
    } else 0f
    
    val bufferedProgress = if (duration > 0) {
        bufferedPosition.toFloat() / duration.toFloat()
    } else 0f
    
    Column(modifier = modifier) {
        // 进度条
        Slider(
            value = progress,
            onValueChange = { value ->
                isDragging = true
                dragPosition = value
            },
            onValueChangeFinished = {
                isDragging = false
                val seekPosition = (dragPosition * duration).toLong()
                onSeekTo(seekPosition)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}