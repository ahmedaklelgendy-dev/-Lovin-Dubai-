
package com.augustus.lovindubai.data.repository

import android.util.Log
import com.augustus.lovindubai.data.local.dao.ChannelDao
import com.augustus.lovindubai.data.local.dao.VideoDao
import com.augustus.lovindubai.data.local.entity.ChannelEntity
import com.augustus.lovindubai.data.local.entity.VideoEntity
import com.augustus.lovindubai.data.remote.YouTubeApiService
import com.augustus.lovindubai.domain.model.Channel
import com.augustus.lovindubai.domain.model.Video
import com.augustus.lovindubai.domain.model.VideoPage
import com.augustus.lovindubai.domain.model.VideoType
import com.augustus.lovindubai.domain.repository.YouTubeRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepositoryImpl @Inject constructor(
    private val api: YouTubeApiService,
    private val channelDao: ChannelDao,
    private val videoDao: VideoDao
) : YouTubeRepository {

    override suspend fun getChannel(): Channel {
        return try {
            val response = api.getChannel()

            val item = response.items.firstOrNull()
                ?: throw IllegalStateException("Lovin Dubai channel not found")

            val snippet = item.snippet
            val statistics = item.statistics
            val playlists = item.contentDetails?.relatedPlaylists

            val channel = Channel(
                id = item.id,
                title = snippet?.title.orEmpty(),
                description = snippet?.description.orEmpty(),
                thumbnailUrl = snippet?.thumbnails?.high?.url
                    ?: snippet?.thumbnails?.medium?.url.orEmpty(),
                subscriberCount = statistics?.subscriberCount?.toLongOrNull() ?: 0L,
                videoCount = statistics?.videoCount?.toLongOrNull() ?: 0L,
                viewCount = statistics?.viewCount?.toLongOrNull() ?: 0L,
                uploadsPlaylistId = playlists?.uploads.orEmpty()
            )

            channelDao.insertChannel(channel.toEntity())

            channel
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("YouTubeRepository", "Failed to fetch channel", e)

            val cachedChannel = channelDao.getChannelOnce()

            cachedChannel?.toDomain() ?: throw e
        }
    }

    override suspend fun getVideos(
        playlistId: String,
        pageToken: String?
    ): VideoPage {

        return try {
            val playlistResponse = api.getPlaylistItems(
                playlistId = playlistId,
                pageToken = pageToken
            )

            val videoIds = playlistResponse.items
                .mapNotNull { it.contentDetails?.videoId }
                .distinct()

            val videos = getVideoDetails(videoIds)

            Log.d("LovinVideos", "Playlist items: ${playlistResponse.items.size}")
            Log.d("LovinVideos", "Extracted video IDs: ${videoIds.size}")
            Log.d("LovinVideos", "Video details returned: ${videos.size}")

            Log.d(
                "LovinVideos",
                "Video types: ${videos.groupingBy { it.type }.eachCount()}"
            )

            if (videos.isNotEmpty()) {
                videoDao.insertVideos(videos.map { it.toEntity() })
            }

            VideoPage(
                videos = videos,
                nextPageToken = playlistResponse.nextPageToken
            )

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("YouTubeRepository", "Failed to fetch videos", e)

            // Use cached videos only when the first page fails.
            if (pageToken == null) {
                val cachedVideos = videoDao.getAllVideosOnce()
                    .map { it.toDomain() }

                if (cachedVideos.isNotEmpty()) {
                    return VideoPage(
                        videos = cachedVideos,
                        nextPageToken = null
                    )
                }
            }

            throw e
        }
    }

    override suspend fun getVideoDetails(
        videoIds: List<String>
    ): List<Video> {

        if (videoIds.isEmpty()) return emptyList()

        return videoIds.distinct().chunked(50).flatMap { chunk ->

            val response = api.getVideos(
                videoIds = chunk.joinToString(",")
            )

            response.items.map { item ->

                val snippet = item.snippet
                val liveDetails = item.liveStreamingDetails
                val broadcastStatus = snippet?.liveBroadcastContent

                val videoType = when {
                    broadcastStatus == "live" ->
                        VideoType.LIVE_ACTIVE

                    broadcastStatus == "upcoming" ->
                        VideoType.LIVE_UPCOMING

                    liveDetails?.actualEndTime != null ->
                        VideoType.LIVE_RECORDED

                    liveDetails?.actualStartTime != null ->
                        VideoType.LIVE_ACTIVE

                    isShortVideo(
                        duration = item.contentDetails?.duration,
                        title = snippet?.title.orEmpty(),
                        description = snippet?.description.orEmpty()
                    ) ->
                        VideoType.SHORT

                    else ->
                        VideoType.REGULAR
                }

                Video(
                    id = item.id,
                    title = snippet?.title.orEmpty(),
                    description = snippet?.description.orEmpty(),
                    thumbnailUrl = snippet?.thumbnails?.high?.url
                        ?: snippet?.thumbnails?.medium?.url.orEmpty(),
                    publishedAt = snippet?.publishedAt.orEmpty(),
                    duration = item.contentDetails?.duration,
                    viewCount = item.statistics?.viewCount?.toLongOrNull() ?: 0L,
                    type = videoType,
                    isLive = videoType == VideoType.LIVE_ACTIVE
                )
            }
        }
    }
}

// Channel mapping

private fun Channel.toEntity() = ChannelEntity(
    id = id,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    subscriberCount = subscriberCount,
    videoCount = videoCount,
    viewCount = viewCount,
    uploadsPlaylistId = uploadsPlaylistId
)

private fun ChannelEntity.toDomain() = Channel(
    id = id,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    subscriberCount = subscriberCount,
    videoCount = videoCount,
    viewCount = viewCount,
    uploadsPlaylistId = uploadsPlaylistId
)

// Video mapping

private fun Video.toEntity() = VideoEntity(
    id = id,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    publishedAt = publishedAt,
    duration = duration,
    viewCount = viewCount,
    type = type.name,
    isLive = isLive
)

private fun VideoEntity.toDomain() = Video(
    id = id,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    publishedAt = publishedAt,
    duration = duration,
    viewCount = viewCount,
    type = runCatching {
        VideoType.valueOf(type)
    }.getOrDefault(VideoType.REGULAR),
    isLive = isLive
)

// Shorts detection

private fun isShortVideo(
    duration: String?,
    title: String,
    description: String
): Boolean {

    val hasShortsTag = (title + " " + description).contains(
        Regex("""#shorts\b""", RegexOption.IGNORE_CASE)
    )

    val durationInSeconds = duration
        ?.let {
            Regex(
                """PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?"""
            ).matchEntire(it)
        }
        ?.let { match ->
            val hours = match.groupValues[1].toLongOrNull() ?: 0L
            val minutes = match.groupValues[2].toLongOrNull() ?: 0L
            val seconds = match.groupValues[3].toLongOrNull() ?: 0L

            hours * 3600 + minutes * 60 + seconds
        }

    return hasShortsTag || (
            durationInSeconds != null &&
                    durationInSeconds <= 60 &&
                    durationInSeconds > 0
            )
}
