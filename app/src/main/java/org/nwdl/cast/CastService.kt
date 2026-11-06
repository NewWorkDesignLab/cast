package org.nwdl.cast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder

/** Foreground service of type microphone: without it Android delivers no microphone or app audio while another app is in front. */
class CastService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Casting", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("NWDL Cast is live")
            .setContentText("What you see and hear is being shared.")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "cast"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) = context.startForegroundService(Intent(context, CastService::class.java))

        fun stop(context: Context) = context.stopService(Intent(context, CastService::class.java))
    }
}
