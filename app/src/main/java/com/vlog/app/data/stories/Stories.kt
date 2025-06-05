package com.vlog.app.data.stories

data class Stories(
    var id: String? = null,
    var createdAt: Long? = null,
    var isTyped: Int? = null,
    var isValued: Int? = null,
    var isCommented: Int? = null,
    var isRecommend: Int? = null,
    var version: Int? = null,
    var title: String? = null,
    var description: String? = null,
    var tags: String? = null,

    var createdBy: String? = null,
    var nickName: String? = null,
    var avatar: String? = null,

    var coverUrl: String? = null,
    var imageList: List<String>? = null
)
