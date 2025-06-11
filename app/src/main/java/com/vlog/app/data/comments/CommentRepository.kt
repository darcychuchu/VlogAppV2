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
        const val TYPE_VIDEO = "VIDEO"
        const val TYPE_STORY = "STORY"
    }

    fun getComments(
        quoteId: String,
        commentType: String,
        forceRefresh: Boolean = false
    ): Flow<Resource<List<Comments>>> = flow {
        emit(Resource.Loading())
        Log.d(TAG, "getComments called for quoteId: $quoteId, type: $commentType, forceRefresh: $forceRefresh")

        val initialLocalComments = try {
            commentDao.getCommentsByQuoteIdAndType(quoteId, commentType).firstOrNull()?.map { it.toDomain() } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading initial local comments for $quoteId ($commentType): ${e.message}", e)
            emptyList<Comments>()
        }

        val oldestTimestamp = try {
            commentDao.getOldestCommentRefreshTimestamp(quoteId, commentType)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting oldest timestamp for $quoteId ($commentType): ${e.message}", e)
            null
        }

        val initialCommentCount = try {
            commentDao.getCommentCountByQuoteIdAndType(quoteId, commentType)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting initial comment count for $quoteId ($commentType): ${e.message}", e)
            0
        }

        val isCacheExpired = oldestTimestamp == null || (System.currentTimeMillis() - oldestTimestamp > CACHE_EXPIRY_MS)
        val needsFetch = forceRefresh || isCacheExpired || initialCommentCount == 0

        if (needsFetch) {
            Log.d(TAG, "Fetching comments for $quoteId ($commentType). Reason: forceRefresh=$forceRefresh, isCacheExpired=$isCacheExpired, initialCommentCount=$initialCommentCount")

            val networkResult = safeApiCall {
                when (commentType) {
                    TYPE_VIDEO -> commentService.getComments(videoId = quoteId).data
                    TYPE_STORY -> commentService.getStoryComments(storyId = quoteId).data
                    else -> throw IllegalArgumentException("Unknown commentType: $commentType")
                }
            }

            if (networkResult.isSuccess) {
                var networkComments = networkResult.getOrNull()
                if (networkComments != null) {
                    // Populate quoteId and commentType in each comment from network before saving
                    networkComments = networkComments.map { comment ->
                        // The 'Comments' domain object now requires quoteId and commentType in constructor.
                        // Assuming API response for Comments does not include these, we must add them.
                        // If API *does* include them, this explicit mapping might be redundant but safe.
                        comment.copy(
                            quoteId = quoteId, // Ensure this is set from the context
                            commentType = commentType // Ensure this is set from the context
                        )
                    }
                    Log.d(TAG, "Successfully fetched ${networkComments.size} comments from network for $quoteId ($commentType).")
                    Log.d(TAG, "Deleting existing comments for $quoteId ($commentType) before inserting fresh list.")
                    try {
                        commentDao.deleteCommentsByQuoteIdAndType(quoteId, commentType)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting comments for $quoteId ($commentType): ${e.message}", e)
                    }

                    try {
                        // Comments.toEntity() now uses quoteId and commentType from the Comments object itself
                        val commentEntities = networkComments.map { it.toEntity() }
                        commentDao.insertAll(commentEntities)
                    } catch (e: Exception) {
                         Log.e(TAG, "Error inserting comments for $quoteId ($commentType): ${e.message}", e)
                    }

                    val updatedLocalComments = try {
                        commentDao.getCommentsByQuoteIdAndType(quoteId, commentType).firstOrNull()?.map { it.toDomain() } ?: emptyList()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading updated local comments for $quoteId ($commentType): ${e.message}", e)
                        networkComments // Prefer to show what we got from network if DB fails after insert
                    }
                    Log.d(TAG, "Emitting Resource.Success with ${updatedLocalComments.size} comments from DB for $quoteId ($commentType).")
                    emit(Resource.Success(updatedLocalComments))
                } else {
                    Log.w(TAG, "Network call successful but no comment data received for $quoteId ($commentType).")
                    try {
                        commentDao.deleteCommentsByQuoteIdAndType(quoteId, commentType)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting comments for $quoteId ($commentType) after null network response", e)
                    }
                    emit(Resource.Success(emptyList()))
                }
            } else {
                val errorMsg = networkResult.exceptionOrNull()?.message ?: "Unknown error fetching comments for $quoteId ($commentType)"
                Log.e(TAG, "API Error fetching comments: $errorMsg")
                emit(Resource.Error(errorMsg, initialLocalComments))
            }
        } else {
            Log.d(TAG, "Cache valid for $quoteId ($commentType). Emitting local comments: ${initialLocalComments.size}")
            emit(Resource.Success(initialLocalComments))
        }
    }.flowOn(Dispatchers.IO)

    fun postComment(
        quoteId: String,
        commentType: String,
        title: String?,
        description: String
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        Log.d(TAG, "Posting comment for $quoteId ($commentType), title: '$title', description: '$description'")

        val networkResult = safeApiCall {
            when (commentType) {
                TYPE_VIDEO -> commentService.postComment(videoId = quoteId, title = title, description = description)
                TYPE_STORY -> commentService.postStoryComment(storyId = quoteId, title = title, description = description)
                else -> throw IllegalArgumentException("Unknown commentType: $commentType")
            }
        }

        if (networkResult.isSuccess) {
            var postedCommentApiResponse = networkResult.getOrNull() // This is ApiResponse<Comments>
            if (postedCommentApiResponse != null && postedCommentApiResponse.data != null) {
                 var postedComment = postedCommentApiResponse.data!! // This is Comments object
                Log.d(TAG, "Successfully posted comment for $quoteId ($commentType). API returned: $postedComment")
                try {
                    // Populate quoteId and commentType before saving to DB
                    postedComment = postedComment.copy(
                        quoteId = quoteId,
                        commentType = commentType
                    )
                    val commentEntity = postedComment.toEntity()
                    commentDao.insertAll(listOf(commentEntity)) // insertAll expects a List
                    Log.d(TAG, "Inserted posted comment into local DB for $quoteId ($commentType).")
                    emit(Resource.Success("Comment posted successfully.")) // Return a meaningful success message
                } catch (e: Exception) {
                    Log.e(TAG, "DB Error inserting posted comment for $quoteId ($commentType): ${e.message}", e)
                    emit(Resource.Success("Comment posted, local save failed."))
                }
            } else {
                Log.w(TAG, "Comment posted successfully for $quoteId ($commentType), but API did not return the comment object or data was null.")
                emit(Resource.Success("Comment posted, no confirmation data."))
            }
        } else {
            val errorMsg = networkResult.exceptionOrNull()?.message ?: "Unknown error posting comment for $quoteId ($commentType)"
            Log.e(TAG, "API Error posting comment: $errorMsg")
            emit(Resource.Error(errorMsg, null))
        }
    }.flowOn(Dispatchers.IO)
}
