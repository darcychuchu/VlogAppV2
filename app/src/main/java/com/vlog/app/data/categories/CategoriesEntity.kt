package com.vlog.app.data.categories

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体类，用于Room数据库存储
 */
@Entity(tableName = "categories")
data class CategoriesEntity(
    @PrimaryKey
    var id: String,
    var orderSort: Int = 0,
    val version: Int = 0,
    val parentId: String? = null,
    var modelId: String? = null,
    var modelTyped: Int? = null,
    var title: String,
    val lastUpdated: Long = System.currentTimeMillis()
)