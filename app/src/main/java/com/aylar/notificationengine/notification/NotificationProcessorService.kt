package com.aylar.notificationengine.notification

import android.app.Service
import android.content.Intent
import android.os.IBinder

class NotificationProcessorService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
