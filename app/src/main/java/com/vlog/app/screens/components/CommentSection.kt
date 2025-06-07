package com.vlog.app.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vlog.app.data.comments.Comments
import java.text.SimpleDateFormat
import java.util.*

/**
 * 评论区组件
 */
@Composable
fun CommentSection(
    comments: List<Comments>,
    isLoading: Boolean,
    onPostComment: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 评论输入框
        CommentInput(onPostComment = onPostComment)

        Spacer(modifier = Modifier.height(16.dp))

        // 评论数量
        Text(
            text = "评论 (${comments.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 评论列表
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无评论，快来发表第一条评论吧！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                comments.forEach { comment ->
                    CommentItem(comment = comment)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 评论输入框
 */
@Composable
fun CommentInput(
    onPostComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = commentText,
            onValueChange = { commentText = it },
            placeholder = { Text("发表评论...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = {
                if (commentText.isNotBlank()) {
                    onPostComment(commentText)
                    commentText = ""
                }
            },
            enabled = commentText.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "发送",
                tint = if (commentText.isNotBlank())
                       MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 评论项
 */
@Composable
fun CommentItem(
    comment: Comments
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 用户头像
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(comment.avatar)
                .crossfade(true)
                .build(),
            contentDescription = "用户头像",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 用户名和时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.nickName ?: "匿名用户",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = formatCommentTime(comment.createdAt.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 评论内容
            Text(
                text = comment.description?:"",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 格式化评论时间
 */
private fun formatCommentTime(timestamp: String?): String {
    if (timestamp == null) return ""

    // 尝试将字符串转换为时间戳
    val date = try {
        val timeInMillis = timestamp.toLongOrNull()
        if (timeInMillis != null) {
            Date(timeInMillis)
        } else {
            // 如果不是数字，尝试解析日期格式
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.parse(timestamp) ?: Date()
        }
    } catch (e: Exception) {
        // 解析失败时返回原始字符串
        return timestamp
    }

    val now = Date()
    val diff = now.time - date.time

    return when {
        diff < 60 * 1000 -> "刚刚"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
        diff < 30 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
        else -> {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.format(date)
        }
    }
}
