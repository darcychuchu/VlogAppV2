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
) : BaseRepository() {

    companion object {
        private const val TAG = "CommentRepository"
        private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    fun getComments(
        videoId: String,
        page: Int = 1,
        pageSize: Int = 20,
        forceRefresh: Boolean = false
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
        // Fetch if forced, or cache expired, or if it's the first page and there are no comments locally (implies initial load)
        val needsFetch = forceRefresh || isCacheExpired || (page == 1 && initialCommentCount == 0)


        if (needsFetch) {
            Log.d(TAG, "Fetching comments for videoId: $videoId, page: $page. Reason: forceRefresh=$forceRefresh, isCacheExpired=$isCacheExpired, initialCommentCount=$initialCommentCount, page=$page")
            // Assuming safeApiCall is designed to extract .data from ApiResponse
            val networkResult = safeApiCall { commentService.getComments(videoId, page, pageSize).data }

            if (networkResult.isSuccess) {
                val networkComments = networkResult.getOrNull()
                if (networkComments != null) {
                    Log.d(TAG, "Successfully fetched ${networkComments.size} comments from network for page $page.")
                    if (page == 1) {
                        Log.d(TAG, "Page 1, deleting existing comments for videoId: $videoId")
                        try {
                            commentDao.deleteCommentsByVideoId(videoId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deleting comments by videoId: $videoId", e)
                        }
                    }
                    try {
                        val commentEntities = networkComments.map { it.toEntity(videoId) }
                        commentDao.insertAll(commentEntities)
                    } catch (e: Exception) {
                         Log.e(TAG, "Error inserting comments for videoId: $videoId", e)
                    }

                    val updatedLocalComments = try {
                        commentDao.getCommentsByVideoId(videoId).firstOrNull()?.map { it.toDomain() } ?: emptyList()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading updated local comments: ${e.message}", e)
                        initialLocalComments // Fallback to initially loaded comments if read fails
                    }
                    Log.d(TAG, "Emitting Resource.Success with ${updatedLocalComments.size} comments from DB.")
                    emit(Resource.Success(updatedLocalComments))
                } else {
                    Log.w(TAG, "Network call successful but no comment data received for page $page.")
                    // If it's page 1 and no data, it means no comments. Otherwise, might be end of pagination.
                    if (page == 1) {
                         emit(Resource.Success(emptyList())) // No comments for this video
                    } else {
                         emit(Resource.Success(initialLocalComments)) // No more new comments, stick with what we have
                    }
                }
            } else {
                val errorMsg = networkResult.exceptionOrNull()?.message ?: "Unknown error fetching comments"
                Log.e(TAG, "API Error fetching comments for page $page: $errorMsg")
                emit(Resource.Error(errorMsg, initialLocalComments))
            }
        } else {
            Log.d(TAG, "Cache valid for videoId: $videoId, page: $page. Emitting local comments: ${initialLocalComments.size}")
            emit(Resource.Success(initialLocalComments))
        }
    }.flowOn(Dispatchers.IO)

    fun postComment(videoId: String, content: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        Log.d(TAG, "Attempting to post comment for videoId: $videoId, content: $content")
        // Simulate network delay
        kotlinx.coroutines.delay(1000) // Increased delay for simulation
        Log.w(TAG, "Posting comments is not yet implemented.")
        emit(Resource.Error("Posting comments is not yet implemented.", null))
    }.flowOn(Dispatchers.IO)
}
