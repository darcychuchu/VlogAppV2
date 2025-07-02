package com.vlog.app.screens.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.map
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vlog.app.R
import com.vlog.app.screens.components.CommonTopBar
import com.vlog.app.screens.components.ErrorView
import com.vlog.app.screens.components.LoadingView
import com.vlog.app.navigation.NavigationRoutes
import com.vlog.app.screens.components.BannerAdScreen
import com.vlog.app.screens.components.EmptyView
import com.vlog.app.screens.components.VideoItem
import com.vlog.app.screens.favorites.FavoriteViewModel
import com.vlog.app.screens.users.UserViewModel // Added UserViewModel import
import com.vlog.app.screens.filter.ConfigCategoryItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    navController: NavController,
    typed: String? = null,
    viewModel: FilterViewModel = hiltViewModel(),
    favoriteViewModel: FavoriteViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel() // Instantiated UserViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    // Derive isLoggedIn state reactively
    val isLoggedIn by userViewModel.currentUser.map { it != null }.collectAsState(initial = userViewModel.isLoggedIn())

    // Effect to consume pending subscription after login
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val pendingVideoId = userViewModel.consumePendingSubscription()
            if (pendingVideoId != null) {
                favoriteViewModel.addToFavorites(pendingVideoId) { success, message ->
                    Log.d("FilterScreen", "Processed pending subscription for $pendingVideoId: Success=$success, Msg=$message")
                    // Optionally show a brief toast or snackbar here
                }
            }
        }
    }

    // 如果有 typed 参数，则设置默认分类
    LaunchedEffect(typed) {
        typed?.toIntOrNull()?.let { categoryId ->
            val categoryItem = DefaultFilterConfig.categories.items.find { it.id == categoryId.toString() }
            categoryItem?.let {
                viewModel.updateFilter(DefaultFilterConfig.categories, it)
            }
        }
    }

    Scaffold(
        topBar = {
            CommonTopBar(
                title = stringResource(R.string.videos),
                navController = navController,
                currentRoute = NavigationRoutes.MainRoute.Videos.route
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Filter options
                FilterOptions(
                    uiState = uiState,
                    onFilterUpdate = viewModel::updateFilter,
                    onShowConfig = viewModel::showCategoryConfig,
                    modifier = Modifier.padding(8.dp)
                )

                // 登录提示（页面内显示）
                uiState.loginRequiredMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "需要登录",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { viewModel.clearLoginRequiredMessage() }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Results
                when {
                    uiState.isLoading -> {
                        LoadingView()
                    }
                    uiState.error != null -> {
                        ErrorView(
                            message = uiState.error ?: "Unknown error",
                            onRetry = { viewModel.applyFilters() }
                        )
                    }
                    uiState.videos.isEmpty() -> {
                        EmptyView(
                            message = stringResource(R.string.no_videos_found),
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }
                    else -> {
                        BannerAdScreen()
                        Spacer(modifier = Modifier.height(16.dp))

                        // 视频列表
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.height((uiState.videos.size / 3 * 160).dp)
                        ) {
                            items(uiState.videos) { video ->
                                VideoItem(
                                    video = video,
                                    onClick = { navController.navigate("video_detail/${video.id}") },
                                    navController = navController, // Pass NavController
                                    favoriteViewModel = favoriteViewModel
                                )
                            }

                            // 加载更多指示器
                            if (uiState.isLoadingMore || uiState.canLoadMore) {
                                item(span = { GridItemSpan(3) }) {
                                    LoadMoreIndicator(
                                        isLoading = uiState.isLoadingMore,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                    if (!uiState.isLoadingMore) {
                                        viewModel.loadMoreVideos()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 分类配置对话框
        if (uiState.showCategoryConfig) {
            CategoryConfigDialog(
                categories = uiState.configCategories,
                onDismiss = viewModel::hideCategoryConfig,
                onToggleEnabled = viewModel::updateCategoryEnabled,
                onReorder = viewModel::updateCategoryOrder
            )
        }
    }
}

@Composable
fun LoadMoreIndicator(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = stringResource(R.string.loading_more),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FilterOptions(
    uiState: FilterUiState,
    onFilterUpdate: (FilterSection, FilterItem) -> Unit,
    onShowConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(uiState.mainCategories) { category ->
                FilterChip(
                    selected = category.id == uiState.selectedCategory.id,
                    onClick = { onFilterUpdate(DefaultFilterConfig.categories, category) },
                    label = { Text(category.name, style = MaterialTheme.typography.bodySmall) }
                )
            }

            ///
            item(){
                IconButton(
                    onClick = onShowConfig
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "分类配置",
                    )
                }
            }
        }

        // 子分类选择（如果有）
        if (uiState.subCategories.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (uiState.isLoadingCategories) {
                    Spacer(modifier = Modifier.width(4.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.subCategories) { subCategory ->
                    FilterChip(
                        selected = subCategory.id == uiState.selectedSubCategory?.id,
                        onClick = { onFilterUpdate(FilterSection("子分类", uiState.subCategories, "cate"), subCategory) },
                        label = { Text(subCategory.name, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }
        }


        // 年份选择

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DefaultFilterConfig.years.items) { year ->
                FilterChip(
                    selected = year.id == uiState.selectedYear.id,
                    onClick = { onFilterUpdate(DefaultFilterConfig.years, year) },
                    label = { Text(year.name, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        // 排序选择

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DefaultFilterConfig.orderBy.items) { orderBy ->
                FilterChip(
                    selected = orderBy.id == uiState.selectedOrderBy.id,
                    onClick = { onFilterUpdate(DefaultFilterConfig.orderBy, orderBy) },
                    label = { Text(orderBy.name, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }


    }
}

@Composable
fun CategoryConfigDialog(
    categories: List<ConfigCategoryItem>,
    onDismiss: () -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onReorder: (List<ConfigCategoryItem>) -> Unit
) {
    var reorderableCategories by remember(categories) { mutableStateOf(categories) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "分类配置",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp) // 限制对话框高度
            ) {
                Text(
                    text = "管理分类显示和排序",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 添加可滚动的列表
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    reorderableCategories.forEachIndexed { index, category ->
                        CategoryConfigItem(
                            category = category,
                            onToggleEnabled = { enabled ->
                                onToggleEnabled(category.id, enabled)
                            },
                            onMoveUp = if (index > 0) {
                                {
                                    val newList = reorderableCategories.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index - 1]
                                    newList[index - 1] = temp
                                    reorderableCategories = newList
                                    onReorder(newList)
                                }
                            } else null,
                            onMoveDown = if (index < reorderableCategories.size - 1) {
                                {
                                    val newList = reorderableCategories.toMutableList()
                                    val temp = newList[index]
                                    newList[index] = newList[index + 1]
                                    newList[index + 1] = temp
                                    reorderableCategories = newList
                                    onReorder(newList)
                                }
                            } else null,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成")
            }
        }
    )
}

@Composable
fun CategoryConfigItem(
    category: ConfigCategoryItem,
    onToggleEnabled: (Boolean) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (category.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 分类信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (category.isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (category.isLocked) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🔒",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // 控制按钮
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 排序按钮
                Column {
                    IconButton(
                        onClick = { onMoveUp?.invoke() },
                        enabled = onMoveUp != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "↑",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (onMoveUp != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            }
                        )
                    }
                    IconButton(
                        onClick = { onMoveDown?.invoke() },
                        enabled = onMoveDown != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text(
                            text = "↓",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (onMoveDown != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 启用/禁用开关
                Switch(
                    checked = category.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }
        }
    }
}


