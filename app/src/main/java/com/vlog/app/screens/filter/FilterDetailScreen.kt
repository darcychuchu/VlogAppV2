package com.vlog.app.screens.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

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
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {

            when {
                uiState.isLoading && uiState.video == null -> { // Show full screen loading only if no data yet
                    Text(
                        text = "loading ...",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.error != null -> {
                    Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error) // Replace "Error: " with stringResource
                    Button(onClick = { viewModel.retryFetch() }) {
                        Text("Retry") // Replace "Retry" with stringResource
                    }
                }
                uiState.video != null -> {
                    val video = uiState.video!!

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = video.coverUrl,
                                contentDescription = video.title, // Consider null check or placeholder
                                modifier = Modifier.fillMaxWidth() // Example height
                            )
                        }


                        Text(text = video.title ?: "No Title", style = MaterialTheme.typography.headlineSmall) // Replace "No Title" with stringResource

                        ///Text(text = "Description:", style = MaterialTheme.typography.titleMedium) // Replace "Description:" with stringResource
                        ////Text(text = video.description ?: "No description available.", style = MaterialTheme.typography.bodyMedium) // Replace "No description available." with stringResource
                        Text(text = "Director: ${video.director ?: "N/A"}", style = MaterialTheme.typography.bodyMedium) // Replace "Director: " and "N/A" with stringResource
                        ///Text(text = "Actors: ${video.actors ?: "N/A"}", style = MaterialTheme.typography.bodyMedium) // Replace "Actors: " and "N/A" with stringResource
                        ///Text(text = "Region: ${video.region ?: "N/A"}", style = MaterialTheme.typography.bodyMedium) // Replace "Region: " and "N/A" with stringResource
                        Text(text = "Released At: ${video.releasedAt}", style = MaterialTheme.typography.bodyMedium) // Replace "Released At: " with stringResource, consider formatting date



                        // GatherList Section
                        when {
                            uiState.isGatherListLoading -> {
                                Text(
                                    text = "loading ...",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            uiState.gatherListError != null -> {
                                Text(
                                    text = "Error loading episodes: ${uiState.gatherListError}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            !uiState.gatherList.isNullOrEmpty() -> {


                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {

                                    uiState.gatherList?.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text(item.gatherTitle)
                                            if (!item.playList.isNullOrEmpty()) {
                                                Row(
                                                ) {
                                                    item.playList.forEach { player ->
                                                        Text(player.title?:"")
                                                    }
                                                }
                                            }
                                        }

                                    }

                                }


                            }
                            else -> {
                                Text(
                                    "No episodes available."
                                )
                            }
                        }

                        // Show a subtle loading indicator if background refresh is happening for the main video detail
                        if (uiState.isLoading) {
                            Text(
                                text = "loading ...",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                }
                else -> { // Should not happen if logic is correct, but as a fallback
                    Text("No video data available.") // Replace with stringResource
                }
            }

        }
    }
}
