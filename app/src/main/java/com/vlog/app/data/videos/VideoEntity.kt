package com.vlog.app.data.videos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val version: Int,
    val isTyped: Int?,
    val publishedAt: String?,
    val categoryId: String?,
    val title: String?,
    val score: String?,
    val tags: String?,
    val remarks: String?,
    val coverUrl: String?,
    val createdAt: Long = System.currentTimeMillis()
)

// 扩展函数：VideoList -> VideoEntity
fun VideoList.toEntity(): VideoEntity {
    return VideoEntity(
        id = this.id ?: "",
        version = this.version ?: 0,
        isTyped = this.isTyped,
        publishedAt = this.publishedAt,
        categoryId = this.categoryId,
        title = this.title,
        score = this.score,
        tags = this.tags,
        remarks = this.remarks,
        coverUrl = this.coverUrl
    )
}

// 扩展函数：VideoEntity -> VideoList
fun VideoEntity.toVideoList(): VideoList {
    return VideoList(
        id = this.id,
        version = this.version,
        isTyped = this.isTyped,
        publishedAt = this.publishedAt,
        categoryId = this.categoryId,
        title = this.title,
        score = this.score,
        tags = this.tags,
        remarks = this.remarks,
        coverUrl = this.coverUrl
    )
}