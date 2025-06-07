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
    val videoId: String,
    val version: Int = 0,
    var gatherListJson: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

fun GatherItem.toEntity(): GatherItemEntity {
    return GatherItemEntity(
        videoId = this.videoId,
        version = this.version,
        gatherListJson = this.gatherListJson
        // lastUpdated will use its default value System.currentTimeMillis()
    )
}

fun GatherItemEntity.toDto(): GatherItem {
    return GatherItem(
        videoId = this.videoId,
        version = this.version,
        gatherListJson = this.gatherListJson
    )
}
