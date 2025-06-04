package com.vlog.app.data.histories.watch

import com.vlog.app.data.videos.VideoDetail
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 观看历史仓库
 */
@Singleton
class WatchHistoryRepository @Inject constructor(
    private val watchHistoryDao: WatchHistoryDao
) {

    /**
     * 获取所有观看历史
     */
    fun getAllWatchHistory(): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getAllWatchHistory()
    }

    /**
     * 获取最近观看的历史记录
     */
    fun getRecentWatchHistory(limit: Int = 10): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getRecentWatchHistory(limit)
    }

    /**
     * 按类型获取观看历史
     */
    fun getWatchHistoryByType(videoType: Int): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getWatchHistoryByType(videoType)
    }

    /**
     * 获取指定视频的观看历史
     */
    suspend fun getWatchHistoryById(videoId: String): WatchHistoryEntity? {
        return watchHistoryDao.getWatchHistoryById(videoId)
    }

    /**
     * 添加或更新观看历史
     */
    suspend fun addOrUpdateWatchHistory(
        videoId: String,
        title: String,
        coverUrl: String?,
        remarks: String?,
        playPosition: Long = 0,
        duration: Long = 0,
        videoType: Int = 0,
        episodeTitle: String? = null,
        episodeIndex: Int = 0,
        totalEpisodes: Int = 0,
        gatherId: String? = null,
        gatherName: String? = null,
        playerUrl: String? = null
    ) {
        val watchHistory = WatchHistoryEntity(
            videoId = videoId,
            title = title,
            coverUrl = coverUrl,
            remarks = remarks,
            lastPlayedPosition = playPosition,
            duration = duration,
            lastWatchTime = 0,
            videoType = videoType,
            episodeTitle = episodeTitle,
            episodeIndex = episodeIndex,
            totalEpisodes = totalEpisodes,
            gatherId = gatherId,
            gatherName = gatherName,
            playerUrl = playerUrl
        )
        watchHistoryDao.insertWatchHistory(watchHistory)
    }

    /**
     * 从 Video 添加观看历史
     * 同时支持列表项和详情
     */
    suspend fun addWatchHistoryFromVideo(
        video: VideoDetail,
        playPosition: Long = 0,
        duration: Long = 0,
        episodeTitle: String? = null,
        episodeIndex: Int = 0,
        gatherId: String? = null,
        gatherName: String? = null,
        playerUrl: String? = null
    ) {


        addOrUpdateWatchHistory(
            videoId = video.id!!,
            title = video.title!!,
            coverUrl = video.coverUrl,
            remarks = video.remarks,
            playPosition = playPosition,
            duration = duration,
            videoType = video.isTyped!!,
            episodeTitle = episodeTitle,
            episodeIndex = episodeIndex,
            totalEpisodes = video.episodeCount ?: 0,
            gatherId = gatherId,
            gatherName = gatherName,
            playerUrl = playerUrl
        )
    }

    /**
     * 从 Video 添加观看历史（兼容旧版本）
     */
    suspend fun addWatchHistoryFromVideoDetail(
        videoDetail: VideoDetail,
        playPosition: Long = 0,
        duration: Long = 0,
        episodeTitle: String? = null,
        episodeIndex: Int = 0,
        gatherId: String? = null,
        gatherName: String? = null,
        playerUrl: String? = null
    ) {
        addWatchHistoryFromVideo(
            video = videoDetail,
            playPosition = playPosition,
            duration = duration,
            episodeTitle = episodeTitle,
            episodeIndex = episodeIndex,
            gatherId = gatherId,
            gatherName = gatherName,
            playerUrl = playerUrl
        )
    }

    /**
     * 更新播放进度
     */
    suspend fun updatePlayProgress(
        videoId: String,
        episodeIndex: Int? = null,
        playPosition: Long? = null,
        duration: Long? = null,
        gatherId: String? = null,
        gatherName: String? = null,
        playerTitle: String? = null,
        playerUrl: String? = null,
        remarks: String? = null
    ) {
        val watchHistory = watchHistoryDao.getWatchHistoryById(videoId)
        watchHistory?.let {
            val updated = it.copy(
                episodeIndex = episodeIndex ?: it.episodeIndex,
                lastPlayedPosition = playPosition ?: it.lastPlayedPosition,
                duration = duration ?: it.duration,
                lastWatchTime = 0,
                gatherId = gatherId ?: it.gatherId,
                gatherName = gatherName ?: it.gatherName,
                episodeTitle = playerTitle ?: it.episodeTitle,
                playerUrl = playerUrl ?: it.playerUrl,
                remarks = remarks ?: it.remarks
            )
            watchHistoryDao.updateWatchHistory(updated)
        }
    }

    /**
     * 删除观看历史
     */
    suspend fun deleteWatchHistory(videoId: String) {
        watchHistoryDao.deleteWatchHistoryById(videoId)
    }

    /**
     * 清空所有观看历史
     */
    suspend fun clearAllWatchHistory() {
        watchHistoryDao.clearAllWatchHistory()
    }
}
