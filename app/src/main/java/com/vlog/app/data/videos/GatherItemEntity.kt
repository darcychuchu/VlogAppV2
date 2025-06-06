package com.vlog.app.data.videos

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gather_items",
    foreignKeys = [ForeignKey(
        entity = VideoEntity::class,
        parentColumns = ["id"],
        childColumns = ["videoId"],
        onDelete = ForeignKey.CASCADE // If a video is deleted, its gather items are also deleted
    )],
    indices = [Index(value = ["videoId"])]
)
data class GatherItemEntity(
    @PrimaryKey
    val id: String,
    val videoId: String,
    val title: String?,
    val episodeNumber: Int?,
    val playUrl: String?,
    val coverUrl: String?,
    val duration: String?,
    val releasedAt: String?,
    // Ensure fields match GatherItem DTO
    val lastUpdated: Long = System.currentTimeMillis() // Timestamp for when this item was synced
)

// Optional: Extension function to map DTO to Entity
fun GatherItem.toEntity(): GatherItemEntity {
    return GatherItemEntity(
        id = this.id,
        videoId = this.videoId,
        title = this.title,
        episodeNumber = this.episodeNumber,
        playUrl = this.playUrl,
        coverUrl = this.coverUrl,
        duration = this.duration,
        releasedAt = this.releasedAt
        // lastUpdated will use its default value System.currentTimeMillis()
    )
}

// Optional: Extension function to map Entity to DTO (if needed)
fun GatherItemEntity.toDto(): GatherItem {
    return GatherItem(
        id = this.id,
        videoId = this.videoId,
        title = this.title,
        episodeNumber = this.episodeNumber,
        playUrl = this.playUrl,
        coverUrl = this.coverUrl,
        duration = this.duration,
        releasedAt = this.releasedAt
    )
}
