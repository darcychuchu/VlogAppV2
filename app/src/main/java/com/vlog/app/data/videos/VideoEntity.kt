package com.vlog.app.data.videos

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.Long
import kotlin.String

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey
    val id: String,
    val version: Int,
    val isTyped: Int,
    var releasedAt: Long,
    var isRecommend: Int,
    var publishedAt: String? = null,
    var orderSort: Int,
    val categoryId: String? = null,
    val title: String? = null,

    var score: String? = null,
    var alias: String? = null,
    var director: String? = null,
    var actors: String? = null,
    var region: String? = null,
    var language: String? = null,
    var description: String? = null,
    var tags: String? = null,
    var author: String? = null,
    var remarks: String? = null,
    var coverUrl: String? = null,

    val duration: String? = null,
    val episodeCount: Int? = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// 扩展函数：Videos -> VideoEntity
fun Videos.toEntity(): VideoEntity {
    return VideoEntity(
        id = this.id ?: "",
        version = this.version ?: 0,
        isTyped = this.isTyped ?: 0,
        publishedAt = this.publishedAt,
        releasedAt = this.releasedAt ?: 0,
        isRecommend = this.isRecommend ?: 0,
        orderSort = this.orderSort ?: 0,
        categoryId = this.categoryId,
        title = this.title,
        score = this.score,
        tags = this.tags,
        remarks = this.remarks,
        coverUrl = this.coverUrl,

        alias = this.alias,
        director = this.director,
        actors = this.actors,
        region = this.region,
        language = this.language,
        description = this.description,
        author = this.author
    )
}

// 扩展函数：VideoEntity -> Videos
fun VideoEntity.toVideos(): Videos {
    return Videos(
        id = this.id,
        version = this.version,
        isTyped = this.isTyped,
        publishedAt = this.publishedAt,
        releasedAt = this.releasedAt,
        isRecommend = this.isRecommend,
        orderSort = this.orderSort,
        categoryId = this.categoryId,
        title = this.title,
        score = this.score,
        tags = this.tags,
        remarks = this.remarks,
        coverUrl = this.coverUrl,

        alias = this.alias,
        director = this.director,
        actors = this.actors,
        region = this.region,
        language = this.language,
        description = this.description,
        author = this.author
    )
}