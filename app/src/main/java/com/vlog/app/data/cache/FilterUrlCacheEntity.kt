package com.vlog.app.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filter_url_cache")
data class FilterUrlCacheEntity(
    @PrimaryKey
    val url: String,
    val timestamp: Long,
    val responseDataJson: String
)
