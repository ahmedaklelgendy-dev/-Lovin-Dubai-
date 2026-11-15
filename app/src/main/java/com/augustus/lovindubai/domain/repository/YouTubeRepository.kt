
package com.augustus.lovindubai.domain.repository

import com.augustus.lovindubai.domain.model.Channel
import com.augustus.lovindubai.domain.model.Video
import com.augustus.lovindubai.domain.model.VideoPage

interface YouTubeRepository {

    suspend fun getChannel(): Channel

    suspend fun getVideos(
        playlistId: String,
        pageToken: String? = null
    ): VideoPage

    suspend fun getVideoDetails(
        videoIds: List<String>
    ): List<Video>
}
