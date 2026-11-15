
package com.augustus.lovindubai.data.remote.mediator

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.augustus.lovindubai.data.local.database.LovinDatabase
import com.augustus.lovindubai.data.local.dao.RemoteKeysDao
import com.augustus.lovindubai.data.local.dao.VideoDao
import com.augustus.lovindubai.data.local.entity.RemoteKeysEntity
import com.augustus.lovindubai.data.local.entity.VideoEntity
import com.augustus.lovindubai.data.remote.YouTubeApiService
import com.augustus.lovindubai.domain.model.Video
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalPagingApi::class)
class YouTubeRemoteMediator(
    private val playlistId: String,
    private val api: YouTubeApiService,
    private val database: LovinDatabase,
    private val videoDao: VideoDao,
    private val remoteKeysDao: RemoteKeysDao,
    private val fetchVideoDetails: suspend (List<String>) -> List<Video>
) : RemoteMediator<Int, VideoEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, VideoEntity>
    ): MediatorResult {

        val pageToken = when (loadType) {

            LoadType.REFRESH -> null

            LoadType.PREPEND -> {
                // YouTube uploads playlist is loaded newest first.
                // We only support loading older videos.
                return MediatorResult.Success(
                    endOfPaginationReached = true
                )
            }

            LoadType.APPEND -> {
                val lastItem = state.lastItemOrNull()
                    ?: return MediatorResult.Success(
                        endOfPaginationReached = true
                    )

                remoteKeysDao.remoteKeysByVideoId(lastItem.id)?.nextKey
                    ?: return MediatorResult.Success(
                        endOfPaginationReached = true
                    )
            }
        }

        return try {

            var requestToken = pageToken
            var videos: List<Video> = emptyList()
            var nextPageToken: String?

            // Skip pages containing only unavailable videos.
            do {

                val response = api.getPlaylistItems(
                    playlistId = playlistId,
                    pageToken = requestToken
                )

                val videoIds = response.items
                    .mapNotNull { it.contentDetails?.videoId }
                    .distinct()

                videos = fetchVideoDetails(videoIds)

                nextPageToken = response.nextPageToken

                if (videos.isEmpty() && nextPageToken != null) {
                    requestToken = nextPageToken
                }

            } while (videos.isEmpty() && nextPageToken != null)

            val endOfPaginationReached = nextPageToken == null

            database.withTransaction {

                if (loadType == LoadType.REFRESH) {
                    videoDao.clearVideos()
                    remoteKeysDao.clearRemoteKeys()
                }

                val videoEntities = videos.map { video ->
                    VideoEntity(
                        id = video.id,
                        title = video.title,
                        description = video.description,
                        thumbnailUrl = video.thumbnailUrl,
                        publishedAt = video.publishedAt,
                        duration = video.duration,
                        viewCount = video.viewCount,
                        type = video.type.name,
                        isLive = video.isLive
                    )
                }

                videoDao.insertVideos(videoEntities)

                val remoteKeys = videos.map { video ->
                    RemoteKeysEntity(
                        videoId = video.id,
                        prevKey = pageToken,
                        nextKey = nextPageToken
                    )
                }

                remoteKeysDao.insertAll(remoteKeys)
            }

            MediatorResult.Success(
                endOfPaginationReached = endOfPaginationReached
            )

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
