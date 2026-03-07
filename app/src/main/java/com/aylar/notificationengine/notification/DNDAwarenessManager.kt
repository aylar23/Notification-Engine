package com.aylar.notificationengine.notification

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class DNDAwarenessManager(
    private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun isDNDActive(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val filter = notificationManager.currentInterruptionFilter
            filter != NotificationManager.INTERRUPTION_FILTER_ALL
        } else false
    }

    fun canBypassDND(priority: NotificationPriority): Boolean {
        if (!isDNDActive()) return true

        return when {
            priority == NotificationPriority.CRITICAL -> true
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val filter = notificationManager.currentInterruptionFilter
                filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY &&
                    priority.level >= NotificationPriority.HIGH.level
            }
            else -> false
        }
    }

    fun shouldDefer(payload: NotificationPayload): Boolean {
        return isDNDActive() && !canBypassDND(payload.priority)
    }

    fun requestDNDPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                activity.startActivity(intent)
            }
        }
    }
}
