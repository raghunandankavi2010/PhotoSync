package com.photosync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing sync metadata (timestamps, preferences)
 */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val key: String,
    val stringValue: String? = null,
    val longValue: Long? = null,
    val booleanValue: Boolean? = null,
    val intValue: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

// Metadata keys
object SyncMetadataKeys {
    const val LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
    const val WIFI_ONLY = "wifi_only"
    const val CHARGING_ONLY = "charging_only"
    const val DATA_SAVER = "data_saver"
    const val AUTO_SYNC = "auto_sync"
    const val STRIP_EXIF = "strip_exif"
    const val FOLDER_FILTERS = "folder_filters"
}
