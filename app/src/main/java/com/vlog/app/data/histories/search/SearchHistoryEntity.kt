package com.vlog.app.data.histories.search

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 搜索历史实体类
 */
@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    val query: String,
    val timestamp: Long
)
