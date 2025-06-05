package com.vlog.app.data.model

data class Comments(
    var id: String? = null,
    var createdAt: Long? = null,
    var isTyped: Int? = null,
    var quoteId: String? = null,
    var parentId: String? = null,
    var title: String? = null,
    var description: String? = null,

    var createdBy: String? = null,
    var nickName: String? = null,
    var avatar: String? = null,
)