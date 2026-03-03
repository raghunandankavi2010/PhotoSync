package com.photosync.domain.usecase

import com.photosync.domain.model.NetworkState
import com.photosync.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to monitor network state
 */
class GetNetworkStateUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    operator fun invoke(): Flow<NetworkState> {
        return networkRepository.getNetworkStateFlow()
    }
}

/**
 * Use case to get current network state
 */
class GetCurrentNetworkStateUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    operator fun invoke(): NetworkState {
        return networkRepository.getCurrentNetworkState()
    }
}

/**
 * Use case to start network monitoring
 */
class StartNetworkMonitoringUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    operator fun invoke() {
        networkRepository.startMonitoring()
    }
}

/**
 * Use case to stop network monitoring
 */
class StopNetworkMonitoringUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    operator fun invoke() {
        networkRepository.stopMonitoring()
    }
}
