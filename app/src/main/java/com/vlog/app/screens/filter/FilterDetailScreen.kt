package com.vlog.app.screens.filter // Or com.vlog.app.screens.videos

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage // Assuming Coil is used for image loading
import com.vlog.app.screens.components.GatherListDialog
// import com.vlog.app.data.videos.GatherList // Not strictly needed here if uiState handles the type
// import com.vlog.app.R // If you need string resources
// import androidx.navigation.NavController // If needed for back navigation

@OptIn(ExperimentalMaterial3Api::class) // For TopAppBar
@Composable
fun FilterDetailScreen(
    videoId: String,
    onNavigateBack: () -> Unit,
    viewModel: FilterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.video?.title ?: "Video Detail") },
                navigationIcon = {
                     IconButton(onClick = { onNavigateBack() }) {
                         Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                     }
                 }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            when {
                uiState.isLoading && uiState.video == null -> { // Show full screen loading only if no data yet
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().align(Alignment.Center)) {
                        Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error) // Replace "Error: " with stringResource
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.retryFetch() }) {
                            Text("Retry") // Replace "Retry" with stringResource
                        }
                    }
                }
                uiState.video != null -> {
                    val video = uiState.video!!
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        AsyncImage(
                            model = video.coverUrl,
                            contentDescription = video.title, // Consider null check or placeholder
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp) // Example height
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = video.title ?: "No Title", style = MaterialTheme.typography.headlineSmall) // Replace "No Title" with stringResource
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Description:", style = MaterialTheme.typography.titleMedium) // Replace "Description:" with stringResource
                        Text(text = video.description ?: "No description available.", style = MaterialTheme.typography.bodyMedium) // Replace "No description available." with stringResource
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "Director: ${video.director ?: "N/A"}", style = MaterialTheme.typography.bodyMedium) // Replace "Director: " and "N/A" with stringResource
                        Text(text = "Actors: ${video.actors ?: "N/A"}", style = MaterialTheme.typography.bodyMedium) // Replace "Actors: " and "N/A" with stringResource
                        Text(text = "Region: ${video.region ?: "N/A"}", style = MaterialTheme.typography.bodyMedium) // Replace "Region: " and "N/A" with stringResource
                        Text(text = "Released At: ${video.releasedAt}", style = MaterialTheme.typography.bodyMedium) // Replace "Released At: " with stringResource, consider formatting date

                        // Add more fields like score, tags, remarks etc.
                        // ...

                        // Display gatherList items if they were part of uiState and fetched
                        // For example:
                        // uiState.gatherList.forEach { item -> Text(item.name) }

                        Spacer(modifier = Modifier.height(16.dp))

                        // GatherList Section
                        when {
                            uiState.isGatherListLoading -> {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                            }
                            uiState.gatherListError != null -> {
                                Text(
                                    text = "Error loading episodes: ${uiState.gatherListError}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            !uiState.gatherList.isNullOrEmpty() -> {
                                Button(
                                    onClick = { viewModel.onShowGatherListDialog() },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("播放列表") // "Episode List" or "Show Episodes"
                                }
                            }
                            else -> {
                                Text(
                                    "No episodes available.",
                                     modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }

                        // Show a subtle loading indicator if background refresh is happening for the main video detail
                        if (uiState.isLoading && uiState.video != null) {
                             Spacer(modifier = Modifier.height(16.dp))
                             CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                        }
                    }
                }
                else -> { // Should not happen if logic is correct, but as a fallback
                    Text("No video data available.", modifier = Modifier.align(Alignment.Center)) // Replace with stringResource
                }
            }

            // Display GatherListDialog
            if (uiState.showGatherListDialog && !uiState.gatherList.isNullOrEmpty()) {
                GatherListDialog(
                    gatherList = uiState.gatherList!!,
                    currentGatherIndex = 0, // Placeholder
                    currentPlayIndex = 0, // Placeholder
                    onGatherSelected = { gatherIdx, playIdx ->
                        // Log or handle selection if needed in ViewModel
                        // viewModel.handleGatherSelection(gatherIdx, playIdx)
                        viewModel.onDismissGatherListDialog() // Dialog itself also calls onDismiss
                    },
                    onPlaySourceSelected = { playIdx ->
                        // Log or handle selection if needed in ViewModel
                        // viewModel.handlePlaySourceSelection(playIdx)
                        viewModel.onDismissGatherListDialog() // Dialog itself also calls onDismiss
                    },
                    onDismiss = { viewModel.onDismissGatherListDialog() }
                )
            }
        }
    }
}
