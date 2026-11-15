
package com.augustus.lovindubai.data.local.di

import android.content.Context
import androidx.room.Room
import com.augustus.lovindubai.data.local.database.LovinDatabase
import com.augustus.lovindubai.data.local.dao.ChannelDao
import com.augustus.lovindubai.data.local.dao.RemoteKeysDao
import com.augustus.lovindubai.data.local.dao.VideoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLovinDatabase(
        @ApplicationContext context: Context
    ): LovinDatabase {
        return Room.databaseBuilder(
            context,
            LovinDatabase::class.java,
            "lovin_dubai_database"
        )        .addMigrations(LovinDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideChannelDao(
        database: LovinDatabase
    ): ChannelDao {
        return database.channelDao()
    }

    @Provides
    @Singleton
    fun provideVideoDao(
        database: LovinDatabase
    ): VideoDao {
        return database.videoDao()
    }

    @Provides
    @Singleton
    fun provideRemoteKeysDao(
        database: LovinDatabase
    ): RemoteKeysDao {
        return database.remoteKeysDao()
    }
}
