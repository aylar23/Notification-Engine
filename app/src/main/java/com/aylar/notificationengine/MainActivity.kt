package com.aylar.notificationengine

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aylar.notificationengine.notification.NotificationAction
import com.aylar.notificationengine.notification.NotificationCategory
import com.aylar.notificationengine.notification.NotificationPayload
import com.aylar.notificationengine.notification.NotificationPriority
import com.aylar.notificationengine.ui.theme.NotificationEngineTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        setContent {
            NotificationEngineTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DemoContent(
                        modifier = Modifier.padding(innerPadding),
                        onSendMessage = { sendMessageNotification() },
                        onSendScore = { sendScoreNotification() },
                        onSendBatch = { sendBatchNotifications() }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> { }
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun sendMessageNotification() {
        (application as? NotificationEngineApplication)?.notificationEngine?.notify(
            NotificationPayload(
                title = "New Message",
                body = "Hey, are you free tonight?",
                priority = NotificationPriority.HIGH,
                category = NotificationCategory.MESSAGE,
                groupKey = "group_messages",
                actions = listOf(
                    NotificationAction("action_reply", "Reply"),
                    NotificationAction("action_dismiss", "Dismiss")
                )
            )
        )
    }

    private fun sendScoreNotification() {
        (application as? NotificationEngineApplication)?.notificationEngine?.notify(
            NotificationPayload(
                title = "Score Update",
                body = "Lakers 98 - 95 Warriors (Q4)",
                priority = NotificationPriority.NORMAL,
                category = NotificationCategory.ALERT,
                collapseKey = "score_lakers_warriors"
            )
        )
    }

    private fun sendBatchNotifications() {
        val engine = (application as? NotificationEngineApplication)?.notificationEngine ?: return
        engine.notifyAll(
            listOf(
                NotificationPayload(
                    title = "Reminder 1",
                    body = "First reminder",
                    category = NotificationCategory.REMINDER
                ),
                NotificationPayload(
                    title = "Reminder 2",
                    body = "Second reminder",
                    category = NotificationCategory.REMINDER
                )
            )
        )
    }
}

@Composable
private fun DemoContent(
    modifier: Modifier = Modifier,
    onSendMessage: () -> Unit,
    onSendScore: () -> Unit,
    onSendBatch: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Notification Engine", modifier = Modifier.padding(bottom = 24.dp))
        Button(onClick = onSendMessage, modifier = Modifier.padding(8.dp)) {
            Text("Send message (grouped)")
        }
        Button(onClick = onSendScore, modifier = Modifier.padding(8.dp)) {
            Text("Send score (collapsible)")
        }
        Button(onClick = onSendBatch, modifier = Modifier.padding(8.dp)) {
            Text("Send batch")
        }
    }
}
