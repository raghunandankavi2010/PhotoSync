package com.photosync.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosync.domain.model.SyncItem
import com.photosync.domain.usecase.GetSyncItemsUseCase
import com.photosync.domain.usecase.ScheduleUploadUseCase
import com.photosync.presentation.intent.GalleryIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Gallery screen
 */
@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val getSyncItemsUseCase: GetSyncItemsUseCase,
    private val scheduleUploadUseCase: ScheduleUploadUseCase
) : ViewModel() {

    private val _galleryItems = MutableStateFlow<List<SyncItem>>(emptyList())
    val galleryItems: StateFlow<List<SyncItem>> = _galleryItems.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<Long>>(emptySet())
    val selectedItems: StateFlow<Set<Long>> = _selectedItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadGallery()
    }

    private fun loadGallery() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getSyncItemsUseCase()
                    .catch { e ->
                        _errorMessage.value = e.message
                    }
                    .collect { items ->
                        _galleryItems.value = items.sortedByDescending { it.dateAdded }
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun processIntent(intent: GalleryIntent) {
        when (intent) {
            is GalleryIntent.Refresh -> refresh()
            is GalleryIntent.SelectItem -> selectItem(intent.mediaStoreId)
            is GalleryIntent.DeselectItem -> deselectItem(intent.mediaStoreId)
            is GalleryIntent.SelectAll -> selectAll()
            is GalleryIntent.DeselectAll -> deselectAll()
            is GalleryIntent.SyncSelected -> syncSelected()
            is GalleryIntent.SyncItem -> syncItem(intent.mediaStoreId)
        }
    }

    private fun refresh() {
        loadGallery()
    }

    private fun selectItem(mediaStoreId: Long) {
        _selectedItems.value = _selectedItems.value + mediaStoreId
    }

    private fun deselectItem(mediaStoreId: Long) {
        _selectedItems.value = _selectedItems.value - mediaStoreId
    }

    private fun selectAll() {
        _selectedItems.value = _galleryItems.value.map { it.mediaStoreId }.toSet()
    }

    private fun deselectAll() {
        _selectedItems.value = emptySet()
    }

    private fun syncSelected() {
        viewModelScope.launch {
            _selectedItems.value.forEach { mediaStoreId ->
                scheduleUploadUseCase(mediaStoreId)
            }
            _selectedItems.value = emptySet()
        }
    }

    private fun syncItem(mediaStoreId: Long) {
        viewModelScope.launch {
            scheduleUploadUseCase(mediaStoreId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
