package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class RealtimeAlertItem(
    val id: Int,
    val title: String,
    val message: String,
    val category: String,
    val timestampLabel: String
)

object NotificationHelper {
    private const val CHANNEL_ID = "apex_calc_realtime_channel"
    private const val CHANNEL_NAME = "Apex Calc Real-Time Sync & Security Alerts"
    private val nextId = AtomicInteger(1001)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Real-time cloud synchronization, encrypted backup alerts, and AI solver notifications."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun dispatchRealtimeNotification(
        context: Context,
        title: String,
        message: String,
        category: String = "SYNC"
    ): RealtimeAlertItem {
        ensureChannel(context)
        val id = nextId.getAndIncrement()
        val timeLabel = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (canNotify) {
            try {
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(id, notification)
            } catch (_: Exception) {
            }
        }

        return RealtimeAlertItem(
            id = id,
            title = title,
            message = message,
            category = category,
            timestampLabel = timeLabel
        )
    }
}
