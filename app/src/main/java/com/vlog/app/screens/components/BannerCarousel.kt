package com.vlog.app.screens.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vlog.app.data.videos.Videos
import kotlinx.coroutines.delay
import kotlin.also
import kotlin.math.absoluteValue
import kotlin.ranges.coerceIn

/**
 * 轮播图组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarousel(
    banners: List<Videos>,
    onBannerClick: (Videos) -> Unit,
    modifier: Modifier = Modifier,
    autoScrollDuration: Long = 3000 // 自动滚动间隔，单位毫秒
) {
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { banners.size })

    // 自动滚动
    LaunchedEffect(banners.size) {
        while (true) {
            delay(autoScrollDuration)
            val nextPage = (pagerState.currentPage + 1) % banners.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                        .graphicsLayer {
                            // 计算当前页面的偏移量
                            val pageOffset = (
                                (pagerState.currentPage - page) + pagerState
                                    .currentPageOffsetFraction
                            ).absoluteValue

                            // 应用动画效果
                            lerp(
                                start = 0.85f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            ).also { scale ->
                                scaleX = scale
                                scaleY = scale
                            }

                            // 添加透明度效果
                            alpha = lerp(
                                start = 0.5f,
                                stop = 1f,
                                fraction = 1f - pageOffset.coerceIn(0f, 1f)
                            )
                        }
                        .clickable { onBannerClick(banners[page]) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 图片
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(banners[page].coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = banners[page].title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // 标题背景渐变
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = banners[page].title?:"",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 指示器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(banners.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val size by animateFloatAsState(
                        targetValue = if (isSelected) 10f else 6f,
                        animationSpec = tween(durationMillis = 300),
                        label = "indicator size"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(size.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
    }
}
