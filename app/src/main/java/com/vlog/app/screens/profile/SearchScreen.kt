package com.vlog.app.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vlog.app.data.histories.search.SearchHistoryEntity
import com.vlog.app.data.videos.Videos
import com.vlog.app.screens.favorites.FavoriteViewModel
import kotlin.collections.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel(),
    favoriteViewModel: FavoriteViewModel = hiltViewModel()
) {
    val uiState by searchViewModel.uiState.collectAsState()
    val searchHistory by searchViewModel.searchHistory.collectAsState(initial = emptyList())
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var searchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "搜索",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索框
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { 
                    searchViewModel.search(searchQuery)
                    keyboardController?.hide()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // 错误提示
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { searchViewModel.clearError() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 搜索历史和结果
                if (searchQuery.isEmpty() && searchHistory.isNotEmpty()) {
                    SearchHistorySection(
                        searchHistory = searchHistory,
                        onHistoryClick = { query ->
                            searchQuery = query
                            searchViewModel.search(query)
                        },
                        onDeleteHistory = { history ->
                            searchViewModel.deleteSearchHistory(history)
                        },
                        onClearAll = {
                            searchViewModel.clearAllSearchHistory()
                        }
                    )
                }
                
                // 搜索结果
                if (uiState.searchResults.isNotEmpty()) {
                    SearchResultsSection(
                        results = uiState.searchResults,
                        onVideoClick = onVideoClick,
                        favoriteViewModel = favoriteViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("搜索视频...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清空"
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun SearchHistorySection(
    searchHistory: List<SearchHistoryEntity>,
    onHistoryClick: (String) -> Unit,
    onDeleteHistory: (SearchHistoryEntity) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索历史",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            TextButton(onClick = onClearAll) {
                Text("清空全部")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn {
            items(searchHistory) { history ->
                SearchHistoryItem(
                    history = history,
                    onClick = { onHistoryClick(history.query) },
                    onDelete = { onDeleteHistory(history) }
                )
            }
        }
    }
}

@Composable
fun SearchHistoryItem(
    history: SearchHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = history.query,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SearchResultsSection(
    results: List<Videos>,
    onVideoClick: (String) -> Unit,
    favoriteViewModel: FavoriteViewModel
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "搜索结果 (${results.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results) { video ->
                SearchVideoCard(
                    video = video,
                    onClick = { onVideoClick(video.id!!)},
                    favoriteViewModel = favoriteViewModel
                )
            }
        }
    }
}

@Composable
fun SearchVideoCard(
    video: Videos,
    onClick: () -> Unit,
    favoriteViewModel: FavoriteViewModel
) {
    val favoriteVideos by favoriteViewModel.favoriteVideosWithVideo.collectAsState()
    val isFavorite = favoriteVideos.any { it.video?.id == video.id }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // 视频封面
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
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
                
                // 收藏按钮
                IconButton(
                    onClick = {
                        if (isFavorite) {
                            favoriteViewModel.removeFromFavorites(video.id!!) { _, _ -> }
                        } else {
                            favoriteViewModel.addToFavorites(video.id!!) { _, _ -> }
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "取消收藏" else "收藏",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
                
                // 评分
                video.score?.takeIf { it.isNotBlank() }?.let { score ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
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
            }
            
            // 视频信息
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = video.title ?: "未知标题",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                
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
                
                video.publishedAt?.takeIf { it.isNotBlank() }?.let { publishedAt ->
                    Text(
                        text = publishedAt,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}