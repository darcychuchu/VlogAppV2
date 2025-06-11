package com.vlog.app.screens.profile

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vlog.app.navigation.NavigationRoutes
import com.vlog.app.screens.components.StoryItem
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileUserScreen(
    navController: NavController,
    onNavigateToLogin: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val isLoggedIn by profileViewModel.isLoggedIn.collectAsState()
    val currentUser by profileViewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isLoggedIn && currentUser != null) {
                            currentUser?.nickName ?: currentUser?.name ?: "Profile"
                        } else {
                            "Profile"
                        }
                    )
                },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = {  }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Settings"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (isLoggedIn) {
                ProfileLoggedInContent(navController = navController, viewModel = profileViewModel)
            } else {
                ProfileLoggedOutContent(navController = navController,onNavigateToLogin = onNavigateToLogin)
            }
        }
    }
}

@Composable
fun ProfileLoggedOutContent(navController: NavController,
                            onNavigateToLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Not Logged In",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("您尚未登录", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigateToLogin }) {
            Text("登录 / 注册")
        }
        Spacer(modifier = Modifier.height(32.dp))
        // Navigation items for logged-out users
        SettingsNavigationItem(
            icon = Icons.Outlined.History,
            text = "观看历史",
            onClick = { navController.navigate(ProfileScreenRoute.WatchHistory.route) }
        )
        SettingsNavigationItem(
            icon = Icons.Outlined.Search,
            text = "搜索历史",
            onClick = { navController.navigate(ProfileScreenRoute.Search.createRoute()) }
        )
        SettingsNavigationItem(
            icon = Icons.Outlined.SystemUpdate,
            text = "版本更新",
            onClick = { navController.navigate(ProfileScreenRoute.AppUpdate.route) }
        )
    }
}

@Composable
fun ProfileLoggedInContent(navController: NavController, viewModel: ProfileViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val followerCount by viewModel.followerCount.collectAsState()
    val followingCount by viewModel.followingCount.collectAsState()

    val userStories by viewModel.userStories.collectAsState()
    val storiesLoading by viewModel.storiesLoading.collectAsState()
    val storiesError by viewModel.storiesError.collectAsState()
    val hasMoreStories by viewModel.hasMoreStories.collectAsState()
    val listState = rememberLazyGridState() // Changed to LazyGridState

    // Trigger load more when the last item is almost visible
    LaunchedEffect(listState, userStories, hasMoreStories, storiesLoading) {
        val layoutInfo = listState.layoutInfo
        if (layoutInfo.visibleItemsInfo.isNotEmpty() && !storiesLoading && hasMoreStories) {
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.last().index
            if (lastVisibleItemIndex >= userStories.size - 5) { // Threshold for loading more
                viewModel.loadMoreUserStories()
            }
        }
    }


    Column(modifier = Modifier.fillMaxSize()) {
        // User Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentUser?.avatar ?: "")

                    .build(),
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentUser?.nickName ?: currentUser?.name ?: "User",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            currentUser?.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Stats Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(count = followerCount, label = "粉丝", onClick = {
                //navController.navigate(ProfileScreen.Followers.route)
            })
            StatItem(count = followingCount, label = "关注", onClick = {
                //navController.navigate(ProfileScreen.Following.route)
            })
            StatItem(count = currentUser?.points ?: 0, label = "积分", onClick = { /* Optional: Navigate to points screen */ })
        }

        // User Stories
        if (storiesLoading && userStories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (storiesError != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $storiesError", color = MaterialTheme.colorScheme.error)
            }
        } else if (userStories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无动态，快去发布吧！")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // Example: 2 columns
                state = listState,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f) // Make sure LazyVerticalGrid takes available space
            ) {
                items(userStories, key = { it.id ?: java.util.UUID.randomUUID().toString() }) { story ->
                    // Assuming StoryItem is adapted for a grid or you use a different item composable
                    StoryItem( // You might need to adjust StoryItem or create a new one for grid
                        story = story,
                        onClick = {
                            story.id?.let { storyId ->
                                // For current user's stories, username might not be needed or can be from currentUser
                                val authorUsername = currentUser?.name ?: "unknown"
                                navController.navigate(NavigationRoutes.OtherRoute.UserStoryDetail.createRoute(authorUsername, storyId))
                            }
                        },
                        // Pass navController if StoryItem's onUserClick needs it, or handle here
                        // onUserClick = { username -> navController.navigate(...) }
                    )
                }
                if (hasMoreStories && !storiesLoading) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { // Span across all columns
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                           Button(onClick = { viewModel.loadMoreUserStories() }) {
                               Text("Load More")
                           }
                        }
                    }
                } else if (storiesLoading && userStories.isNotEmpty()) {
                     item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(count: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SettingsNavigationItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

