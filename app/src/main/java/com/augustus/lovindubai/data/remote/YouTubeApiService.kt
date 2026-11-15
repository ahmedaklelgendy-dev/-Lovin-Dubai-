
package com.augustus.lovindubai.data.remote

import com.augustus.lovindubai.data.remote.model.ChannelResponse
import com.augustus.lovindubai.data.remote.model.PlaylistItemsResponse
import com.augustus.lovindubai.data.remote.model.VideosResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    // Get channel information and statistics
    @GET("channels")
    suspend fun getChannel(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("forHandle") handle: String = "@LovinDubai"
    ): ChannelResponse

    // Get videos from channel uploads playlist
    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null
    ): PlaylistItemsResponse

    // Get detailed video information
    @GET("videos")
    suspend fun getVideos(
        @Query("part") part: String = "snippet,contentDetails,statistics,liveStreamingDetails",
        @Query("id") videoIds: String
    ): VideosResponse
}
