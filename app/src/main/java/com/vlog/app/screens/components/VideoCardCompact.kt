package com.vlog.app.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vlog.app.data.videos.Videos

/**
 * 紧凑型视频卡片，用于网格布局
 */
@Composable
fun VideoCardCompact(
    video: Videos,
    modifier: Modifier = Modifier,
    onClick: (Videos) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(video) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column {
            // 封面图片
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.65f) // 更紧凑的宽高比，适合一行3个
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            )

            // 视频信息
            Column(modifier = Modifier.padding(4.dp)) {
                // 标题
                Text(
                    text = video.title ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 地区和评分
                Text(
                    text = "${video.remarks ?: ""} · ${video.score}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.dp.value.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
