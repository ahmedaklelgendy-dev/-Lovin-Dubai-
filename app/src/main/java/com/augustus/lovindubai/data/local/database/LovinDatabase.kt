package com.augustus.lovindubai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.augustus.lovindubai.data.local.dao.ChannelDao
import com.augustus.lovindubai.data.local.dao.VideoDao
import com.augustus.lovindubai.data.local.entity.ChannelEntity
import com.augustus.lovindubai.data.local.entity.VideoEntity

@Database(
    entities = [
        ChannelEntity::class,
        VideoEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class LovinDatabase : RoomDatabase() {

    abstract fun channelDao(): ChannelDao

    abstract fun videoDao(): VideoDao
}