package com.vlog.app.data.videos

data class VideoList(
    var id: String? = null,
    var isTyped: Int? = null,
    var publishedAt: String? = null,
    var categoryId: String? = null,
    var title: String? = null,
    var score: String? = null,
    var tags: String? = null,
    var remarks: String? = null,
    var coverUrl: String? = null
)