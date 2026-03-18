package com.example.puretube

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.PendingIntent

class BackgroundAudioService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "UPDATE_PLAYSTATE") {
            val isPlaying = intent.getBooleanExtra("is_playing", true)
            updateNotification(isPlaying)
        } else {
            createNotificationChannel()
            startForeground(1, buildNotification(true))
        }
        return START_STICKY
    }

    private fun updateNotification(isPlaying: Boolean) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, buildNotification(isPlaying))
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val playPauseIntent = Intent("com.puretube.ACTION_PLAY_PAUSE")
        val pendingIntent = PendingIntent.getBroadcast(this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val icon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val title = if (isPlaying) "Pause" else "Play"
        
        return NotificationCompat.Builder(this, "BACKGROUND_AUDIO")
            .setContentTitle("PureTube")
            .setContentText(if (isPlaying) "Playing in background" else "Paused in background")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(icon, title, pendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "BACKGROUND_AUDIO",
                "Background Audio",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Clean up when user swipes app away from recents
        stopSelf()
    }
}
