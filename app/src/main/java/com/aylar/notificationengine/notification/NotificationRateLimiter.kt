package com.aylar.notificationengine.notification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class NotificationRateLimiter {

    private val limits = mapOf(
        NotificationCategory.PROMO to RateLimit(max = 2, windowMs = TimeUnit.HOURS.toMillis(1)),
        NotificationCategory.MESSAGE to RateLimit(max = 10, windowMs = TimeUnit.MINUTES.toMillis(1)),
        NotificationCategory.ALERT to RateLimit(max = 5, windowMs = TimeUnit.MINUTES.toMillis(1)),
        NotificationCategory.SYSTEM to RateLimit(max = 20, windowMs = TimeUnit.MINUTES.toMillis(1)),
        NotificationCategory.REMINDER to RateLimit(max = 3, windowMs = TimeUnit.MINUTES.toMillis(30))
    )

    private val sentLog = ConcurrentHashMap<NotificationCategory, ArrayDeque<Long>>()

    data class RateLimit(val max: Int, val windowMs: Long)

    fun isAllowed(payload: NotificationPayload): Boolean {
        if (payload.priority == NotificationPriority.CRITICAL) return true

        val limit = limits[payload.category] ?: return true
        val now = System.currentTimeMillis()
        val timestamps = sentLog.getOrPut(payload.category) { ArrayDeque() }

        while (timestamps.isNotEmpty() && now - timestamps.first() > limit.windowMs) {
            timestamps.removeFirst()
        }

        return timestamps.size < limit.max
    }

    fun recordSent(payload: NotificationPayload) {
        val timestamps = sentLog.getOrPut(payload.category) { ArrayDeque() }
        timestamps.addLast(System.currentTimeMillis())
    }

    fun getWaitTimeMs(payload: NotificationPayload): Long {
        val limit = limits[payload.category] ?: return 0L
        val now = System.currentTimeMillis()
        val timestamps = sentLog[payload.category] ?: return 0L
        if (timestamps.size < limit.max) return 0L
        val oldest = timestamps.first()
        return (oldest + limit.windowMs) - now
    }
}
