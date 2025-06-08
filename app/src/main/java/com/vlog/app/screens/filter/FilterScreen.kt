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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vlog.app.R
import com.vlog.app.screens.components.CommonTopBar
import com.vlog.app.screens.components.ErrorView
import com.vlog.app.screens.components.LoadingView
import com.vlog.app.navigation.NavigationRoutes
import com.vlog.app.screens.components.VideoItem
import com.vlog.app.screens.favorites.FavoriteViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    navController: NavController,
    typed: String? = null,
    viewModel: FilterViewModel = hiltViewModel(),
    favoriteViewModel: FavoriteViewModel = hiltViewModel()
) {
    // 如果有 typed 参数，则设置默认分类
    LaunchedEffect(typed) {
        typed?.toIntOrNull()?.let { categoryId ->
            // 找到对应的分类项
            val categoryItem = DefaultFilterConfig.categories.items.find { it.id == categoryId.toString() }
            categoryItem?.let {
                // 更新筛选条件
                viewModel.updateFilter(DefaultFilterConfig.categories, it)
            }
        }
    }
    val uiState by viewModel.uiState.collectAsState()

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
                    modifier = Modifier.padding(8.dp)
                )

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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_videos_found),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {

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
                                    onClick = { navController.navigate("filter_detail/${video.id}") },
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 分类选择

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
