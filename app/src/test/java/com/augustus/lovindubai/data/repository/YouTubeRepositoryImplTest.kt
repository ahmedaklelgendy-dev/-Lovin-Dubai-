
package com.augustus.lovindubai.data.repository

import android.util.Log
import com.augustus.lovindubai.data.local.dao.ChannelDao
import com.augustus.lovindubai.data.local.dao.RemoteKeysDao
import com.augustus.lovindubai.data.local.dao.VideoDao
import com.augustus.lovindubai.data.local.database.LovinDatabase
import com.augustus.lovindubai.data.local.entity.ChannelEntity
import com.augustus.lovindubai.data.local.entity.VideoEntity
import com.augustus.lovindubai.data.remote.YouTubeApiService
import com.augustus.lovindubai.data.remote.model.*
import com.augustus.lovindubai.domain.model.VideoType
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class YouTubeRepositoryImplTest {

    private lateinit var api: YouTubeApiService
    private lateinit var channelDao: ChannelDao
    private lateinit var videoDao: VideoDao
    private lateinit var remoteKeysDao: RemoteKeysDao
    private lateinit var database: LovinDatabase

    private lateinit var repository: YouTubeRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        channelDao = mockk()
        videoDao = mockk()
        remoteKeysDao = mockk()
        database = mockk()

        repository = YouTubeRepositoryImpl(
            api = api,
            channelDao = channelDao,
            videoDao = videoDao,
            remoteKeysDao = remoteKeysDao,
            database = database
        )

        // Android Log is not available normally in local JVM tests.
        mockkStatic(Log::class)

        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    // ---------------------------------------------------------
    // GET CHANNEL
    // ---------------------------------------------------------

    @Test
    fun getChannel_success_returnsChannelAndCachesIt() = runTest {

        coEvery { api.getChannel() } returns ChannelResponse(
            items = listOf(
                ChannelItem(
                    id = "channel_123",
                    snippet = ChannelSnippet(
                        title = "Lovin Dubai",
                        description = "Dubai News",
                        thumbnails = Thumbnails(
                            high = Thumbnail(url = "high.jpg"),
                            medium = Thumbnail(url = "medium.jpg")
                        )
                    ),
                    statistics = ChannelStatistics(
                        subscriberCount = "1000000",
                        videoCount = "5000",
                        viewCount = "200000000"
                    ),
                    contentDetails = ChannelContentDetails(
                        relatedPlaylists = RelatedPlaylists(
                            uploads = "uploads_123"
                        )
                    )
                )
            )
        )

        coEvery { channelDao.insertChannel(any()) } just Runs

        val result = repository.getChannel()

        assertEquals("channel_123", result.id)
        assertEquals("Lovin Dubai", result.title)
        assertEquals("Dubai News", result.description)
        assertEquals("high.jpg", result.thumbnailUrl)
        assertEquals(1000000L, result.subscriberCount)
        assertEquals(5000L, result.videoCount)
        assertEquals(200000000L, result.viewCount)
        assertEquals("uploads_123", result.uploadsPlaylistId)

        coVerify(exactly = 1) {
            channelDao.insertChannel(
                match {
                    it.id == "channel_123" &&
                            it.title == "Lovin Dubai"
                }
            )
        }
    }

    @Test
    fun getChannel_apiFailure_returnsCachedChannel() = runTest {

        coEvery { api.getChannel() } throws RuntimeException("Network error")

        coEvery { channelDao.getChannelOnce() } returns ChannelEntity(
            id = "cached_123",
            title = "Cached Channel",
            description = "Cached description",
            thumbnailUrl = "cached.jpg",
            subscriberCount = 100L,
            videoCount = 50L,
            viewCount = 1000L,
            uploadsPlaylistId = "cached_playlist"
        )

        val result = repository.getChannel()

        assertEquals("cached_123", result.id)
        assertEquals("Cached Channel", result.title)
        assertEquals("cached_playlist", result.uploadsPlaylistId)
    }

    @Test
    fun getChannel_apiFailureWithoutCache_throwsException() = runTest {

        coEvery { api.getChannel() } throws RuntimeException("Network error")

        coEvery { channelDao.getChannelOnce() } returns null

        try {
            repository.getChannel()
            fail("Expected RuntimeException")
        } catch (e: RuntimeException) {
            assertEquals("Network error", e.message)
        }
    }

    // ---------------------------------------------------------
    // GET VIDEO DETAILS
    // ---------------------------------------------------------

    @Test
    fun getVideoDetails_emptyIds_returnsEmptyList() = runTest {

        val result = repository.getVideoDetails(emptyList())

        assertTrue(result.isEmpty())

        coVerify(exactly = 0) {
            api.getVideos(
                part = any(),
                videoIds = any()
            )
        }
    }

    @Test
    fun getVideoDetails_classifiesShortVideo() = runTest {

        coEvery {
            api.getVideos(videoIds = "short_1")
        } returns VideosResponse(
            items = listOf(
                VideoItem(
                    id = "short_1",
                    snippet = VideoSnippet(
                        title = "Amazing Short",
                        description = "Short video",
                        publishedAt = "2026-11-15",
                        thumbnails = Thumbnails(
                            high = Thumbnail(url = "short.jpg")
                        )
                    ),
                    contentDetails = VideoContentDetails(
                        duration = "PT45S"
                    )
                )
            )
        )

        val result = repository.getVideoDetails(listOf("short_1"))

        assertEquals(1, result.size)
        assertEquals(VideoType.SHORT, result.first().type)
        assertEquals("short_1", result.first().id)
    }

    @Test
    fun getVideoDetails_classifiesUpcomingLive() = runTest {

        coEvery {
            api.getVideos(videoIds = "live_1")
        } returns VideosResponse(
            items = listOf(
                VideoItem(
                    id = "live_1",
                    snippet = VideoSnippet(
                        title = "Upcoming Live",
                        liveBroadcastContent = "upcoming"
                    )
                )
            )
        )

        val result = repository.getVideoDetails(listOf("live_1"))

        assertEquals(VideoType.LIVE_UPCOMING, result.first().type)
        assertFalse(result.first().isLive)
    }

    @Test
    fun getVideoDetails_classifiesActiveLive() = runTest {

        coEvery {
            api.getVideos(videoIds = "live_2")
        } returns VideosResponse(
            items = listOf(
                VideoItem(
                    id = "live_2",
                    snippet = VideoSnippet(
                        title = "Live Now",
                        liveBroadcastContent = "live"
                    )
                )
            )
        )

        val result = repository.getVideoDetails(listOf("live_2"))

        assertEquals(VideoType.LIVE_ACTIVE, result.first().type)
        assertTrue(result.first().isLive)
    }

    @Test
    fun getVideoDetails_classifiesRecordedLive() = runTest {

        coEvery {
            api.getVideos(videoIds = "live_3")
        } returns VideosResponse(
            items = listOf(
                VideoItem(
                    id = "live_3",
                    snippet = VideoSnippet(
                        title = "Recorded Live"
                    ),
                    liveStreamingDetails = LiveStreamingDetails(
                        actualStartTime = "2026-11-15T10:00:00Z",
                        actualEndTime = "2026-11-15T11:00:00Z"
                    )
                )
            )
        )

        val result = repository.getVideoDetails(listOf("live_3"))

        assertEquals(VideoType.LIVE_RECORDED, result.first().type)
        assertFalse(result.first().isLive)
    }

    @Test
    fun getVideoDetails_classifiesRegularVideo() = runTest {

        coEvery {
            api.getVideos(videoIds = "video_1")
        } returns VideosResponse(
            items = listOf(
                VideoItem(
                    id = "video_1",
                    snippet = VideoSnippet(
                        title = "Regular Video",
                        description = "Normal video"
                    ),
                    contentDetails = VideoContentDetails(
                        duration = "PT10M30S"
                    )
                )
            )
        )

        val result = repository.getVideoDetails(listOf("video_1"))

        assertEquals(VideoType.REGULAR, result.first().type)
    }

    @Test
    fun getVideoDetails_removesDuplicateIds() = runTest {

        coEvery {
            api.getVideos(videoIds = "video_1,video_2")
        } returns VideosResponse(
            items = listOf(
                VideoItem(id = "video_1"),
                VideoItem(id = "video_2")
            )
        )

        val result = repository.getVideoDetails(
            listOf("video_1", "video_1", "video_2")
        )

        assertEquals(2, result.size)

        coVerify(exactly = 1) {
            api.getVideos(videoIds = "video_1,video_2")
        }
    }


    @Test
    fun getVideoDetails_moreThan50Ids_splitsRequestsIntoChunks() = runTest {

        val ids = (1..51).map { "video_$it" }
        val requestedIds = mutableListOf<String>()

        coEvery {
            api.getVideos(videoIds = any())
        } coAnswers {

            // First argument is part, second argument is videoIds.
            val requestIds = secondArg<String>()

            requestedIds.add(requestIds)

            VideosResponse(
                items = requestIds.split(",").map { id ->
                    VideoItem(id = id)
                }
            )
        }

        val result = repository.getVideoDetails(ids)

        assertEquals(51, result.size)
        assertEquals(2, requestedIds.size)

        assertEquals(50, requestedIds[0].split(",").size)
        assertEquals(1, requestedIds[1].split(",").size)
    }


    // ---------------------------------------------------------
    // GET VIDEOS
    // ---------------------------------------------------------

    @Test
    fun getVideos_success_returnsVideosAndCachesThem() = runTest {

        coEvery {
            api.getPlaylistItems(
                playlistId = "uploads_123",
                pageToken = null
            )
        } returns PlaylistItemsResponse(
            nextPageToken = "next_page",
            items = listOf(
                PlaylistItem(
                    contentDetails = PlaylistItemContentDetails(
                        videoId = "video_1"
                    )
                )
            )
        )

        coEvery {
            api.getVideos(videoIds = "video_1")
        } returns VideosResponse(
            items = listOf(
                VideoItem(
                    id = "video_1",
                    snippet = VideoSnippet(
                        title = "Test Video",
                        description = "Test Description",
                        publishedAt = "2026-11-15",
                        thumbnails = Thumbnails(
                            high = Thumbnail(url = "video.jpg")
                        )
                    ),
                    contentDetails = VideoContentDetails(
                        duration = "PT5M"
                    ),
                    statistics = VideoStatistics(
                        viewCount = "1000"
                    )
                )
            )
        )

        coEvery { videoDao.insertVideos(any()) } just Runs

        val result = repository.getVideos("uploads_123")

        assertEquals(1, result.videos.size)
        assertEquals("video_1", result.videos.first().id)
        assertEquals("Test Video", result.videos.first().title)
        assertEquals(1000L, result.videos.first().viewCount)
        assertEquals("next_page", result.nextPageToken)

        coVerify(exactly = 1) {
            videoDao.insertVideos(
                match {
                    it.size == 1 &&
                            it.first().id == "video_1"
                }
            )
        }
    }

    @Test
    fun getVideos_apiFailureOnFirstPage_returnsCachedVideos() = runTest {

        coEvery {
            api.getPlaylistItems(
                playlistId = "uploads_123",
                pageToken = null
            )
        } throws RuntimeException("Network error")

        coEvery { videoDao.getAllVideosOnce() } returns listOf(
            VideoEntity(
                id = "cached_video",
                title = "Cached Video",
                description = "Cached description",
                thumbnailUrl = "cached.jpg",
                publishedAt = "2026-11-15",
                duration = "PT5M",
                viewCount = 500L,
                type = "REGULAR",
                isLive = false
            )
        )

        val result = repository.getVideos("uploads_123")

        assertEquals(1, result.videos.size)
        assertEquals("cached_video", result.videos.first().id)
        assertEquals(VideoType.REGULAR, result.videos.first().type)
        assertNull(result.nextPageToken)
    }

    @Test
    fun getVideos_emptyPlaylist_returnsEmptyList() = runTest {

        coEvery {
            api.getPlaylistItems(
                playlistId = "uploads_123",
                pageToken = null
            )
        } returns PlaylistItemsResponse(
            items = emptyList()
        )

        val result = repository.getVideos("uploads_123")

        assertTrue(result.videos.isEmpty())
        assertNull(result.nextPageToken)

        coVerify(exactly = 0) {
            videoDao.insertVideos(any())
        }
    }
}
