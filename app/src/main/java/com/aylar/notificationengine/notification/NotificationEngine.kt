package com.aylar.notificationengine.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aylar.notificationengine.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificationEngine(
    private val context: Context,
    private val queue: NotificationQueue,
    private val rateLimiter: NotificationRateLimiter,
    private val dndManager: DNDAwarenessManager,
    private val channelManager: NotificationChannelManager,
    private val groupManager: NotificationGroupManager,
    private val scope: CoroutineScope
) {
    private val notificationManager = NotificationManagerCompat.from(context)
    private val deferredPayloads = mutableListOf<NotificationPayload>()
    private var dndReceiver: BroadcastReceiver? = null

    init {
        channelManager.createAllChannels()
        startProcessingLoop()
        observeDNDChanges()
    }

    fun notify(payload: NotificationPayload) {
        queue.enqueue(payload)
    }

    fun notifyAll(payloads: List<NotificationPayload>) {
        payloads.forEach { queue.enqueue(it) }
    }

    private fun startProcessingLoop() {
        scope.launch(Dispatchers.Default) {
            while (isActive) {
                processNext()
                delay(100)
            }
        }
    }

    private suspend fun processNext() {
        val payload = queue.dequeue() ?: return

        if (System.currentTimeMillis() > payload.expiresAt) return

        if (dndManager.shouldDefer(payload)) {
            synchronized(deferredPayloads) {
                deferredPayloads.add(payload)
            }
            return
        }

        val channel = channelManager.getChannelForPriority(payload.priority)
        if (channelManager.isChannelBlocked(channel)) return

        if (!rateLimiter.isAllowed(payload)) {
            if (payload.priority.level >= NotificationPriority.HIGH.level) {
                val wait = rateLimiter.getWaitTimeMs(payload)
                delay(wait)
                queue.enqueue(payload)
            }
            return
        }

        postNotification(payload, channel)
        rateLimiter.recordSent(payload)
    }

    private fun postNotification(payload: NotificationPayload, channelId: String) {
        val notifId = payload.id.hashCode()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setPriority(payload.priority.toAndroidPriority())
            .setAutoCancel(true)
            .setWhen(payload.timestamp)
            .setShowWhen(true)

        payload.groupKey?.let { builder.setGroup(it) }

        payload.actions.forEach { action ->
            val intent = Intent(context, NotificationBroadcastReceiver::class.java).apply {
                this.action = ACTION_NOTIFICATION_ACTION
                putExtra("action_id", action.actionId)
                putExtra("notif_id", notifId)
                putExtra("group_key", payload.groupKey)
            }
            val pi = android.app.PendingIntent.getBroadcast(
                context,
                action.actionId.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    action.icon ?: R.drawable.ic_notification,
                    action.label,
                    pi
                ).build()
            )
        }

        val dismissIntent = Intent(context, NotificationBroadcastReceiver::class.java).apply {
            action = ACTION_NOTIFICATION_DISMISSED
            putExtra("notif_id", notifId)
            putExtra("group_key", payload.groupKey)
        }
        builder.setDeleteIntent(
            android.app.PendingIntent.getBroadcast(
                context,
                notifId,
                dismissIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        )

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(notifId, builder.build())
            groupManager.handleGrouping(payload, notifId)
        }
    }

    fun flushDeferred() {
        val toFlush = synchronized(deferredPayloads) {
            deferredPayloads.toList().also { deferredPayloads.clear() }
        }
        toFlush.forEach { queue.enqueue(it) }
    }

    private fun observeDNDChanges() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return
        dndReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
                    if (!dndManager.isDNDActive()) flushDeferred()
                }
            }
        }
        context.registerReceiver(
            dndReceiver,
            IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        )
    }

    private fun NotificationPriority.toAndroidPriority() = when (this) {
        NotificationPriority.CRITICAL -> NotificationCompat.PRIORITY_MAX
        NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
        NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
        NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
    }

    companion object {
        const val ACTION_NOTIFICATION_ACTION = "com.aylar.notificationengine.NOTIFICATION_ACTION"
        const val ACTION_NOTIFICATION_DISMISSED = "com.aylar.notificationengine.NOTIFICATION_DISMISSED"
    }
}
