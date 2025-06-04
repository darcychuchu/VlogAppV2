package com.vlog.app.screens.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
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

import com.vlog.app.data.videos.Categories
import com.vlog.app.data.videos.VideoList
import com.vlog.app.screens.favorites.FavoriteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    viewModel: VideoViewModel = hiltViewModel(),
    favoriteViewModel: FavoriteViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val paginationState by viewModel.paginationState.collectAsState()
    
    val gridState = rememberLazyGridState()
    val pullToRefreshState = rememberPullToRefreshState()
    
    // 监听滚动到底部，自动加载更多
    LaunchedEffect(gridState, videos.size) {
        snapshotFlow { 
            val layoutInfo = gridState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            
            lastVisibleItemIndex to totalItemsNumber
        }.collect { (lastVisibleItemIndex, totalItemsNumber) ->
            // 当滚动到倒数第3个item时开始加载更多
            if (lastVisibleItemIndex >= totalItemsNumber - 3 && 
                paginationState.hasMorePages && 
                !paginationState.isLoadingMore &&
                !uiState.isLoadingVideos &&
                videos.isNotEmpty()) {
                viewModel.loadMoreVideos()
            }
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("视频列表")
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() }
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
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = uiState.isLoadingVideos && videos.isEmpty(),
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 筛选条件区域
                FilterSection(
                    categories = categories.filter { 
                        it.modelTyped == filterState.selectedType 
                    },
                    filterState = filterState,
                    onFilterChanged = viewModel::updateFilter,
                    modifier = Modifier.padding(16.dp)
                )
                
                // 视频列表区域
                VideoGridSection(
                    videos = videos,
                    isLoading = uiState.isLoadingVideos,
                    isLoadingMore = paginationState.isLoadingMore,
                    hasMorePages = paginationState.hasMorePages,
                    error = uiState.videosError,
                    gridState = gridState,
                    favoriteViewModel = favoriteViewModel,
                    onVideoClick = { video ->
                        // 导航到视频详情页
                        video.id?.let { videoId ->
                            navController?.navigate("video_detail/$videoId")
                        }
                    },
                    onLoadMore = { viewModel.loadMoreVideos() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// 筛选条件组件
@Composable
fun FilterSection(
    categories: List<Categories>,
    filterState: FilterState,
    onFilterChanged: (Int, String?, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 类型筛选
        FilterRow(
            title = "类型",
            items = listOf(
                FilterItem("电影", 1),
                FilterItem("电视剧", 2),
                FilterItem("动漫", 3),
                FilterItem("综艺", 4)
            ),
            selectedValue = filterState.selectedType,
            onItemSelected = { type ->
                // 当类型改变时，重置分类选择
                onFilterChanged(type!!, null, filterState.selectedYear, filterState.selectedSort)
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 分类筛选
        FilterRow<String?>(
            title = "分类",
            items = buildList {
                add(FilterItem("全部", null))
                addAll(categories.map { FilterItem(it.title ?: "未知", it.id?.toString()) })
            },
            selectedValue = filterState.selectedCategoryId,
            onItemSelected = { categoryId ->
                onFilterChanged(filterState.selectedType, categoryId, filterState.selectedYear, filterState.selectedSort)
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 年份筛选
        FilterRow(
            title = "年份",
            items = listOf(
                FilterItem("全部", 0),
                FilterItem("2025", 2025),
                FilterItem("2024", 2024),
                FilterItem("2023", 2023),
                FilterItem("2022", 2022),
                FilterItem("2021年以前", 2021)
            ),
            selectedValue = filterState.selectedYear,
            onItemSelected = { year ->
                onFilterChanged(filterState.selectedType, filterState.selectedCategoryId, year!!, filterState.selectedSort)
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 排序筛选
        FilterRow(
            title = "排序",
            items = listOf(
                FilterItem("最新", 0), // 按上映时间排序
                FilterItem("评分", 1), // 按评分排序
                FilterItem("最热", 2), // 按热度排序
                FilterItem("推荐", 3)  // 按推荐排序
            ),
            selectedValue = filterState.selectedSort,
            onItemSelected = { sort ->
                onFilterChanged(filterState.selectedType, filterState.selectedCategoryId, filterState.selectedYear, sort!!)
            }
        )
    }
}

// 筛选行组件
@Composable
fun <T> FilterRow(
    title: String,
    items: List<FilterItem<T>>,
    selectedValue: T?,
    onItemSelected: (T?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "$title:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items) { item ->
                FilterChip(
                    selected = selectedValue == item.value,
                    onClick = { onItemSelected(item.value) },
                    label = {
                        Text(
                            text = item.label,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}

// 视频网格组件
@Composable
fun VideoGridSection(
    videos: List<VideoList>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMorePages: Boolean,
    error: String?,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    favoriteViewModel: FavoriteViewModel,
    onVideoClick: (VideoList) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            error != null && videos.isEmpty() -> {
                // 错误状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "加载失败",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = error,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            videos.isEmpty() && !isLoading -> {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无视频数据",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            else -> {
                // 视频网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(videos) { video ->
                        VideoItem(
                            video = video,
                            favoriteViewModel = favoriteViewModel,
                            onClick = { onVideoClick(video) }
                        )
                    }
                    
                    // 加载更多指示器
                    if (isLoadingMore) {
                        item(span = { GridItemSpan(3) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    
                    // 加载更多按钮
                    if (videos.isNotEmpty() && !isLoadingMore && hasMorePages) {
                        item(span = { GridItemSpan(3) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = { onLoadMore() },
                                    modifier = Modifier.fillMaxWidth(0.6f)
                                ) {
                                    Text("加载更多")
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                    
                    // 底部占位，确保最后一行能完整显示
                    if (videos.isNotEmpty() && !isLoadingMore && !hasMorePages) {
                        item(span = { GridItemSpan(3) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "没有更多内容了",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
        
        // 初始加载指示器
        if (isLoading && videos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

// 视频项组件
@Composable
fun VideoItem(
    video: VideoList,
    favoriteViewModel: FavoriteViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteUiState by favoriteViewModel.uiState.collectAsState()
    val favorites by favoriteViewModel.favorites.collectAsState()
    // 实时检查订阅状态，基于最新的订阅列表数据
    val isFavorite by favoriteViewModel.isVideoFavoriteFlow(video.id ?: "").collectAsState(initial = false)
    
    var showMessage by remember { mutableStateOf<String?>(null) }
    
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

// 筛选项数据类
data class FilterItem<T>(
    val label: String,
    val value: T
)

