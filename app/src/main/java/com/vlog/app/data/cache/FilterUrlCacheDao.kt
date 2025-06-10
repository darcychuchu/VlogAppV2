package com.vlog.app.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FilterUrlCacheDao {

    @Query("SELECT * FROM filter_url_cache WHERE url = :url")
    suspend fun getCache(url: String): FilterUrlCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: FilterUrlCacheEntity)

    /**
     * Deletes all cache entries whose timestamp is older than the provided minValidTimestamp.
     * minValidTimestamp = System.currentTimeMillis() - expiryDuration (e.g., 30 * 60 * 1000L)
     */
    @Query("DELETE FROM filter_url_cache WHERE timestamp < :minValidTimestamp")
    suspend fun deleteExpiredCache(minValidTimestamp: Long)

    // Optional: A method to clear the entire cache table if needed for development/testing.
    // @Query("DELETE FROM filter_url_cache")
    // suspend fun clearAllCache()
}
