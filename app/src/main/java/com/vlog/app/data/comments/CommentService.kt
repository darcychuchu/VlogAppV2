package com.vlog.app.data.comments

import com.vlog.app.data.ApiResponse // Assuming this is the generic response wrapper
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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
     * @param title The content of the comment.
     * @param description The content of the comment.
     * @return ApiResponse containing the newly created Comment.
     */
    @POST("videos/comments-created/{videoId}")
    suspend fun postComment(
        @Path("videoId") videoId: String,
        @Query("title") title: String?,
        @Query("description") description: String
    ): ApiResponse<Comments> // Assuming API returns the created comment

    /**
     * Fetches a list of comments for a specific story.
     * @param storyId The ID of the story for which to fetch comments.
     * @return ApiResponse containing a list of Comments.
     * Note: The subtask mentioned potentially different return types (PaginatedResponse),
     * but for consistency with getComments and simplicity, using ApiResponse<List<Comments>>.
     */
    @GET("stories/comments/{storyId}") // Example endpoint, adjust as needed
    suspend fun getStoryComments(
        @Path("storyId") storyId: String
        // Add @Query("page") page: Int, @Query("size") size: Int if API supports pagination here
    ): ApiResponse<List<Comments>>

    /**
     * Posts a new comment to a specific story.
     * @param storyId The ID of the story to which the comment is being posted.
     * @param title The title of the comment (optional).
     * @param description The content of the comment.
     * @return ApiResponse containing the newly created Comment.
     */
    @POST("stories/comments-created/{storyId}") // Example endpoint, adjust as needed
    suspend fun postStoryComment(
        @Path("storyId") storyId: String,
        @Query("title") title: String?, // Using @Query to match existing postComment style
        @Query("description") description: String
        // If using CommentPostRequest and @Body:
        // @Body commentRequest: CommentPostRequest
    ): ApiResponse<Comments>
}
