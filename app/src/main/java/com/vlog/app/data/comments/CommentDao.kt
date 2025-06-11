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

    @Query("SELECT * FROM comments WHERE quoteId = :quoteId ORDER BY createdAt DESC")
    fun getCommentsByQuoteIdAndType(quoteId: String): Flow<List<CommentEntity>>

    /**
     * Gets the timestamp of the oldest refresh for any comment related to the given quoteId and type.
     * If no comments for the quoteId and type have a lastRefreshed timestamp (e.g., all are null),
     * or if there are no comments for the quoteId and type, this will return null.
     */
    @Query("SELECT MIN(lastRefreshed) FROM comments WHERE quoteId = :quoteId")
    suspend fun getOldestCommentRefreshTimestamp(quoteId: String): Long?

    @Query("DELETE FROM comments WHERE quoteId = :quoteId")
    suspend fun deleteCommentsByQuoteIdAndType(quoteId: String)

    @Query("SELECT COUNT(*) FROM comments WHERE quoteId = :quoteId")
    suspend fun getCommentCountByQuoteIdAndType(quoteId: String): Int

    // Optional: A method to insert a single comment if needed for posting.
    // @Insert(onConflict = OnConflictStrategy.REPLACE)
    // suspend fun insertComment(comment: CommentEntity)
}
