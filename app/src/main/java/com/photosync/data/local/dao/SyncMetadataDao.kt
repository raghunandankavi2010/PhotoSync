package com.photosync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photosync.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for sync metadata (timestamps, preferences)
 */
@Dao
interface SyncMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE key = :key")
    suspend fun getByKey(key: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE key = :key")
    fun getByKeyFlow(key: String): Flow<SyncMetadataEntity?>

    @Query("DELETE FROM sync_metadata WHERE key = :key")
    suspend fun deleteByKey(key: String)

    // Helper methods for specific metadata
    suspend fun getLongValue(key: String, defaultValue: Long = 0L): Long {
        return getByKey(key)?.longValue ?: defaultValue
    }

    suspend fun setLongValue(key: String, value: Long) {
        insert(SyncMetadataEntity(key = key, longValue = value))
    }

    suspend fun getStringValue(key: String, defaultValue: String? = null): String? {
        return getByKey(key)?.stringValue ?: defaultValue
    }

    suspend fun setStringValue(key: String, value: String) {
        insert(SyncMetadataEntity(key = key, stringValue = value))
    }

    suspend fun getBooleanValue(key: String, defaultValue: Boolean = false): Boolean {
        return getByKey(key)?.booleanValue ?: defaultValue
    }

    suspend fun setBooleanValue(key: String, value: Boolean) {
        insert(SyncMetadataEntity(key = key, booleanValue = value))
    }
}
