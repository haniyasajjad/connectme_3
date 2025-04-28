package com.fast.assignment3

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class ApplicationClass : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable verbose logging for debugging (remove in production)
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // Initialize with your OneSignal App ID
        OneSignal.initWithContext(this, "fdfba351-1235-4786-83ba-928a372b9cad")

        // Prompt for push notification permission
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(true)
        }

        // Get and send player ID to backend
        val playerId = OneSignal.User.pushSubscription.id
        if (playerId != null) {
            sendPlayerIdToServer(playerId)
        }

        // Observe changes to push subscription
        OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                val newPlayerId = state.current.id
                if (newPlayerId != null) {
                    sendPlayerIdToServer(newPlayerId)
                }
            }
        })

        // Handle notification opened
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val data = event.notification.additionalData
                val receiverId = data?.optString("receiver_id")
                val senderId = data?.optString("sender_id")
                val type = data?.optString("type")

                if (type == "new_message" && senderId != null) {
                    val intent = Intent(this@ApplicationClass, Screen6::class.java).apply {
                        putExtra("userId", senderId) // Open chat with sender
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
                if (type == "new_follow" && senderId != null) {
                    val intent = Intent(this@ApplicationClass, Screen18::class.java).apply {
                        putExtra("userId", senderId) // Open chat with sender
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
            }
        })

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "chat_notifications",
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendPlayerIdToServer(playerId: String) {
        val userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_id", "") ?: return
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
            put("player_id", playerId)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/store_onesignal_player_id.php",
            jsonBody,
            { response ->
                if (response.getString("status") == "success") {
                    android.util.Log.d("OneSignal", "Player ID stored successfully")
                } else {
                    android.util.Log.e("OneSignal", "Error storing player ID: ${response.getString("message")}")
                }
            },
            { error ->
                android.util.Log.e("OneSignal", "Network error storing player ID: ${error.message}")
            }
        )
        queue.add(request)
    }
}