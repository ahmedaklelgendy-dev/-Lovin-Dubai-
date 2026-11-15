
package com.augustus.lovindubai.di

import com.augustus.lovindubai.data.repository.YouTubeRepositoryImpl
import com.augustus.lovindubai.domain.repository.YouTubeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindYouTubeRepository(
        implementation: YouTubeRepositoryImpl
    ): YouTubeRepository
}
