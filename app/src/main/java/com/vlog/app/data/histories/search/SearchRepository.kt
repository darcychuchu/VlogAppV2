package com.vlog.app.data.histories.search

import com.vlog.app.data.videos.VideoList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * 搜索仓库
 */
class SearchRepository(
    private val searchService: SearchService,
    private val searchHistoryDao: SearchHistoryDao
) {
    /**
     * 搜索视频
     * @param key 搜索关键词，为空时返回热门搜索
     * @return 搜索结果列表
     */
    suspend fun searchVideos(key: String? = null): List<VideoList>? {
        return withContext(Dispatchers.IO) {
            searchService.searchVideos(key).data
        }
    }

    /**
     * 获取最近搜索历史
     * @return 最近搜索历史列表
     */
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>> {
        return searchHistoryDao.getRecentSearches()
    }

    /**
     * 保存搜索历史
     * @param query 搜索关键词
     */
    suspend fun saveSearchQuery(query: String) {
        withContext(Dispatchers.IO) {
            val searchHistory = SearchHistoryEntity(
                query = query,
                timestamp = System.currentTimeMillis()
            )
            searchHistoryDao.insertSearchQuery(searchHistory)
        }
    }

    /**
     * 删除搜索历史
     * @param searchHistory 搜索历史
     */
    suspend fun deleteSearchQuery(searchHistory: SearchHistoryEntity) {
        withContext(Dispatchers.IO) {
            searchHistoryDao.deleteSearchQuery(searchHistory)
        }
    }

    /**
     * 清空所有搜索历史
     */
    suspend fun clearAllSearchHistory() {
        withContext(Dispatchers.IO) {
            searchHistoryDao.clearAllSearchHistory()
        }
    }
}