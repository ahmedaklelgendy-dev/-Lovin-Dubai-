package com.augustus.lovindubai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.augustus.lovindubai.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Query("SELECT * FROM videos ORDER BY publishedAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY publishedAt DESC")
    suspend fun getAllVideosOnce(): List<VideoEntity>

    @Query(
        "SELECT * FROM videos " +
                "WHERE type = :type " +
                "ORDER BY publishedAt DESC"
    )
    fun getVideosByType(type: String): Flow<List<VideoEntity>>

    @Query("DELETE FROM videos")
    suspend fun clearVideos()
}