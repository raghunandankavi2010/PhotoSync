package com.photosync.domain.repository

import com.photosync.domain.model.NetworkState
import kotlinx.coroutines.flow.Flow

/**
 * Repository for monitoring network state
 */
interface NetworkRepository {
    fun getNetworkStateFlow(): Flow<NetworkState>
    fun getCurrentNetworkState(): NetworkState
    fun startMonitoring()
    fun stopMonitoring()
}
