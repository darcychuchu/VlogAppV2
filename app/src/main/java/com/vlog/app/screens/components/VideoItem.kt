package com.vlog.app.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vlog.app.data.videos.Videos
import com.vlog.app.navigation.NavigationRoutes // Added for login navigation
import com.vlog.app.screens.favorites.FavoriteViewModel

// 视频项组件
@Composable
fun VideoItem(
    video: Videos,
    onClick: () -> Unit,
    navController: NavController, // Added NavController
    modifier: Modifier = Modifier,
    favoriteViewModel: FavoriteViewModel = hiltViewModel()
) {
    val favoriteUiState by favoriteViewModel.uiState.collectAsState()
    val favorites by favoriteViewModel.favorites.collectAsState()

    val isFavorite by favoriteViewModel.isVideoFavoriteFlow(video.id ?: "").collectAsState(initial = false)
    val loginRequired by favoriteViewModel.loginRequiredEvent.collectAsState()

    var showMessage by remember { mutableStateOf<String?>(null) }

    // Handle login required event
    LaunchedEffect(loginRequired) {
        if (loginRequired) {
            navController.navigate(NavigationRoutes.OtherRoute.Login.route)
            favoriteViewModel.consumeLoginRequiredEvent()
        }
    }

    // 显示操作结果消息
    LaunchedEffect(favoriteUiState.lastUpdateMessage) {
        favoriteUiState.lastUpdateMessage?.let { message ->
            showMessage = message
            favoriteViewModel.clearUpdateMessage()
        }
    }

    LaunchedEffect(favoriteUiState.error) {
        favoriteUiState.error?.let { error ->
            showMessage = error
            favoriteViewModel.clearError()
        }
    }

    Column(
        modifier = modifier
            .clickable { onClick() }
    ) {
        // 视频封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 评分 - 右上角
            video.score?.takeIf { it.isNotBlank() }?.let { score ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = score,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 备注 - 左下角
            video.remarks?.takeIf { it.isNotBlank() }?.let { remarks ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = remarks,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 订阅按钮 - 右下角
            IconButton(
                onClick = {
                    video.id?.let { videoId ->
                        if (isFavorite) {
                            favoriteViewModel.removeFromFavorites(videoId) { success, message ->
                                showMessage = message ?: if (success) "取消订阅成功" else "取消订阅失败"
                            }
                        } else {
                            favoriteViewModel.addToFavorites(videoId) { success, message ->
                                showMessage = message ?: if (success) "订阅成功" else "订阅失败"
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "取消订阅" else "订阅",
                    tint = if (isFavorite) Color.Red else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 标题
        Text(
            text = video.title ?: "未知标题",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        // 标签
        video.tags?.takeIf { it.isNotBlank() }?.let { tags ->
            Text(
                text = tags,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // 发布时间
        video.publishedAt?.takeIf { it.isNotBlank() }?.let { publishedAt ->
            Text(
                text = publishedAt,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // 显示操作结果消息
        showMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(2000)
                showMessage = null
            }
            Text(
                text = message,
                fontSize = 9.sp,
                color = if (message.contains("成功")) Color.Green else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
