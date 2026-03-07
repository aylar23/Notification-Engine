package com.aylar.notificationengine.notification

import java.util.concurrent.PriorityBlockingQueue

class NotificationQueue {

    private val queue = PriorityBlockingQueue<NotificationPayload>(
        20,
        compareByDescending<NotificationPayload> { it.priority.level }
            .thenBy { it.timestamp }
    )

    fun enqueue(payload: NotificationPayload) {
        queue.removeIf { System.currentTimeMillis() > it.expiresAt }

        payload.collapseKey?.let { key ->
            queue.removeIf { it.collapseKey == key }
        }

        queue.offer(payload)
    }

    fun dequeue(): NotificationPayload? = queue.poll()

    fun peek(): NotificationPayload? = queue.peek()

    fun size(): Int = queue.size

    fun drainAll(): List<NotificationPayload> {
        val list = mutableListOf<NotificationPayload>()
        queue.drainTo(list)
        return list
    }
}
