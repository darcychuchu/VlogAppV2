package com.vlog.app.data.favorites

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vlog.app.data.videos.VideoList

@Entity(tableName = "favorite_videos")
data class FavoriteVideoEntity(
    @PrimaryKey
    val videoId: String,
    val title: String?,
    val coverUrl: String?,
    val score: String?,
    val tags: String?,
    val remarks: String?,
    val publishedAt: String?,
    val categoryId: String?,
    val isTyped: Int?,
    val version: Int?,
    val createdAt: Long = System.currentTimeMillis()
)

// 扩展函数：VideoList -> FavoriteVideoEntity
fun VideoList.toFavoriteEntity(): FavoriteVideoEntity {
    return FavoriteVideoEntity(
        videoId = this.id ?: "",
        title = this.title,
        coverUrl = this.coverUrl,
        score = this.score,
        tags = this.tags,
        remarks = this.remarks,
        publishedAt = this.publishedAt,
        categoryId = this.categoryId,
        isTyped = this.isTyped,
        version = this.version
    )
}

// 扩展函数：FavoriteVideoEntity -> VideoList
fun FavoriteVideoEntity.toVideoList(): VideoList {
    return VideoList(
        id = this.videoId,
        title = this.title,
        coverUrl = this.coverUrl,
        score = this.score,
        tags = this.tags,
        remarks = this.remarks,
        publishedAt = this.publishedAt,
        categoryId = this.categoryId,
        isTyped = this.isTyped,
        version = this.version
    )
}