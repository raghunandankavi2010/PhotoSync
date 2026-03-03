package com.photosync.presentation.intent

/**
 * User intents for Sync Status screen
 */
sealed class SyncIntent {
    data object Refresh : SyncIntent()
    data object RetryFailed : SyncIntent()
    data object PauseAll : SyncIntent()
    data object ResumeAll : SyncIntent()
    data object ClearSynced : SyncIntent()
    data class RetryItem(val mediaStoreId: Long) : SyncIntent()
    data class CancelItem(val mediaStoreId: Long) : SyncIntent()
    data class DeleteItem(val mediaStoreId: Long) : SyncIntent()
}

/**
 * User intents for Gallery screen
 */
sealed class GalleryIntent {
    data object Refresh : GalleryIntent()
    data class SelectItem(val mediaStoreId: Long) : GalleryIntent()
    data class DeselectItem(val mediaStoreId: Long) : GalleryIntent()
    data object SelectAll : GalleryIntent()
    data object DeselectAll : GalleryIntent()
    data object SyncSelected : GalleryIntent()
    data class SyncItem(val mediaStoreId: Long) : GalleryIntent()
}

/**
 * User intents for Settings screen
 */
sealed class SettingsIntent {
    data class SetWifiOnly(val enabled: Boolean) : SettingsIntent()
    data class SetChargingOnly(val enabled: Boolean) : SettingsIntent()
    data class SetDataSaver(val enabled: Boolean) : SettingsIntent()
    data class SetAutoSync(val enabled: Boolean) : SettingsIntent()
    data class SetStripExif(val enabled: Boolean) : SettingsIntent()
    data class SetFolderFilters(val folders: List<String>) : SettingsIntent()
    data object ResetToDefaults : SettingsIntent()
}

/**
 * User intents for Sync Detail screen
 */
sealed class SyncDetailIntent {
    data object Load : SyncDetailIntent()
    data object Retry : SyncDetailIntent()
    data object Cancel : SyncDetailIntent()
    data object Delete : SyncDetailIntent()
    data object Share : SyncDetailIntent()
}
