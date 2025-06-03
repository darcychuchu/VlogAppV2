package com.vlog.app.data.videos

data class GatherList(
    var gatherId: String,
    var gatherTitle: String,
    var gatherTips: String,
    var remarks: String? = null,
    val playList: List<PlayList>? = null
)