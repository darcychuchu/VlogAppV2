package com.vlog.app.data.categories

import com.squareup.moshi.Json

/**
 * 分类数据模型
 */
data class Categories(
    var id: String,
    var orderSort: Int = 0,
    val version: Int = 0,
    val parentId: String? = null,
    var modelId: String? = null,
    var modelTyped: Int = 0,
    var title: String,
    @Json(name = "categoryList")
    val categoryList: List<Categories> = emptyList()
)