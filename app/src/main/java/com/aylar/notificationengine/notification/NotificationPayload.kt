package com.aylar.notificationengine.notification

import java.util.UUID
import java.util.concurrent.TimeUnit

enum class NotificationPriority(val level: Int) {
    LOW(0), NORMAL(1), HIGH(2), CRITICAL(3)
}

enum class NotificationCategory {
    MESSAGE, ALERT, REMINDER, PROMO, SYSTEM
}

data class NotificationPayload(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val category: NotificationCategory = NotificationCategory.MESSAGE,
    val groupKey: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val actions: List<NotificationAction> = emptyList(),
    val data: Map<String, String> = emptyMap(),
    val expiresAt: Long = timestamp + TimeUnit.HOURS.toMillis(24),
    val collapseKey: String? = null
)

data class NotificationAction(
    val actionId: String,
    val label: String,
    val icon: Int? = null
)
