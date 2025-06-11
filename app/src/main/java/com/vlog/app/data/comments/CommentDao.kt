package com.vlog.app.data.comments

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CommentEntity>)

    @Query("SELECT * FROM comments WHERE quoteId = :videoId ORDER BY createdAt DESC")
    fun getCommentsByVideoId(videoId: String): Flow<List<CommentEntity>>

    /**
     * Gets the timestamp of the oldest refresh for any comment related to the given videoId.
     * If no comments for the videoId have a lastRefreshed timestamp (e.g., all are null),
     * or if there are no comments for the videoId, this will return null.
     */
    @Query("SELECT MIN(lastRefreshed) FROM comments WHERE quoteId = :videoId")
    suspend fun getOldestCommentRefreshTimestamp(videoId: String): Long?

    @Query("DELETE FROM comments WHERE quoteId = :videoId")
    suspend fun deleteCommentsByVideoId(videoId: String)

    @Query("SELECT COUNT(*) FROM comments WHERE quoteId = :videoId")
    suspend fun getCommentCountByVideoId(videoId: String): Int

    // Optional: A method to insert a single comment if needed for posting.
    // @Insert(onConflict = OnConflictStrategy.REPLACE)
    // suspend fun insertComment(comment: CommentEntity)
}
