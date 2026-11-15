
package com.augustus.lovindubai.domain.repository

import androidx.paging.PagingData
import com.augustus.lovindubai.domain.model.Channel
import com.augustus.lovindubai.domain.model.Video
import com.augustus.lovindubai.domain.model.VideoPage
import kotlinx.coroutines.flow.Flow
 interface YouTubeRepository {

    suspend fun getChannel(): Channel

    suspend fun getVideos(
        playlistId: String,
        pageToken: String? = null
    ): VideoPage

    suspend fun getVideoDetails(
        videoIds: List<String>
    ): List<Video>

    // Paging 3
    fun getPagedVideos(
        playlistId: String
    ): Flow<PagingData<Video>>
}
