package com.vlog.app.data.users

data class Users(
    var id: String? = null,
    var isLocked: Int? = null,
    var createdAt: Long? = null,
    var name: String? = null, // 主要字段，跟全局API请求，唯一，不能重复
    var nickName: String? = null, // 主要字段，唯一，不能重复
    var description: String? = null,
    var avatar: String? = null,  // 存储attachmentId 通过 IMAGE_BASE_URL 展示头像
    var accessToken: String? = null,
    var points: Int? = 0 // 用户积分
)