package com.vlog.app.data.comments

import android.util.Log
import com.vlog.app.data.database.BaseRepository
import com.vlog.app.data.database.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val commentDao: CommentDao,
    private val commentService: CommentService
) : BaseRepository {

    companion object {
        private const val TAG = "CommentRepository"
        private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    fun getComments(
        videoId: String,
        forceRefresh: Boolean = false // page and pageSize parameters removed
    ): Flow<Resource<List<Comments>>> = flow {
        emit(Resource.Loading())

        val initialLocalComments = try {
            commentDao.getCommentsByVideoId(videoId).firstOrNull()?.map { it.toDomain() } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading initial local comments: ${e.message}", e)
            emptyList<Comments>()
        }

        val oldestTimestamp = try {
            commentDao.getOldestCommentRefreshTimestamp(videoId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting oldest timestamp: ${e.message}", e)
            null
        }

        val initialCommentCount = try {
            commentDao.getCommentCountByVideoId(videoId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting initial comment count: ${e.message}", e)
            0
        }

        val isCacheExpired = oldestTimestamp == null || (System.currentTimeMillis() - oldestTimestamp > CACHE_EXPIRY_MS)
        // Fetch if forced, or cache expired, or if there are no comments locally (implies initial load for this video)
        val needsFetch = forceRefresh || isCacheExpired || initialCommentCount == 0

        if (needsFetch) {
            Log.d(TAG, "Fetching all comments for videoId: $videoId. Reason: forceRefresh=$forceRefresh, isCacheExpired=$isCacheExpired, initialCommentCount=$initialCommentCount")
            // Call updated service method (no pagination)
            val networkResult = safeApiCall { commentService.getComments(videoId).data }

            if (networkResult.isSuccess) {
                val networkComments = networkResult.getOrNull()
                if (networkComments != null) {
                    Log.d(TAG, "Successfully fetched ${networkComments.size} comments from network.")
                    // Always delete existing comments for the video before inserting the new full list
                    Log.d(TAG, "Deleting existing comments for videoId: $videoId before inserting fresh list.")
                    try {
                        commentDao.deleteCommentsByVideoId(videoId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting comments by videoId: $videoId", e)
                        // Decide if we should proceed if delete fails. For now, we will.
                    }

                    try {
                        val commentEntities = networkComments.map { it.toEntity(videoId) }
                        commentDao.insertAll(commentEntities)
                    } catch (e: Exception) {
                         Log.e(TAG, "Error inserting comments for videoId: $videoId", e)
                    }

                    // Fetch the newly inserted comments to ensure UI gets the most up-to-date list from DB
                    val updatedLocalComments = try {
                        commentDao.getCommentsByVideoId(videoId).firstOrNull()?.map { it.toDomain() } ?: emptyList()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading updated local comments: ${e.message}", e)
                        // If read fails, we could emit networkComments mapped to domain, or initialLocalComments as a fallback
                        networkComments.map { it } // Prefer to show what we got from network if DB fails after insert
                    }
                    Log.d(TAG, "Emitting Resource.Success with ${updatedLocalComments.size} comments from DB.")
                    emit(Resource.Success(updatedLocalComments))
                } else { // Network call successful but data is null (e.g. API returns success with empty body for no comments)
                    Log.w(TAG, "Network call successful but no comment data received (data is null).")
                    // This means there are no comments for this video on the server.
                    // Clear local cache as well.
                    try {
                        commentDao.deleteCommentsByVideoId(videoId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting comments by videoId: $videoId after null network response", e)
                    }
                    emit(Resource.Success(emptyList()))
                }
            } else { // Network call failed
                val errorMsg = networkResult.exceptionOrNull()?.message ?: "Unknown error fetching comments"
                Log.e(TAG, "API Error fetching comments: $errorMsg")
                // Emit error but provide existing local comments if available (stale data)
                emit(Resource.Error(errorMsg, initialLocalComments))
            }
        } else { // Cache is valid and not forced refresh
            Log.d(TAG, "Cache valid for videoId: $videoId. Emitting local comments: ${initialLocalComments.size}")
            emit(Resource.Success(initialLocalComments))
        }
    }.flowOn(Dispatchers.IO)

    fun postComment(videoId: String, content: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        Log.d(TAG, "Posting comment for videoId: $videoId, content: '$content'")

        val requestBody = CommentPostRequest(content = content)

        // Assuming safeApiCall handles ApiResponse and extracts .data
        val networkResult = safeApiCall { commentService.postComment(videoId, requestBody).data }

        if (networkResult.isSuccess) {
            val postedComment = networkResult.getOrNull()
            if (postedComment != null) {
                Log.d(TAG, "Successfully posted comment. API returned: $postedComment")
                try {
                    // Add to local database
                    val commentEntity = postedComment.toEntity(videoId)
                    // postedComment.toEntity(videoId) should set lastRefreshed via System.currentTimeMillis()
                    commentDao.insertAll(listOf(commentEntity))
                    Log.d(TAG, "Inserted posted comment into local DB.")
                    emit(Resource.Success(Unit))
                } catch (e: Exception) {
                    Log.e(TAG, "DB Error inserting posted comment: ${e.message}", e)
                    // API post was successful, but local save failed.
                    // Emit Success as the primary operation succeeded.
                    // The local cache will be updated on the next fetch.
                    emit(Resource.Success(Unit))
                }
            } else {
                // This case might occur if API returns 200 OK but with null data in ApiResponse.data
                // This could mean the API confirms the post but doesn't return the full object,
                // or it's an empty list/object response.
                Log.w(TAG, "Comment posted successfully, but API did not return the comment object or data was null.")
                // Consider this a success for the operation of posting.
                // The local cache will update on the next full refresh triggered by the ViewModel.
                emit(Resource.Success(Unit))
            }
        } else {
            val errorMsg = networkResult.exceptionOrNull()?.message ?: "Unknown error posting comment"
            Log.e(TAG, "API Error posting comment: $errorMsg")
            emit(Resource.Error(errorMsg, null))
        }
    }.flowOn(Dispatchers.IO)
}
