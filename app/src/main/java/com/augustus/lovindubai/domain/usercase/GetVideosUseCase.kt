
package com.augustus.lovindubai.domain.usecase

import com.augustus.lovindubai.domain.repository.YouTubeRepository
import javax.inject.Inject

class GetVideosUseCase @Inject constructor(
    private val repository: YouTubeRepository
) {
    suspend operator fun invoke(
        playlistId: String,
        pageToken: String? = null
    ) = repository.getVideos(
        playlistId = playlistId,
        pageToken = pageToken
    )
}
