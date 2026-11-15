
package com.augustus.lovindubai.presentation.channel

import com.augustus.lovindubai.domain.model.Channel

data class ChannelUiState(
    val channel: Channel? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)
