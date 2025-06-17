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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
// ExoPlayer related imports are now primarily in ViewModel, PlayerView is for UI
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    modifier: Modifier = Modifier,
    playerViewModel: VideoPlayerViewModel = hiltViewModel(), // Obtain ViewModel
    // Parameters below are mostly driven by ViewModel state or are direct pass-through for controls
    // playUrl: String?, // ViewModel now manages the URL/MediaItem
    isFullscreen: Boolean, // From ViewModel UI State
    // isOrientationFullscreen: Boolean, // From ViewModel UI State - REMOVED
    onFullscreenToggle: () -> Unit, // Calls ViewModel method
    onPrevious: () -> Unit, // Calls ViewModel method
    onNext: () -> Unit, // Calls ViewModel method
    hasPrevious: Boolean, // From ViewModel
    hasNext: Boolean, // From ViewModel
    currentTitle: String?, // From ViewModel PlaylistState
    currentGatherTitle: String?, // From ViewModel PlaylistState
    currentPlayTitle: String?, // From ViewModel PlaylistState
    // onOrientationToggle: () -> Unit = {} // REMOVED
) {
    var showControls by remember { mutableStateOf(true) }
    val uiState by playerViewModel.uiState.collectAsState()
    // val playlistState by playerViewModel.playlistState.collectAsState() // Already used in VideoDetailScreen

    // Controls auto-hide logic
    LaunchedEffect(showControls, uiState.isPlaying) {
        if (showControls && uiState.isPlaying) {
            delay(3000) // Hide controls after 3 seconds of playing
            showControls = false
        }
    }

    // No local ExoPlayer instance, no LaunchedEffect for playUrl, no DisposableEffect for local player release.
    // ViewModel handles ExoPlayer lifecycle and media item changes.

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isFullscreen) Modifier.fillMaxHeight()
                else Modifier.aspectRatio(16f / 9f)
            )
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // ExoPlayer视图
        // Make sure playerViewModel.exoPlayer is not null before passing to PlayerView
        playerViewModel.exoPlayer?.let { exoPlayerInstance ->
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayerInstance
                        useController = false // We use custom controls
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Custom control overlay
        if (showControls) {
            VideoPlayerControls(
                isPlaying = uiState.isPlaying,
                currentPosition = uiState.currentPosition,
                duration = uiState.duration,
                bufferedPosition = uiState.bufferedPosition,
                isFullscreen = isFullscreen, // This is uiState.isFullscreen, passed as parameter
                onPlayPause = {
                    if (uiState.isPlaying) playerViewModel.pause()
                    else playerViewModel.play()
                },
                onSeekTo = { position -> playerViewModel.seekTo(position) },
                onPrevious = onPrevious, // Directly use the passed lambda which calls ViewModel
                onNext = onNext,         // Directly use the passed lambda
                hasPrevious = hasPrevious, // Directly use the passed boolean
                hasNext = hasNext,         // Directly use the passed boolean
                onFullscreenToggle = onFullscreenToggle, // Directly use the passed lambda
                onFastForward = {
                    val newPosition = (uiState.currentPosition + 10000).coerceAtMost(uiState.duration)
                    playerViewModel.seekTo(newPosition)
                },
                onFastRewind = {
                    val newPosition = (uiState.currentPosition - 10000).coerceAtLeast(0)
                    playerViewModel.seekTo(newPosition)
                },
                currentTitle = currentTitle,
                currentGatherTitle = currentGatherTitle, // From ViewModel via parameter
                currentPlayTitle = currentPlayTitle,   // From ViewModel via parameter
                // onOrientationToggle = onOrientationToggle, // REMOVED
                modifier = Modifier.fillMaxSize(),

                // isOrientationFullscreen = isOrientationFullscreen // REMOVED
            )
        }

        // Loading indicator, driven by ViewModel's state
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Display error message if any
        uiState.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: $error \nTap to retry or select another video.",
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.clickable { playerViewModel.clearError() /* TODO: Add retry logic if needed */ }
                )
            }
        }
    }
}

@Composable
fun VideoPlayerControls(
    isPlaying: Boolean, // From ViewModel
    currentPosition: Long, // From ViewModel
    duration: Long, // From ViewModel
    bufferedPosition: Long, // From ViewModel
    isFullscreen: Boolean, // From ViewModel
    // isOrientationFullscreen: Boolean, // REMOVED
    onPlayPause: () -> Unit, // Calls ViewModel method
    onSeekTo: (Long) -> Unit, // Calls ViewModel method
    onPrevious: () -> Unit, // Calls ViewModel method
    onNext: () -> Unit, // Calls ViewModel method
    hasPrevious: Boolean, // From ViewModel
    hasNext: Boolean, // From ViewModel
    onFullscreenToggle: () -> Unit, // Calls ViewModel method
    onFastForward: () -> Unit, // Calls ViewModel method
    onFastRewind: () -> Unit, // Calls ViewModel method
    currentTitle: String?, // From ViewModel
    currentGatherTitle: String?, // From ViewModel
    currentPlayTitle: String?, // From ViewModel
    // onOrientationToggle: () -> Unit, // REMOVED
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color.Transparent)) { // Ensure controls background is transparent


        // 顶部信息栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (isFullscreen) {
                currentTitle?.let { title ->
                    Text(
                        text = "$title / ",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            currentGatherTitle?.let { title ->
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            currentPlayTitle?.let { title ->
                Text(
                    text = " - $title",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
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
                    // The ScreenRotation button is removed.
                    // The Fullscreen button's visibility should solely depend on whether the player
                    // is currently in its "fullscreen UI mode".
                    // if (!isOrientationFullscreen) { // This condition is removed
                    IconButton(
                        onClick = { onFullscreenToggle() }
                    ) {
                        Icon(
                            imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = if (isFullscreen) "退出全屏" else "全屏",
                            tint = Color.White
                        )
                    }
                    // }
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