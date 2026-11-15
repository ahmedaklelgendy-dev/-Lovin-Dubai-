
package com.augustus.lovindubai.presentation.channel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.augustus.lovindubai.domain.model.Channel
import com.augustus.lovindubai.domain.model.Video
import com.augustus.lovindubai.domain.model.VideoType
import com.augustus.lovindubai.domain.usecase.GetChannelUseCase
import com.augustus.lovindubai.domain.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getChannelUseCase: GetChannelUseCase,
    private val repository: YouTubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    private val playlistId = MutableStateFlow<String?>(null)

    val videos: kotlinx.coroutines.flow.Flow<PagingData<Video>> =
        playlistId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) {
                    kotlinx.coroutines.flow.flowOf(PagingData.empty())
                } else {
                    repository.getPagedVideos(id)
                }
            }
            .cachedIn(viewModelScope)

    init {
        loadChannel()
    }

    fun loadChannel() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val channel = getChannelUseCase()

                _uiState.value = _uiState.value.copy(
                    channel = channel,
                    isLoading = false,
                    error = null
                )

                playlistId.value = channel.uploadsPlaylistId

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "Failed to load channel", e)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load channel"
                )
            }
        }
    }

    fun refresh() {
        val channel = _uiState.value.channel ?: return

        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                error = null
            )

            try {
                val updatedChannel = getChannelUseCase()

                _uiState.value = _uiState.value.copy(
                    channel = updatedChannel,
                    error = null
                )

                // Trigger a new Paging generation.
                playlistId.value = updatedChannel.uploadsPlaylistId

                if (playlistId.value == channel.uploadsPlaylistId) {
                    // Same playlist: UI can also call LazyPagingItems.refresh()
                    // to explicitly refresh the current Paging generation.
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ChannelViewModel", "Refresh failed", e)

                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to refresh channel"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false
                )
            }
        }
    }

    fun onVideosLoaded(videos: List<Video>) {
        Log.d("VideoStats", "Loaded videos: ${videos.size}")

        Log.d(
            "VideoStats",
            "Shorts: ${videos.count { it.type == VideoType.SHORT }}"
        )

        Log.d(
            "VideoStats",
            "Regular: ${videos.count { it.type == VideoType.REGULAR }}"
        )

        Log.d(
            "VideoStats",
            "Live: ${
                videos.count {
                    it.type == VideoType.LIVE_ACTIVE ||
                            it.type == VideoType.LIVE_UPCOMING ||
                            it.type == VideoType.LIVE_RECORDED
                }
            }"
        )
    }
}
