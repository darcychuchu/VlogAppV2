package com.vlog.app.data.comments

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey
    val id: String, // Assuming 'id' from Comments data class is unique and non-null for entity

    @ColumnInfo(index = true) // Indexed for faster lookups by videoId
    val videoId: String,

    val createdAt: Long?,
    val isTyped: Int?,
    val quoteId: String?,
    val parentId: String?,
    val title: String?,
    val description: String?,
    val createdBy: String?,
    val nickName: String?,
    val avatar: String?,

    var lastRefreshed: Long? // Timestamp for caching logic
)

// Helper function to map from the network model (Comments) to CommentEntity
fun Comments.toEntity(videoId: String): CommentEntity {
    return CommentEntity(
        id = this.id ?: throw IllegalArgumentException("Comment ID cannot be null for entity"),
        videoId = videoId,
        createdAt = this.createdAt,
        isTyped = this.isTyped,
        quoteId = this.quoteId,
        parentId = this.parentId,
        title = this.title,
        description = this.description,
        createdBy = this.createdBy,
        nickName = this.nickName,
        avatar = this.avatar,
        lastRefreshed = System.currentTimeMillis() // Set lastRefreshed on creation/update
    )
}

// Helper function to map from CommentEntity to the domain/UI model (Comments)
fun CommentEntity.toDomain(): Comments {
    return Comments(
        id = this.id,
        createdAt = this.createdAt,
        isTyped = this.isTyped,
        quoteId = this.quoteId,
        parentId = this.parentId,
        title = this.title,
        description = this.description,
        createdBy = this.createdBy,
        nickName = this.nickName,
        avatar = this.avatar
        // lastRefreshed is not part of the Comments domain model
    )
}
