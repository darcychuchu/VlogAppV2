package com.vlog.app.data.videos

data class GatherItem(
    val videoId: String,
    val version: Int = 0,
    var gatherListJson: String? = null,
    val lastUpdated: Long = 0,
    var gatherList: List<GatherList>? = null
)
