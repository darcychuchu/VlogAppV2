package com.vlog.app.data.comments

import com.vlog.app.data.ApiResponse // Assuming this is the generic response wrapper
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
// import retrofit2.http.POST // For future postComment method
// import retrofit2.http.Body // For future postComment method

interface CommentService {

    /**
     * Fetches a list of comments for a specific video.
     * @param videoId The ID of the video for which to fetch comments.
     * @param page The page number for pagination of comments.
     * @param pageSize The number of comments to fetch per page.
     * @return ApiResponse containing a list of Comments.
     */
    @GET("videos/{videoId}/comments") // Placeholder endpoint, confirm with actual API
    suspend fun getComments(
        @Path("videoId") videoId: String,
        @Query("page") page: Int,
        @Query("size") pageSize: Int = 20 // Changed "pageSize" to "size" to match VideoService
    ): ApiResponse<List<Comments>>

    // TODO: Add a service method for posting a comment if required by the API.
    // Example:
    // @POST("videos/{videoId}/comments")
    // suspend fun postComment(
    //     @Path("videoId") videoId: String,
    //     @Body commentRequestBody: CommentPostRequest // Define this request body data class
    // ): ApiResponse<Comments> // Or ApiResponse<Unit> or the created Comment
}
