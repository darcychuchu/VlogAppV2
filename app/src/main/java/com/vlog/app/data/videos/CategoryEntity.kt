package com.vlog.app.data.videos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val orderSort: Int?,
    val version: Int?,
    val modelId: String?,
    val modelTyped: Int?,
    val title: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

// 扩展函数：Categories -> CategoryEntity
fun Categories.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = this.id ?: "",
        orderSort = this.orderSort,
        version = this.version,
        modelId = this.modelId,
        modelTyped = this.modelTyped,
        title = this.title
    )
}

// 扩展函数：CategoryEntity -> Categories
fun CategoryEntity.toCategories(): Categories {
    return Categories(
        id = this.id,
        orderSort = this.orderSort,
        version = this.version,
        modelId = this.modelId,
        modelTyped = this.modelTyped,
        title = this.title
    )
}