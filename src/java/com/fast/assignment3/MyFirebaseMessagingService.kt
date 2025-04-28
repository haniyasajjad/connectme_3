package com.fast.assignment3

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import android.util.Log
import org.json.JSONObject

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FCM"
        private const val CHANNEL_ID = "default_channel_id"
        private const val CHANNEL_NAME = "Default Channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val title = when (remoteMessage.data["type"]) {
                "message" -> "New Message"
                "follow_request" -> "Follow Request"
                "screenshot_alert" -> "Screenshot Alert"
                else -> "Notification"
            }
            val body = remoteMessage.data["body"] ?: when (remoteMessage.data["type"]) {
                "message" -> "You have a new message!"
                "follow_request" -> "New follow request received!"
                "screenshot_alert" -> "Screenshot detected in chat!"
                else -> "New notification received!"
            }
            sendNotification(title, body)
        }

        // Handle notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Title: ${it.title}")
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "New Notification", it.body ?: "You have a new message!")
        }
    }

    private fun sendRegistrationToServer(token: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "http://192.168.100.40/assignment3_backend/update_fcm_token.php"
        val jsonBody = JSONObject().apply {
            put("user_id", 1) // Replace with dynamic user_id
            put("fcm_token", token)
        }
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response -> Log.d(TAG, "Token saved: $response") },
            { error -> Log.e(TAG, "Error saving token: ${error.message}") }
        )
        queue.add(request)
    }

    private fun sendNotification(title: String, message: String) {
        Log.d(TAG, "Attempting to send notification: title=$title, message=$message")
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Default notification channel for FCM"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created: $CHANNEL_ID")
            }

            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Default icon
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "Notification sent with ID: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification: ${e.message}", e)
        }
    }
}