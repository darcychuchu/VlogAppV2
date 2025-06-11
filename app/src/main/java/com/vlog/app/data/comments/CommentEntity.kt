package com.vlog.app.data.comments

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey
    val id: String, // Assuming 'id' from Comments data class is unique and non-null for entity
    val createdAt: Long?,
    val isTyped: Int?, // Consider if this is related to commentType or a different flag
    @ColumnInfo(index = true)
    val quoteId: String, // Made non-null
    val parentId: String?,
    val title: String?,
    val description: String?,
    val createdBy: String?,
    val nickName: String?,
    val avatar: String?,
    var lastRefreshed: Long? // Timestamp for caching logic
)

// Helper function to map from the network model (Comments) to CommentEntity
// videoId parameter is removed as quoteId and commentType are now primary identifiers
fun Comments.toEntity(): CommentEntity {
    return CommentEntity(
        id = this.id ?: throw IllegalArgumentException("Comment ID cannot be null for entity"),
        createdAt = this.createdAt,
        isTyped = this.isTyped,
        quoteId = this.quoteId, // This is now non-null in Comments domain model
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
        quoteId = this.quoteId, // Non-null in entity
        parentId = this.parentId,
        title = this.title,
        description = this.description,
        createdBy = this.createdBy,
        nickName = this.nickName,
        avatar = this.avatar
        // lastRefreshed is not part of the Comments domain model
    )
}
