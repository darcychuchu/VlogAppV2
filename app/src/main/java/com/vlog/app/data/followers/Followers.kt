package com.vlog.app.data.followers

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vlog.app.data.users.Users

/**
 * 关注关系数据模型
 */
@JsonClass(generateAdapter = true)
data class Followers(
    val id: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val isLocked: Int? = null,
    val isEnabled: Int? = null,
    val isTyped: Int? = null,
    val orderSort: Int? = null,
    val version: Int? = null,
    val createdBy: String? = null,
    val userId: String? = null,
    val alternateId: String? = null,

    // 用户信息（可能从关联查询中获取）
    val userItem: Users? = null
)
