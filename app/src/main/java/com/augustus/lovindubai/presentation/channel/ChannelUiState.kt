
package com.augustus.lovindubai.presentation.channel

import com.augustus.lovindubai.domain.model.Channel
import com.augustus.lovindubai.domain.model.Video

data class ChannelUiState(
    val isLoading: Boolean = false,
    val channel: Channel? = null,
    val error: String? = null,

    val videos: List<Video> = emptyList(),
    val isVideosLoading: Boolean = false,
    val videosError: String? = null,

    // Pagination
    val nextPageToken: String? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreError: String? = null
)
