package com.photosync.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.photosync.domain.model.NetworkState
import com.photosync.domain.repository.NetworkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NetworkRepository using ConnectivityManager
 */
@Singleton
class NetworkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkRepository {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _networkState = MutableStateFlow(getCurrentNetworkState())
    private val networkStateFlow = _networkState.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun getNetworkStateFlow(): Flow<NetworkState> {
        return networkStateFlow
    }

    override fun getCurrentNetworkState(): NetworkState {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        return capabilities?.let { caps ->
            NetworkState(
                isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                isRoaming = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
            )
        } ?: NetworkState(
            isConnected = false,
            isWifi = false,
            isMetered = true,
            isRoaming = false
        )
    }

    override fun startMonitoring() {
        if (networkCallback != null) return // Already monitoring

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkState()
            }

            override fun onLost(network: Network) {
                updateNetworkState()
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                updateNetworkState()
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        networkCallback = callback
    }

    override fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }

    private fun updateNetworkState() {
        _networkState.value = getCurrentNetworkState()
    }
}