package com.vlog.app.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vlog.app.data.stories.Stories
import com.vlog.app.screens.components.StoryItem

/**
 * 关注标签页内容
 */
@Composable
fun FollowContent(
    storiesList: List<Stories>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    hasMoreData: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToStoryDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToArtworkDetail: (String, String, Boolean) -> Unit = { _, _, _ -> },
    isLoggedIn: Boolean = true
) {
    val listState = rememberLazyListState()

    // 监听滚动到底部，加载更多
    LaunchedEffect(listState.canScrollForward) {
        if (!listState.canScrollForward && !isLoading && hasMoreData && storiesList.isNotEmpty()) {
            onLoadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (!isLoggedIn) {
            // 未登录状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "登录后查看关注内容",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* 导航到登录页面 */ }
                ) {
                    Text("去登录")
                }
            }
        } else if (storiesList.isEmpty() && !isLoading) {
            // 空列表状态
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "暂无关注内容",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "关注更多用户以查看他们的内容",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
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
                            story.id?.let { id ->
                                story.createdBy?.let { userName ->
                                    onNavigateToStoryDetail(userName, id)
                                }
                            }
                        },
                        onUserClick = { userName ->
                            onNavigateToUserProfile(userName)
                        }
                    )
                }

                // 底部加载更多指示器
                if (isLoading && !isRefreshing && storiesList.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        // 加载指示器
        if (isLoading && storiesList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

    }
}
