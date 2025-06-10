package com.vlog.app.data.videos

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    
    @Query("SELECT * FROM videos ORDER BY createdAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>
    
    @Query("""
        SELECT * FROM videos 
        WHERE (:type IS NULL OR isTyped = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:year IS NULL OR releasedAt = :year)
        ORDER BY 
        CASE WHEN :sort = 'latest' THEN publishedAt END DESC,
        CASE WHEN :sort = 'hot' THEN CAST(orderSort AS REAL) END DESC,
        CASE WHEN :sort = 'rating' THEN CAST(score AS REAL) END DESC,
        CASE WHEN :sort = 'recommend' THEN CAST(isRecommend AS REAL) END DESC,
        createdAt DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getFilteredVideos(
        type: Int?,
        categoryId: String?,
        year: Int?,
        sort: Int?,
        limit: Int,
        offset: Int
    ): List<VideoEntity>
    
    @Query("""
        SELECT COUNT(*) FROM videos 
        WHERE (:type IS NULL OR isTyped = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:year IS NULL OR releasedAt = :year)
    """)
    suspend fun getFilteredVideosCount(
        type: Int?,
        categoryId: String?,
        year: Int?
    ): Int

    @Query("""
        SELECT MIN(lastRefreshed) FROM videos
        WHERE (:type IS NULL OR isTyped = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:year IS NULL OR releasedAt = :year)
    """)
    suspend fun getMinLastRefreshedTimestamp(
        type: Int?,
        categoryId: String?,
        year: Int?
    ): Long?
    
    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE id = :id")
    fun getVideoByIdFlow(id: String): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoByIdSuspend(id: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)
    
    @Update
    suspend fun updateVideo(video: VideoEntity)


    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: String)
    
    @Transaction
    suspend fun replaceAllVideos(videos: List<VideoEntity>) {
        updateVideosWithVersionCheck(videos)
    }
    
    // 根据version更新视频数据
    @Query("SELECT version FROM videos WHERE id = :id")
    suspend fun getVideoVersion(id: String): Int?

    @RawQuery
    suspend fun getSimilarVideosRaw(query: SupportSQLiteQuery): List<VideoEntity>

    suspend fun getSimilarVideos(categoryId: String, tags: List<String>, regions: List<String>, limit: Int, excludeId: String): List<VideoEntity> {
        val sb = StringBuilder()
        sb.append("SELECT * FROM videos WHERE categoryId = ? AND id != ? ")
        val args = mutableListOf<Any?>()
        args.add(categoryId)
        args.add(excludeId)

        val processedTags = tags.map { it.trim() }.filter { it.isNotBlank() }
        val processedRegions = regions.map { it.trim() }.filter { it.isNotBlank() }

        if (processedTags.isNotEmpty()) {
            sb.append("AND (")
            processedTags.forEachIndexed { index, tag ->
                if (index > 0) sb.append(" OR ")
                // Robust matching for comma-separated values:
                // Using (',' || column || ',' LIKE '%,value,%' OR column = 'value')
                // Must also escape the 'value' itself for LIKE special characters.
                val escapedTag = tag.replace("%", "\\%").replace("_", "\\_") // Use standard SQL escape for % and _
                sb.append(" (',' || tags || ',' LIKE ? ESCAPE '\\'")
                args.add("%,${escapedTag},%")
                sb.append(" OR tags = ?")
                args.add(tag) // Exact match for single tag
                sb.append(") ") // Close parenthesis for this tag's conditions
            }
            sb.append(") ") // Close parenthesis for the OR group of tags
        }

        if (processedRegions.isNotEmpty()) {
            sb.append("AND (")
            processedRegions.forEachIndexed { index, region ->
                if (index > 0) sb.append(" OR ")
                val escapedRegion = region.replace("%", "\\%").replace("_", "\\_")
                sb.append(" (',' || region || ',' LIKE ? ESCAPE '\\'")
                args.add("%,${escapedRegion},%")
                sb.append(" OR region = ?")
                args.add(region)
                sb.append(") ")
            }
            sb.append(") ")
        }

        sb.append("ORDER BY RANDOM() LIMIT ?")
        args.add(limit)

        val query = SimpleSQLiteQuery(sb.toString(), args.toTypedArray())
        return getSimilarVideosRaw(query)
    }

    //*****************************************************************************
    @Transaction
    suspend fun updateVideosWithVersionCheck(videosFromListDto: List<VideoEntity>) {
        videosFromListDto.forEach { videoFromListDto ->
            val existingEntity = getVideoByIdSuspend(videoFromListDto.id)
            if (existingEntity == null) {
                insertVideo(videoFromListDto)
            } else if (videoFromListDto.version > existingEntity.version) {
                val updatedEntity = existingEntity.copy(
                    // Fields typically authoritative in a ListDto
                    version = videoFromListDto.version,
                    title = videoFromListDto.title,
                    //coverUrl = videoFromListDto.coverUrl,
                    //isTyped = videoFromListDto.isTyped,
                    remarks = videoFromListDto.remarks,
                    //categoryId = videoFromListDto.categoryId,
                    orderSort = videoFromListDto.orderSort,
                    publishedAt = videoFromListDto.publishedAt,
                    isRecommend = videoFromListDto.isRecommend,
                    releasedAt = videoFromListDto.releasedAt,
                    score = videoFromListDto.score,
                    tags = videoFromListDto.tags,
                    //duration = videoFromListDto.duration,
                    //episodeCount = videoFromListDto.episodeCount

                    // Fields like alias, director, actors, region, language, description, author
                    // are NOT copied from videoFromListDto here, as videoFromListDto is assumed
                    // to be from a list response and may not have authoritative values for these details.
                    // lastRefreshed and gatherListVersion are also intentionally not copied,
                    // preserving their values from existingEntity.
                    // id and createdAt also retain their values from existingEntity by default with copy.
                )
                updateVideo(updatedEntity)
            }
        }
    }
}