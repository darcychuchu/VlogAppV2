package com.vlog.app.data.comments

import com.vlog.app.data.ApiResponse // Assuming this is the generic response wrapper
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
// Removed: import retrofit2.http.Query

interface CommentService {

    /**
     * Fetches a list of comments for a specific video.
     * @param videoId The ID of the video for which to fetch comments.
     * @return ApiResponse containing a list of Comments.
     */
    @GET("videos/comments/{videoId}") // Updated endpoint
    suspend fun getComments(
        @Path("videoId") videoId: String
    ): ApiResponse<List<Comments>>

    /**
     * Posts a new comment to a specific video.
     * @param videoId The ID of the video to which the comment is being posted.
     * @param commentRequestBody The content of the comment.
     * @return ApiResponse containing the newly created Comment.
     */
    @POST("videos/comments-created/{videoId}")
    suspend fun postComment(
        @Path("videoId") videoId: String,
        @Body commentRequestBody: CommentPostRequest
    ): ApiResponse<Comments> // Assuming API returns the created comment
}
