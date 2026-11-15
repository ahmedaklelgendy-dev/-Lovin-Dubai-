
package com.augustus.lovindubai.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChannelResponse(
    val items: List<ChannelItem> = emptyList()
)

data class ChannelItem(
    val id: String = "",
    val snippet: ChannelSnippet? = null,
    val contentDetails: ChannelContentDetails? = null,
    val statistics: ChannelStatistics? = null
)

data class ChannelSnippet(
    val title: String? = null,
    val description: String? = null,
    val customUrl: String? = null,
    val publishedAt: String? = null,
    val thumbnails: Thumbnails? = null
)

data class ChannelContentDetails(
    val relatedPlaylists: RelatedPlaylists? = null
)

data class RelatedPlaylists(
    val uploads: String? = null
)

data class ChannelStatistics(
    val viewCount: String? = null,
    val subscriberCount: String? = null,
    val videoCount: String? = null
)

data class Thumbnails(
    val default: Thumbnail? = null,
    val medium: Thumbnail? = null,
    val high: Thumbnail? = null
)

data class Thumbnail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

// Playlist Items

data class PlaylistItemsResponse(
    val nextPageToken: String? = null,
    val items: List<PlaylistItem> = emptyList()
)

data class PlaylistItem(
    val snippet: PlaylistItemSnippet? = null,
    val contentDetails: PlaylistItemContentDetails? = null
)

data class PlaylistItemSnippet(
    val title: String? = null,
    val description: String? = null,
    val publishedAt: String? = null,
    val thumbnails: Thumbnails? = null
)

data class PlaylistItemContentDetails(
    val videoId: String? = null,
    val videoPublishedAt: String? = null
)

// Videos

data class VideosResponse(
    val items: List<VideoItem> = emptyList()
)

data class VideoItem(
    val id: String = "",
    val snippet: VideoSnippet? = null,
    val contentDetails: VideoContentDetails? = null,
    val statistics: VideoStatistics? = null,
    val liveStreamingDetails: LiveStreamingDetails? = null
)

data class VideoSnippet(
    val title: String? = null,
    val description: String? = null,
    val publishedAt: String? = null,
    val thumbnails: Thumbnails? = null,
    val liveBroadcastContent: String? = null
)

data class VideoContentDetails(
    val duration: String? = null,
    val dimension: String? = null,
    val definition: String? = null
)

data class VideoStatistics(
    val viewCount: String? = null,
    val likeCount: String? = null,
    val commentCount: String? = null
)

data class LiveStreamingDetails(
    val scheduledStartTime: String? = null,
    val actualStartTime: String? = null,
    val actualEndTime: String? = null
)
