package com.isqu.ai.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

class InferenceNotifier(private val context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ISQU AI Model Processing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi real-time pemuatan & hasil pemrosesan model offline."
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyModelResult(title: String, detail: String, id: Int = System.currentTimeMillis().toInt()) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim)
            .setContentTitle(title)
            .setContentText(detail)
            .setAutoCancel(true)
            .build()
        manager.notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "isqu_realtime_inference"
    }
}
