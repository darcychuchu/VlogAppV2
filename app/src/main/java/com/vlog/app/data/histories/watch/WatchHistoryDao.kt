package com.vlog.app.data.histories.watch

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 观看历史数据访问对象
 */
@Dao
interface WatchHistoryDao {

    /**
     * 插入观看历史记录，如果已存在则替换
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertWatchHistory(watchHistory: WatchHistoryEntity)

    /**
     * 更新观看历史记录
     */
    @Update
    suspend fun updateWatchHistory(watchHistory: WatchHistoryEntity)

    /**
     * 获取所有观看历史记录，按最后观看时间降序排列
     */
    @Transaction
    @Query("SELECT * FROM watch_history ORDER BY lastWatchTime DESC")
    fun getAllWatchHistory(): Flow<List<WatchHistoryWithVideo>>

    /**
     * 获取指定视频的观看历史记录
     */
    @Query("SELECT * FROM watch_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getWatchHistoryById(videoId: String): WatchHistoryEntity?

    /**
     * 删除指定视频的观看历史记录
     */
    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun deleteWatchHistoryById(videoId: String)

    /**
     * 清空所有观看历史记录
     */
    @Query("DELETE FROM watch_history")
    suspend fun clearAllWatchHistory()

    /**
     * 获取最近观看的 N 条历史记录
     */
    @Query("SELECT * FROM watch_history ORDER BY lastWatchTime DESC LIMIT :limit")
    fun getRecentWatchHistory(limit: Int): Flow<List<WatchHistoryEntity>>

    /**
     * 按视频类型获取观看历史记录
     */
    @Query("SELECT * FROM watch_history WHERE videoType = :videoType ORDER BY lastWatchTime DESC")
    fun getWatchHistoryByType(videoType: Int): Flow<List<WatchHistoryEntity>>
}