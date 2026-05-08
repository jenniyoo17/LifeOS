package com.example.rememberclaw

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val channelId = "lifeos_channel"

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "LifeOS Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager.createNotificationChannel(channel)
        }

        // Open app intent
        val openIntent = Intent(
            context,
            MainActivity::class.java
        )

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

        // Notification
        val notification =
            NotificationCompat.Builder(
                context,
                channelId
            )
                .setContentTitle(
                    "🔔 LifeOS Reminder"
                )
                .setContentText(
                    "Time to complete your task"
                )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentIntent(
                    pendingIntent
                )
                .setAutoCancel(true)
                .build()

        manager.notify(
            1,
            notification
        )
    }
}