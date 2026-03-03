package com.photosync.domain.usecase

import com.photosync.domain.model.UploadQuality
import javax.inject.Inject

/**
 * Use case to determine the upload quality based on network conditions.
 */
class GetUploadQualityUseCase @Inject constructor(
    private val getCurrentNetworkStateUseCase: GetCurrentNetworkStateUseCase
) {
    operator fun invoke(): UploadQuality {
        val networkState = getCurrentNetworkStateUseCase()
        return if (networkState.isWifi) {
            UploadQuality.FULL
        } else {
            UploadQuality.COMPRESSED
        }
    }
}
