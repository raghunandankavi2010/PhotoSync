package com.photosync.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosync.domain.model.SyncItem
import com.photosync.domain.repository.SyncRepository
import com.photosync.domain.usecase.ScheduleUploadUseCase
import com.photosync.presentation.intent.SyncDetailIntent
import com.photosync.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Sync Detail screen
 */
@HiltViewModel
class SyncDetailViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val scheduleUploadUseCase: ScheduleUploadUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mediaStoreId: Long = savedStateHandle.get<Long>("mediaStoreId") ?: -1L

    private val _syncItem = MutableStateFlow<UiState<SyncItem>>(UiState.Loading)
    val syncItem: StateFlow<UiState<SyncItem>> = _syncItem.asStateFlow()

    init {
        loadItem()
    }

    private fun loadItem() {
        viewModelScope.launch {
            _syncItem.value = UiState.Loading
            try {
                val item = syncRepository.getSyncItem(mediaStoreId)
                if (item != null) {
                    _syncItem.value = UiState.Success(item)
                } else {
                    _syncItem.value = UiState.Error("Item not found")
                }
            } catch (e: Exception) {
                _syncItem.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun processIntent(intent: SyncDetailIntent) {
        when (intent) {
            is SyncDetailIntent.Load -> loadItem()
            is SyncDetailIntent.Retry -> retryUpload()
            is SyncDetailIntent.Cancel -> cancelUpload()
            is SyncDetailIntent.Delete -> deleteItem()
            is SyncDetailIntent.Share -> shareItem()
        }
    }

    private fun retryUpload() {
        viewModelScope.launch {
            scheduleUploadUseCase(mediaStoreId)
            loadItem()
        }
    }

    private fun cancelUpload() {
        viewModelScope.launch {
            syncRepository.markAsPaused(mediaStoreId)
            loadItem()
        }
    }

    private fun deleteItem() {
        viewModelScope.launch {
            syncRepository.removeFromQueue(mediaStoreId)
            _syncItem.value = UiState.Error("Item deleted")
        }
    }

    private fun shareItem() {
        // TODO: Implement share functionality
    }
}
