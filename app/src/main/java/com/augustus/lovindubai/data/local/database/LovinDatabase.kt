package com.augustus.lovindubai.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.augustus.lovindubai.data.local.dao.ChannelDao
import com.augustus.lovindubai.data.local.dao.RemoteKeysDao
import com.augustus.lovindubai.data.local.dao.VideoDao
import com.augustus.lovindubai.data.local.entity.ChannelEntity
import com.augustus.lovindubai.data.local.entity.RemoteKeysEntity
import com.augustus.lovindubai.data.local.entity.VideoEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(
    entities = [
        ChannelEntity::class,
        VideoEntity::class,
        RemoteKeysEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class LovinDatabase : RoomDatabase() {

    abstract fun channelDao(): ChannelDao

    abstract fun videoDao(): VideoDao

    abstract fun remoteKeysDao(): RemoteKeysDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `remote_keys` (
                        `videoId` TEXT NOT NULL,
                        `prevKey` TEXT,
                        `nextKey` TEXT,
                        PRIMARY KEY(`videoId`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
