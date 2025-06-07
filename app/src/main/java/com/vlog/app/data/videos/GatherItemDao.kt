package com.vlog.app.data.videos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GatherItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GatherItemEntity)

    @Query("SELECT * FROM gather_items WHERE videoId = :videoId LIMIT 1")
    fun getGatherItemForVideo(videoId: String): Flow<GatherItemEntity?>

    @Query("DELETE FROM gather_items WHERE videoId = :videoId")
    suspend fun deleteGatherItemForVideo(videoId: String)

    @Query("SELECT * FROM gather_items WHERE videoId = :videoId LIMIT 1")
    fun getGatherItemSync(videoId: String): GatherItemEntity?
}
