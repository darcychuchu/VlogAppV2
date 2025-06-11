package com.vlog.app.screens.users

import androidx.compose.foundation.layout.Arrangement
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vlog.app.data.comments.Comments
import com.vlog.app.data.database.Resource
import com.vlog.app.navigation.NavigationRoutes // Added for login navigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserStoriesDetailScreen(
    navController: NavController,
    username: String?,
    storyId: String
) {
    val viewModel: UserStoriesDetailViewModel = hiltViewModel()
    val context = LocalContext.current

    val storyDetail by viewModel.storyDetail.collectAsState()
    val isLoadingStory by viewModel.isLoading.collectAsState() // isLoading for story detail
    val errorStory by viewModel.error.collectAsState()

    val commentsResource by viewModel.comments.collectAsState()
    val postCommentState by viewModel.postCommentState.collectAsState()

    var commentText by remember { mutableStateOf("") }

    // Handle post comment state
    LaunchedEffect(postCommentState) {
        when (val resource = postCommentState) {
            is Resource.Success -> {
                Toast.makeText(context, resource.data ?: "Comment posted", Toast.LENGTH_SHORT).show()
                commentText = "" // Clear text field
                viewModel.clearPostCommentState()
            }
            is Resource.Error -> {
                Toast.makeText(context, resource.message ?: "Failed to post comment", Toast.LENGTH_SHORT).show()
                viewModel.clearPostCommentState()
            }
            is Resource.Loading -> {
                // Optionally show a loading indicator for posting
            }
            null -> {
                // Initial state or cleared state
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = storyDetail?.title ?: "Story Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadComments(forceRefresh = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Comments")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Story Detail Section
            if (isLoadingStory) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (errorStory != null) {
                Text("Error loading story: $errorStory", color = MaterialTheme.colorScheme.error)
            } else if (storyDetail != null) {
                val story = storyDetail!!
                Text("Title: ${story.title ?: "N/A"}", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Description: ${story.description ?: "No description."}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("Author: ${story.nickName ?: story.createdBy ?: viewModel.username}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
            } else {
                Text("No story details available.", modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            // Comments Section
            Text("Comments", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 8.dp))

            when (val res = commentsResource) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is Resource.Success -> {
                    val commentsList = res.data
                    if (commentsList.isNullOrEmpty()) {
                        Text("No comments yet. Be the first to comment!", modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(commentsList) { comment ->
                                CommentItemView(comment) // You'll need to create this Composable
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Text("Error loading comments: ${res.message}", color = MaterialTheme.colorScheme.error)
                }
            }

            // Post Comment Section
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Write a comment...") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (viewModel.isUserLoggedIn()) {
                            if (commentText.isNotBlank()) {
                                viewModel.postComment(title = null, description = commentText)
                            }
                        } else {
                            navController.navigate(NavigationRoutes.OtherRoute.Login.route)
                        }
                    },
                    enabled = commentText.isNotBlank() && postCommentState !is Resource.Loading
                ) {
                    if (postCommentState is Resource.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Post")
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItemView(comment: Comments) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = comment.nickName ?: comment.createdBy ?: "Anonymous",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(text = comment.description ?: "", style = MaterialTheme.typography.bodyMedium)
        // Optionally, add timestamp, etc.
    }
}
