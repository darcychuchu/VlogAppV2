package com.vlog.app.data.videos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GatherItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GatherItemEntity>)

    @Query("SELECT * FROM gather_items WHERE videoId = :videoId")
    fun getGatherItemsForVideo(videoId: String): Flow<List<GatherItemEntity>>

    @Query("DELETE FROM gather_items WHERE videoId = :videoId")
    suspend fun deleteGatherItemsForVideo(videoId: String)
}
