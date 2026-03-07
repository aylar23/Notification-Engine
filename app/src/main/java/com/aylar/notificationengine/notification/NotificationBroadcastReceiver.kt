package com.aylar.notificationengine.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.aylar.notificationengine.NotificationEngineApplication

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? NotificationEngineApplication ?: return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val serviceIntent = Intent(context, NotificationProcessorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
            NotificationEngine.ACTION_NOTIFICATION_ACTION -> {
                val actionId = intent.getStringExtra("action_id") ?: return
                val notifId = intent.getIntExtra("notif_id", -1)
                handleAction(context, actionId, notifId)
            }
            NotificationEngine.ACTION_NOTIFICATION_DISMISSED -> {
                val notifId = intent.getIntExtra("notif_id", -1)
                val groupKey = intent.getStringExtra("group_key")
                app.notificationGroupManager.onNotificationDismissed(notifId, groupKey)
            }
        }
    }

    private fun handleAction(context: Context, actionId: String, notifId: Int) {
        when (actionId) {
            "action_reply" -> { /* Open reply UI */ }
            "action_dismiss" -> NotificationManagerCompat.from(context).cancel(notifId)
            "action_snooze" -> { /* Re-schedule */ }
            else -> { /* Dispatch to app action handler */ }
        }
    }
}
