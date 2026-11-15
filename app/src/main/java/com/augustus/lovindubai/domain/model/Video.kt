
package com.augustus.lovindubai.domain.model

enum class VideoType {
    SHORT,
    REGULAR,
    LIVE_UPCOMING,
    LIVE_ACTIVE,
    LIVE_RECORDED
}

data class Video(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val publishedAt: String,
    val duration: String?,
    val viewCount: Long,
    val type: VideoType,
    val isLive: Boolean
)

data class VideoPage(
    val videos: List<Video>,
    val nextPageToken: String?
)
