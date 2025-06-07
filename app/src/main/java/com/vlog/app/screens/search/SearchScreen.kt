package com.vlog.app.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vlog.app.screens.components.VideoItem
import com.vlog.app.screens.favorites.FavoriteViewModel
import kotlin.collections.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    initialQuery: String = "",
    viewModel: SearchViewModel = hiltViewModel(),
    favoriteViewModel: FavoriteViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val hotSearches by viewModel.hotSearches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // 处理初始查询参数
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            viewModel.updateSearchQuery(initialQuery)
            viewModel.search(initialQuery)
        }
    }
    
    Scaffold(
        topBar = {
            SearchTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onSearch = {
                    viewModel.search(searchQuery)
                    keyboardController?.hide()
                },
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (searchResults.isEmpty() && !isLoading) {
                // 显示搜索历史和热门搜索
                if (searchHistory.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "搜索历史",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(
                                onClick = { viewModel.clearAllSearchHistory() }
                            ) {
                                Text("清空")
                            }
                        }
                    }
                    
                    items(searchHistory) { historyEntity ->
                        SearchHistoryItem(
                            query = historyEntity.query,
                            onQueryClick = {
                                viewModel.updateSearchQuery(historyEntity.query)
                                viewModel.search(historyEntity.query)
                            },
                            onDeleteClick = {
                                viewModel.deleteSearchHistory(historyEntity)
                            }
                        )
                    }
                }
                
                if (hotSearches.isNotEmpty()) {
                    item {
                        // 热门搜索
                        Text(
                            text = "热门搜索",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(hotSearches) { video ->
                                HotSearchChip(
                                    query = video.title ?: "",
                                    onClick = {
                                        viewModel.updateSearchQuery(video.title ?: "")
                                        viewModel.search(video.title ?: "")
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                // 显示搜索结果
                items(searchResults) { video ->
                    VideoItem(
                        video = video,
                        favoriteViewModel = favoriteViewModel,
                        onClick = { onVideoClick(video.id ?: "") },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("搜索视频...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") }
                        ) {
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
                modifier = Modifier.fillMaxWidth()
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

@Composable
fun SearchHistoryItem(
    query: String,
    onQueryClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onQueryClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = query,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDeleteClick
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun HotSearchChip(
    query: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = query,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 14.sp
        )
    }
}