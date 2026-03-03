package com.photosync.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosync.domain.model.SyncItem
import com.photosync.domain.model.SyncStats
import com.photosync.domain.usecase.*
import com.photosync.presentation.intent.SyncIntent
import com.photosync.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Sync Status screen
 */
@HiltViewModel
class SyncStatusViewModel @Inject constructor(
    private val getSyncItemsUseCase: GetSyncItemsUseCase,
    private val getSyncStatsUseCase: GetSyncStatsUseCase,
    private val getSyncPreferencesUseCase: GetSyncPreferencesUseCase,
    private val retryFailedUploadsUseCase: RetryFailedUploadsUseCase,
    private val pauseAllUploadsUseCase: PauseAllUploadsUseCase,
    private val clearSyncedItemsUseCase: ClearSyncedItemsUseCase,
    private val scheduleUploadUseCase: ScheduleUploadUseCase,
    private val getPendingAndPausedItemsUseCase: GetPendingAndPausedItemsUseCase
) : ViewModel() {

    // UI State
    private val _syncItems = MutableStateFlow<List<SyncItem>>(emptyList())
    val syncItems: StateFlow<List<SyncItem>> = _syncItems.asStateFlow()

    private val _syncStats = MutableStateFlow<SyncStats?>(null)
    val syncStats: StateFlow<SyncStats?> = _syncStats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Collect sync items
                getSyncItemsUseCase()
                    .catch { e ->
                        _errorMessage.value = e.message
                    }
                    .collect { items ->
                        _syncItems.value = items
                    }
            } finally {
                _isLoading.value = false
            }
        }

        viewModelScope.launch {
            // Collect sync stats
            getSyncStatsUseCase()
                .catch { e ->
                    _errorMessage.value = e.message
                }
                .collect { stats ->
                    _syncStats.value = stats
                }
        }
    }

    fun processIntent(intent: SyncIntent) {
        when (intent) {
            is SyncIntent.Refresh -> refresh()
            is SyncIntent.RetryFailed -> retryFailed()
            is SyncIntent.PauseAll -> pauseAll()
            is SyncIntent.ResumeAll -> resumeAll()
            is SyncIntent.ClearSynced -> clearSynced()
            is SyncIntent.RetryItem -> retryItem(intent.mediaStoreId)
            is SyncIntent.CancelItem -> cancelItem(intent.mediaStoreId)
            is SyncIntent.DeleteItem -> deleteItem(intent.mediaStoreId)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Re-trigger collection
                loadData()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun retryFailed() {
        viewModelScope.launch {
            retryFailedUploadsUseCase()
                .onFailure { e ->
                    _errorMessage.value = e.message
                }
        }
    }

    private fun pauseAll() {
        viewModelScope.launch {
            pauseAllUploadsUseCase()
                .onFailure { e ->
                    _errorMessage.value = e.message
                }
        }
    }

    private fun resumeAll() {
        viewModelScope.launch {
            val pendingItems = getPendingAndPausedItemsUseCase()
            pendingItems.forEach { item ->
                scheduleUploadUseCase(item.mediaStoreId)
            }
        }
    }

    private fun clearSynced() {
        viewModelScope.launch {
            clearSyncedItemsUseCase()
                .onFailure { e ->
                    _errorMessage.value = e.message
                }
        }
    }

    private fun retryItem(mediaStoreId: Long) {
        viewModelScope.launch {
            scheduleUploadUseCase(mediaStoreId)
        }
    }

    private fun cancelItem(mediaStoreId: Long) {
        // TODO: Implement cancel logic
    }

    private fun deleteItem(mediaStoreId: Long) {
        // TODO: Implement delete logic
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
