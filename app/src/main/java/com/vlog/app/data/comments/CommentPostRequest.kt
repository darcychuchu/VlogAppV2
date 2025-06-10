package com.vlog.app.data.comments

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data class for the request body when posting a new comment.
 */
@JsonClass(generateAdapter = true)
data class CommentPostRequest(
    @Json(name = "content") // API field name for the comment text
    val content: String
    // Add other fields here if the API requires them, e.g., parentId, quoteId, etc.
    // For now, assuming only content is needed for a new top-level comment.
)
