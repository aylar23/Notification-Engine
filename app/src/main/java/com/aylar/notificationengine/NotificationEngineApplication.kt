package com.aylar.notificationengine

import android.app.Application
import com.aylar.notificationengine.notification.DNDAwarenessManager
import com.aylar.notificationengine.notification.NotificationChannelManager
import com.aylar.notificationengine.notification.NotificationEngine
import com.aylar.notificationengine.notification.NotificationGroupManager
import com.aylar.notificationengine.notification.NotificationQueue
import com.aylar.notificationengine.notification.NotificationRateLimiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class NotificationEngineApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    private val channelManager by lazy { NotificationChannelManager(this) }
    private val queue by lazy { NotificationQueue() }
    private val rateLimiter by lazy { NotificationRateLimiter() }
    private val dndManager by lazy { DNDAwarenessManager(this) }
    private val groupManager by lazy { NotificationGroupManager(this) }

    val notificationEngine: NotificationEngine by lazy {
        NotificationEngine(
            context = this,
            queue = queue,
            rateLimiter = rateLimiter,
            dndManager = dndManager,
            channelManager = channelManager,
            groupManager = groupManager,
            scope = applicationScope
        )
    }

    val notificationGroupManager: NotificationGroupManager
        get() = groupManager

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }
}
