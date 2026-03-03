package com.photosync.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.photosync.MainActivity
import com.photosync.R

/**
 * Foreground service for upload operations
 * Note: This is optional and can be used for critical uploads that must complete
 */
class UploadForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "upload_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_MEDIA_STORE_ID = "media_store_id"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mediaStoreId = intent?.getLongExtra(EXTRA_MEDIA_STORE_ID, -1L) ?: -1L

        val notification = createNotification("Uploading photo…")
        startForeground(NOTIFICATION_ID, notification)

        // The actual upload is handled by WorkManager
        // This service just keeps the process alive for critical uploads

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_sync),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_sync_desc)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
    }
}
