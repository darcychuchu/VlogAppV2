package com.vlog.app.data.histories.watch

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 观看历史实体类
 */
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val coverUrl: String?,
    var remarks: String? = null,
    val lastPlayedPosition: Long = 0, // 上次播放位置（毫秒）
    val duration: Long = 0, // 视频总时长（毫秒）
    val lastWatchTime: Long = 0, // 最后观看时间
    val videoType: Int = 0, // 视频类型：0-未知，1-电影，2-电视剧，3-动漫
    val episodeTitle: String? = null, // 当前观看的剧集标题
    val episodeIndex: Int = 0, // 当前观看的剧集索引
    val totalEpisodes: Int = 0, // 总剧集数
    val gatherId: String? = null, // 服务商ID
    val gatherName: String? = null, // 服务商名称
    val playerUrl: String? = null, // 播放地址
)