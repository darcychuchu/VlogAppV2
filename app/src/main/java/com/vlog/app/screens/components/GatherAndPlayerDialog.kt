package com.vlog.app.screens.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vlog.app.data.videos.GatherList
import com.vlog.app.data.videos.PlayList

/**
 * 服务商和播放列表整合对话框
 */
@Composable
fun GatherAndPlayerDialog(
    gathers: List<GatherList>,
    players: List<PlayList>,
    selectedGatherId: String?,
    selectedPlayerUrl: String?,
    isLoadingPlayers: Boolean,
    onGatherSelected: (String) -> Unit,
    onPlayerSelected: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "选择视频来源和剧集",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider()

                // 服务商列表（横向滚动）
                Text(
                    text = "视频来源",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // 使用 rememberLazyListState 来控制滚动位置
                val gatherListState = rememberLazyListState()

                // 自动滚动到选中的服务商
                LaunchedEffect(selectedGatherId) {
                    selectedGatherId?.let { gatherId ->
                        val selectedIndex = gathers.indexOfFirst { it.gatherId == gatherId }
                        if (selectedIndex >= 0) {
                            gatherListState.animateScrollToItem(selectedIndex)
                        }
                    }
                }

                LazyRow(
                    state = gatherListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(gathers) { gather ->
                        val isSelected = gather.gatherId == selectedGatherId
                        GatherChip(
                            gather = gather,
                            isSelected = isSelected,
                            onClick = { onGatherSelected(gather.gatherId) }
                        )
                    }
                }

                HorizontalDivider()

                // 播放列表
                Text(
                    text = "剧集列表",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (isLoadingPlayers) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (players.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "请先选择一个视频来源",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // 使用 rememberLazyListState 来控制滚动位置
                    val playerListState = rememberLazyListState()

                    // 自动滚动到选中的剧集
                    LaunchedEffect(selectedPlayerUrl) {
                        selectedPlayerUrl?.let { url ->
                            val playerIndex = players.indexOfFirst { it.playUrl == url }
                            if (playerIndex >= 0) {
                                // 计算行索引（4个一行）
                                val rowIndex = playerIndex / 4
                                playerListState.animateScrollToItem(rowIndex)
                            }
                        }
                    }

                    LazyColumn(
                        state = playerListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(players.chunked(4)) { rowPlayers ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPlayers.forEach { player ->
                                    val isSelected = player.playUrl == selectedPlayerUrl
                                    PlayerChip(
                                        player = player,
                                        isSelected = isSelected,
                                        onClick = {
                                            onPlayerSelected(player.playUrl.toString(), player.title)
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // 填充空白位置，保持对齐
                                repeat(4 - rowPlayers.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            }
        }
    }
}

/**
 * 服务商选项
 */
@Composable
private fun GatherChip(
    gather: GatherList,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = gather.gatherTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 如果有集数信息，显示集数
            val countVideo = gather.playList?.size ?: 0
            if (countVideo > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($countVideo)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 播放列表项
 */
@Composable
private fun PlayerChip(
    player: PlayList,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary
               else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.title ?: "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
