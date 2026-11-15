
package com.augustus.lovindubai.presentation.channel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augustus.lovindubai.domain.model.VideoType
import com.augustus.lovindubai.domain.usecase.GetChannelUseCase
import com.augustus.lovindubai.domain.usecase.GetVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getChannelUseCase: GetChannelUseCase,
    private val getVideosUseCase: GetVideosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState())

    val uiState: StateFlow<ChannelUiState> =
        _uiState.asStateFlow()

    init {
        loadChannel()
    }

    fun loadChannel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                val channel = getChannelUseCase()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    channel = channel
                )

                loadVideos(channel.uploadsPlaylistId)
                 } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong"
                )
            }
        }
    }

    private suspend fun loadVideos(playlistId: String) {
        _uiState.value = _uiState.value.copy(
            isVideosLoading = true,
            videosError = null,
            nextPageToken = null,
            loadMoreError = null
        )

        try {
            val result = getVideosUseCase(
                playlistId = playlistId,
                pageToken = null
            )
            Log.d("VideoStats", "Total fetched: ${result.videos.size}")

            Log.d(
                "VideoStats",
                "Shorts: ${result.videos.count { it.type == VideoType.SHORT }}"
            )

            Log.d(
                "VideoStats",
                "Regular: ${result.videos.count { it.type == VideoType.REGULAR }}"
            )

            Log.d(
                "VideoStats",
                "Live: ${result.videos.count {
                    it.type == VideoType.LIVE_ACTIVE ||
                            it.type == VideoType.LIVE_UPCOMING ||
                            it.type == VideoType.LIVE_RECORDED
                }}"
            )

            Log.d("VideoStats", "Next page token: ${result.nextPageToken}")
            _uiState.value = _uiState.value.copy(
                videos = result.videos,
                nextPageToken = result.nextPageToken,
                isVideosLoading = false,
                videosError = null
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isVideosLoading = false,
                videosError = e.message ?: "Failed to load videos"
            )
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        val playlistId = currentState.channel?.uploadsPlaylistId
        val pageToken = currentState.nextPageToken

        // Prevent duplicate requests
        if (
            currentState.isVideosLoading ||
            currentState.isLoadingMore ||
            pageToken == null ||
            playlistId == null
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingMore = true,
                loadMoreError = null
            )

            try {
                val result = getVideosUseCase(
                    playlistId = playlistId,
                    pageToken = pageToken
                )

                val latestVideos = _uiState.value.videos
                val existingIds = latestVideos.map { it.id }.toSet()

                val newVideos = result.videos.filter { it.id !in existingIds }

                _uiState.value = _uiState.value.copy(
                    videos = latestVideos + newVideos,
                    nextPageToken = result.nextPageToken,
                    isLoadingMore = false,
                    loadMoreError = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    loadMoreError = e.message ?: "Failed to load more videos"
                )
            }
        }
    }

    val shorts: List<com.augustus.lovindubai.domain.model.Video>
        get() = _uiState.value.videos.filter {
            it.type == VideoType.SHORT
        }

    val regularVideos: List<com.augustus.lovindubai.domain.model.Video>
        get() = _uiState.value.videos.filter {
            it.type == VideoType.REGULAR
        }

    val liveVideos: List<com.augustus.lovindubai.domain.model.Video>
        get() = _uiState.value.videos.filter {
            it.type == VideoType.LIVE_ACTIVE ||
                    it.type == VideoType.LIVE_UPCOMING ||
                    it.type == VideoType.LIVE_RECORDED
        }
}
