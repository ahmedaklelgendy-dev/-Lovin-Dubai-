
package com.augustus.lovindubai.presentation.channel

import android.util.Log
import androidx.paging.PagingData
import app.cash.turbine.test
import com.augustus.lovindubai.domain.model.Channel
import com.augustus.lovindubai.domain.repository.YouTubeRepository
import com.augustus.lovindubai.domain.usecase.GetChannelUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
 @OptIn(ExperimentalCoroutinesApi::class)
class ChannelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getChannelUseCase: GetChannelUseCase
    private lateinit var repository: YouTubeRepository
    private lateinit var viewModel: ChannelViewModel

    private val testChannel = Channel(
        id = "channel_123",
        title = "Lovin Dubai",
        description = "Lovin Dubai Channel",
        thumbnailUrl = "https://example.com/channel.jpg",
        subscriberCount = 100000L,
        videoCount = 500L,
        viewCount = 10000000L,
        uploadsPlaylistId = "uploads_playlist_123"
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)

        every {
            Log.e(any<String>(), any<String>(), any<Throwable>())
        } returns 0

        every {
            Log.d(any<String>(), any<String>())
        } returns 0

        getChannelUseCase = mockk()
        repository = mockk()
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun createViewModel(): ChannelViewModel {
        return ChannelViewModel(
            getChannelUseCase = getChannelUseCase,
            repository = repository
        )
    }

    @Test
    fun loadChannel_success_updatesUiState() = runTest {
        coEvery { getChannelUseCase() } returns testChannel

        viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(testChannel, state.channel)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun loadChannel_failure_updatesErrorState() = runTest {
        val exception = RuntimeException("Network error")

        coEvery { getChannelUseCase() } throws exception

        viewModel = createViewModel()

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertNull(state.channel)
        assertFalse(state.isLoading)
        assertEquals("Network error", state.error)
    }


     @Test
     fun loadChannel_success_setsPlaylistIdAndRequestsPagedVideos() = runTest {

         coEvery {
             getChannelUseCase()
         } returns testChannel

         every {
             repository.getPagedVideos(testChannel.uploadsPlaylistId)
         } returns flowOf(PagingData.empty())

         viewModel = createViewModel()

         // Wait for initial channel loading
         advanceUntilIdle()

         // Verify channel was loaded
         assertEquals(
             testChannel,
             viewModel.uiState.value.channel
         )

         // Collect Paging Flow and verify repository call
         viewModel.videos.test {
             awaitItem()

             verify(exactly = 1) {
                 repository.getPagedVideos(testChannel.uploadsPlaylistId)
             }

             cancelAndIgnoreRemainingEvents()
         }
     }

    @Test
    fun refresh_success_updatesChannelAndResetsRefreshingState() = runTest {
        val updatedChannel = testChannel.copy(
            title = "Lovin Dubai Updated",
            subscriberCount = 200000L
        )

        coEvery { getChannelUseCase() } returnsMany listOf(
            testChannel,
            updatedChannel
        )

        viewModel = createViewModel()

        advanceUntilIdle()

        viewModel.refresh()

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(updatedChannel, state.channel)
        assertFalse(state.isRefreshing)
        assertNull(state.error)

        io.mockk.coVerify(exactly = 2) {
            getChannelUseCase()
        }
    }

    @Test
    fun refresh_failure_keepsPreviousChannelAndShowsError() = runTest {
        coEvery { getChannelUseCase() } returns testChannel

        viewModel = createViewModel()

        advanceUntilIdle()

        coEvery {
            getChannelUseCase()
        } throws RuntimeException("Refresh failed")

        viewModel.refresh()

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(testChannel, state.channel)
        assertEquals("Refresh failed", state.error)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun onVideosLoaded_doesNotThrowException() {
        viewModel = createViewModel()

        viewModel.onVideosLoaded(emptyList())

        verify {
            Log.d("VideoStats", "Loaded videos: 0")
        }
    }
}
