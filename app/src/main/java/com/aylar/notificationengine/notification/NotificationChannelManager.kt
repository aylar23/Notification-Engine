package com.aylar.notificationengine.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

class NotificationChannelManager(
    private val context: Context
) {
    companion object {
        const val CHANNEL_CRITICAL = "channel_critical"
        const val CHANNEL_HIGH = "channel_high"
        const val CHANNEL_NORMAL = "channel_normal"
        const val CHANNEL_LOW = "channel_low"
        const val CHANNEL_PROMO = "channel_promo"
    }

    fun createAllChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channels = listOf(
            NotificationChannelCompat.Builder(
                CHANNEL_CRITICAL,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
                .setName("Critical Alerts")
                .setDescription("Urgent system notifications")
                .setVibrationEnabled(true)
                .setVibrationPattern(longArrayOf(0, 250, 150, 250))
                .build(),
            NotificationChannelCompat.Builder(
                CHANNEL_HIGH,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .setName("High Priority")
                .setDescription("Important updates")
                .setVibrationEnabled(true)
                .build(),
            NotificationChannelCompat.Builder(
                CHANNEL_NORMAL,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .setName("General")
                .setDescription("Standard notifications")
                .build(),
            NotificationChannelCompat.Builder(
                CHANNEL_LOW,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
                .setName("Low Priority")
                .setDescription("Silent informational updates")
                .build(),
            NotificationChannelCompat.Builder(
                CHANNEL_PROMO,
                NotificationManagerCompat.IMPORTANCE_MIN
            )
                .setName("Promotions")
                .setDescription("Offers and promotions")
                .build()
        )

        NotificationManagerCompat.from(context).apply {
            channels.forEach { createNotificationChannel(it) }
        }
    }

    fun getChannelForPriority(priority: NotificationPriority): String = when (priority) {
        NotificationPriority.CRITICAL -> CHANNEL_CRITICAL
        NotificationPriority.HIGH -> CHANNEL_HIGH
        NotificationPriority.NORMAL -> CHANNEL_NORMAL
        NotificationPriority.LOW -> CHANNEL_LOW
    }

    fun isChannelBlocked(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(channelId)
        return channel?.importance == NotificationManager.IMPORTANCE_NONE
    }
}
