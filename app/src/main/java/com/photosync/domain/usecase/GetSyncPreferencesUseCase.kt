package com.photosync.domain.usecase

import com.photosync.domain.model.SyncPreferences
import com.photosync.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get sync preferences
 */
class GetSyncPreferencesUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<SyncPreferences> {
        return syncRepository.getSyncPreferencesFlow()
    }
}

/**
 * Use case to update sync preferences
 */
class UpdateSyncPreferencesUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(preferences: SyncPreferences): Result<Unit> {
        return runCatching {
            syncRepository.updateSyncPreferences(preferences)
        }
    }
}

/**
 * Use case to toggle Wi-Fi only setting
 */
class ToggleWifiOnlyUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> {
        return runCatching {
            val current = syncRepository.getSyncPreferences()
            syncRepository.updateSyncPreferences(current.copy(wifiOnly = enabled))
        }
    }
}

/**
 * Use case to toggle auto sync
 */
class ToggleAutoSyncUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> {
        return runCatching {
            val current = syncRepository.getSyncPreferences()
            syncRepository.updateSyncPreferences(current.copy(autoSync = enabled))
        }
    }
}
