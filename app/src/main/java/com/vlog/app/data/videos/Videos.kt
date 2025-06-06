package com.vlog.app.data.videos

data class Videos(
    var id: String? = null,
    val version: Int? = null,
    var isTyped: Int? = null,
    var releasedAt: Long? = null,
    var isRecommend: Int? = null,
    var publishedAt: String? = null,
    var orderSort: Int? = null,
    var categoryId: String? = null,
    var title: String? = null,
    var score: String? = null,
    var alias: String? = null,
    var director: String? = null,
    var actors: String? = null,
    var region: String? = null,
    var language: String? = null,
    var description: String? = null,
    var tags: String? = null,
    var author: String? = null,
    var remarks: String? = null,
    var coverUrl: String? = null,

    val duration: String? = null,
    val episodeCount: Int? = null,

    var gatherList: MutableList<GatherList>? = mutableListOf(),
)