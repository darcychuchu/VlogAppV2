package com.vlog.app.data.favorites

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.vlog.app.data.videos.VideoEntity

@Entity(tableName = "favorites")
data class FavoritesEntity(
    @PrimaryKey
    var id: String,
    var createdAt: Long? = null,
    var orderSort: Int? = null,
    var version: Int? = null,
    var createdBy: String? = null,
    var quoteId: String? = null,
    var quoteType: Int? = null
)

/**
 * 订阅与视频的关系数据类
 * 用于查询时获取完整的订阅信息（包含关联的视频信息）
 * 只有当 quoteType == 11 (VIDEO) 时才会关联 VideoEntity
 */
data class FavoritesWithVideo(
    @Embedded val favorites: FavoritesEntity,
    @Relation(
        parentColumn = "quoteId",
        entityColumn = "id"
    )
    val video: VideoEntity?
)