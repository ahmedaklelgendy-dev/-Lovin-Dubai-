package com.augustus.lovindubai.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.augustus.lovindubai.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Query("SELECT * FROM channels LIMIT 1")
    fun getChannel(): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels LIMIT 1")
    suspend fun getChannelOnce(): ChannelEntity?

    @Query("DELETE FROM channels")
    suspend fun clearChannels()
}