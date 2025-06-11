package com.vlog.app.data.comments

data class CommentPostRequest(
    val title: String?,
    val description: String,
    // Potentially add quoteId and commentType here if the API requires them in the body
    // For now, assuming they are path/query parameters or handled by the repository/service layer.
    // val quoteId: String,
    // val commentType: String
)
