package com.aylar.notificationengine.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.ConcurrentHashMap

class NotificationGroupManager(
    private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val activeGroups = ConcurrentHashMap<String, MutableList<Int>>()

    companion object {
        private const val MAX_VISIBLE_IN_GROUP = 4
    }

    fun handleGrouping(payload: NotificationPayload, notifId: Int): Boolean {
        val groupKey = payload.groupKey ?: return false

        synchronized(activeGroups) {
            val groupIds = activeGroups.getOrPut(groupKey) { mutableListOf() }
            groupIds.add(notifId)
        }

        val groupIds = activeGroups[groupKey] ?: return true
        if (groupIds.size >= 2) {
            postGroupSummary(groupKey, groupIds.size, payload)
        }

        return true
    }

    private fun postGroupSummary(
        groupKey: String,
        count: Int,
        latest: NotificationPayload
    ) {
        val summaryId = groupKey.hashCode()

        val inboxStyle = NotificationCompat.InboxStyle()
            .setSummaryText("$count notifications")

        val summary = NotificationCompat.Builder(context, NotificationChannelManager.CHANNEL_NORMAL)
            .setSmallIcon(com.aylar.notificationengine.R.drawable.ic_notification)
            .setContentTitle(latest.title)
            .setContentText("$count new notifications")
            .setStyle(inboxStyle)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(summaryId, summary)
        }
    }

    fun onNotificationDismissed(notifId: Int, groupKey: String?) {
        groupKey ?: return
        synchronized(activeGroups) {
            activeGroups[groupKey]?.remove(notifId)
            if (activeGroups[groupKey].isNullOrEmpty()) {
                notificationManager.cancel(groupKey.hashCode())
                activeGroups.remove(groupKey)
            }
        }
    }
}
