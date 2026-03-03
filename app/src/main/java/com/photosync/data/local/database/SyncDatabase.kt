package com.photosync.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.photosync.data.local.dao.SyncMetadataDao
import com.photosync.data.local.dao.SyncQueueDao
import com.photosync.data.local.entity.SyncItemEntity
import com.photosync.data.local.entity.SyncMetadataEntity
import com.photosync.domain.model.SyncStatus

/**
 * Room database for the sync system
 */
@Database(
    entities = [
        SyncItemEntity::class,
        SyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(SyncStatusConverter::class)
abstract class SyncDatabase : RoomDatabase() {

    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        private const val DATABASE_NAME = "sync_database"

        @Volatile
        private var instance: SyncDatabase? = null

        fun getInstance(context: Context): SyncDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): SyncDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SyncDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

/**
 * Type converter for SyncStatus enum
 */
class SyncStatusConverter {
    @TypeConverter
    fun fromString(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
    }

    @TypeConverter
    fun toString(status: SyncStatus): String {
        return status.name
    }
}
