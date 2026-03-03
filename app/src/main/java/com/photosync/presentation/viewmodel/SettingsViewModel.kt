package com.photosync.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosync.domain.model.SyncPreferences
import com.photosync.domain.usecase.GetSyncPreferencesUseCase
import com.photosync.domain.usecase.UpdateSyncPreferencesUseCase
import com.photosync.presentation.intent.SettingsIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSyncPreferencesUseCase: GetSyncPreferencesUseCase,
    private val updateSyncPreferencesUseCase: UpdateSyncPreferencesUseCase
) : ViewModel() {

    private val _preferences = MutableStateFlow(SyncPreferences())
    val preferences: StateFlow<SyncPreferences> = _preferences.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getSyncPreferencesUseCase()
                    .catch { e ->
                        _errorMessage.value = e.message
                    }
                    .collect { prefs ->
                        _preferences.value = prefs
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetWifiOnly -> updateWifiOnly(intent.enabled)
            is SettingsIntent.SetChargingOnly -> updateChargingOnly(intent.enabled)
            is SettingsIntent.SetDataSaver -> updateDataSaver(intent.enabled)
            is SettingsIntent.SetAutoSync -> updateAutoSync(intent.enabled)
            is SettingsIntent.SetStripExif -> updateStripExif(intent.enabled)
            is SettingsIntent.SetFolderFilters -> updateFolderFilters(intent.folders)
            is SettingsIntent.ResetToDefaults -> resetToDefaults()
        }
    }

    private fun updateWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            val current = _preferences.value
            updateSyncPreferencesUseCase(current.copy(wifiOnly = enabled))
                .onFailure { e ->
                    _errorMessage.value = e.message
                }
        }
    }

    private fun updateChargingOnly(enabled: Boolean) {
        viewModelScope.launch {
            val current = _preferences.value
            updateSyncPreferencesUseCase(current.copy(chargingOnly = enabled))
                .onFailure { e ->
                    _errorMessage.value = e.message
                }
        }
    }

    private fun updateDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            val current = _preferences.value
            updateSyncPreferencesUseCase(current.copy(dataSaver = enabled))
                .onFailure { e ->
                    _errorMessage.value = e.message
                }
        }
    }

    private fun updateAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            val current = _preferences.value
            updateSyncPreferencesUseCase(current.copy(autoSync = enabled))
                .onFailure { e ->
                    _errorMessage.value = e.message
                }
        }
    }

    private fun updateStripExif(enabled: Boolean) {
        viewModelScope.launch {
            val current = _preferences.value
            updateSyncPreferencesUseCase(current.copy(stripExif = enabled))
                .onFailure { e ->
                    _errorMessage.value = e.message
                }
        }
    }

    private fun updateFolderFilters(folders: List<String>) {
        viewModelScope.launch {
            val current = _preferences.value
            updateSyncPreferencesUseCase(current.copy(folderFilters = folders))
                .onFailure { e ->
                    _errorMessage.value = e.message
                }
        }
    }

    private fun resetToDefaults() {
        viewModelScope.launch {
            updateSyncPreferencesUseCase(SyncPreferences())
                .onFailure { e ->
                    _errorMessage.value = e.message
                }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
