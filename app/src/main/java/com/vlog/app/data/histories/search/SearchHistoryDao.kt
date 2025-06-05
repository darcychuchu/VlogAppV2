package com.vlog.app.data.histories.search

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertSearchQuery(searchHistory: SearchHistoryEntity)

    @Delete
    suspend fun deleteSearchQuery(searchHistory: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearAllSearchHistory()
}