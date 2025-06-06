package com.vlog.app.data.videos

data class GatherItem(
    val id: String, // Episode ID or unique identifier for the item
    val videoId: String, // Foreign key to the parent VideoEntity.id
    val title: String?,
    val episodeNumber: Int?,
    val playUrl: String?, // Stream URL
    val coverUrl: String?, // Thumbnail for the episode
    val duration: String?,
    val releasedAt: String? // Release date of the episode
    // Add any other relevant fields for an episode or gather list item
)
