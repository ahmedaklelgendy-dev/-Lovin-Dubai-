
package com.augustus.lovindubai.domain.usecase

import com.augustus.lovindubai.domain.repository.YouTubeRepository
import javax.inject.Inject

class GetChannelUseCase @Inject constructor(
    private val repository: YouTubeRepository
) {
    suspend operator fun invoke() =
        repository.getChannel()
}
