package com.vlog.app.data.comments

data class Comments(
    var id: String? = null, // Should ideally be non-null if it's a unique identifier from API
    var createdAt: Long? = null,
    var isTyped: Int? = null, // Consider if this is related to commentType or a different flag
    val quoteId: String, // Made non-null as per requirement
    var parentId: String? = null,
    var title: String? = null,
    var description: String? = null,

    var createdBy: String? = null,
    var nickName: String? = null,
    var avatar: String? = null,
)