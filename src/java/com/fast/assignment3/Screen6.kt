package com.fast.assignment3

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.UUID

class Screen6 : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var recipientUserId: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var messagesAdapter: MessageAdapter
    private val messagesList = mutableListOf<Message>()
    private lateinit var queue: com.android.volley.RequestQueue
    private val userId: String by lazy {
        getSharedPreferences("user_prefs", MODE_PRIVATE).getString("user_id", "") ?: ""
    }

    private val handler = Handler(Looper.getMainLooper())

    private val loadMessagesRunnable = object : Runnable {
        override fun run() {
            loadMessages()
            handler.postDelayed(this, 5000) // Poll every 5 seconds
        }
    }

    // Add Runnable for polling status
    private val loadStatusRunnable = object : Runnable {
        override fun run() {
            fetchRecipientStatus()
            handler.postDelayed(this, 10000) // Poll every 10 seconds
        }
    }





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen6)

        queue = Volley.newRequestQueue(this)

        recipientUserId = intent.getStringExtra("userId") ?: ""
        val userNameTextView = findViewById<TextView>(R.id.user_name)
        val profileImageView = findViewById<ImageView>(R.id.profile_image2)
        val onlineStatusTextView = findViewById<TextView>(R.id.online_status)

        if (userId.isEmpty() || recipientUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in or invalid recipient", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Screen2::class.java))
            finish()
            return
        }
        updateUserStatus("online")

        fetchRecipientDetails(recipientUserId, userNameTextView, onlineStatusTextView, profileImageView)
        handler.post(loadStatusRunnable) // Start polling status

        recyclerView = findViewById(R.id.recycler_messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        messagesAdapter = MessageAdapter(messagesList)
        recyclerView.adapter = messagesAdapter

        val sendButton = findViewById<ImageView>(R.id.send_button)
        val messageInput = findViewById<EditText>(R.id.message_input)
        val viewProfileButton = findViewById<Button>(R.id.view_profile)
        val callIcon = findViewById<ImageView>(R.id.call_icon)
        val videocall = findViewById<ImageView>(R.id.video_call_icon)
        val imageButton = findViewById<ImageView>(R.id.attachment_button)
        handler.post(loadMessagesRunnable)


        callIcon.setOnClickListener {
            val intent = Intent(this, Screen8::class.java)
            intent.putExtra("userId", recipientUserId)
            startActivity(intent)
        }
        videocall.setOnClickListener {
            val intent = Intent(this, Screen9::class.java)
            intent.putExtra("userId", recipientUserId)
            startActivity(intent)
        }

        viewProfileButton.setOnClickListener {
            val intent = Intent(this, Screen7::class.java)
            intent.putExtra("userId", recipientUserId)
            startActivity(intent)
        }

        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageInput.text.clear()
            }
        }

        imageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        findViewById<ImageView>(R.id.back_icon).setOnClickListener {
            finish()
        }

        loadMessages()
    }

    private fun updateUserStatus(status: String) {
        val jsonBody = JSONObject().apply {
            put("user_id", userId)
            put("online_status", status == "online") // Send boolean for OneSignal
            put("last_online", System.currentTimeMillis())
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/update_status.php",
            jsonBody,
            { response ->
                if (response.getString("status") != "success") {
                    Log.e("StatusUpdate", "Failed to update status: ${response.getString("message")}")
                }
            },
            { error ->
                Log.e("StatusUpdate", "Network error: ${error.message}, code: ${error.networkResponse?.statusCode}")
            }
        )
        queue.add(request)
    }

    override fun onPause() {
        super.onPause()
        updateUserStatus("offline")
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    val base64String = Utils.encodeImageToBase64(bitmap)
                    sendMessage(imageBase64 = base64String)
                } catch (e: Exception) {
                    Log.e("Base64Error", "Error encoding image: ${e.message}")
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(loadMessagesRunnable)
        handler.removeCallbacks(loadStatusRunnable) // Stop polling status
    }

    private fun fetchRecipientDetails(
        userId: String,
        userNameTextView: TextView,
        onlineStatusTextView: TextView,
        profileImageView: ImageView
    ) {
        val url = "http://192.168.100.40/assignment3_backend/get_user_s.php?user_id=$userId"
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                if (response.getString("status") == "success") {
                    val userJson = response.getJSONObject("user")
                    val name = userJson.getString("Name") ?: "Unknown"
                    Log.d("For name", name)
                    val profileImageUrl = userJson.getString("Profileimage") ?: ""
                    val onlineStatus = userJson.getString("online_status") ?: "offline"
                    val lastOnline = userJson.optLong("last_online", 0L)

                    userNameTextView.text = name
                    updateOnlineStatus(onlineStatus, lastOnline, onlineStatusTextView)

                    if (profileImageUrl.isNotEmpty()) {
                        profileImageView.setImageBitmap(Utils.decodeBase64ToBitmap(profileImageUrl))
                    } else {
                        profileImageView.setImageResource(R.drawable.profile1)
                    }
                } else {
                    userNameTextView.text = "Unknown"
                    onlineStatusTextView.text = "Offline"
                    profileImageView.setImageResource(R.drawable.profile1)
                }
            },
            { error ->
                Log.e("Volley", "Error fetching user: ${error.message}")
                userNameTextView.text = "Unknown"
                onlineStatusTextView.text = "Offline"
                profileImageView.setImageResource(R.drawable.profile1)
            }
        )
        queue.add(request)
    }

    private fun fetchRecipientStatus() {
        val url = "http://192.168.100.40/assignment3_backend/get_user_status.php?user_id=$recipientUserId"
        val onlineStatusTextView = findViewById<TextView>(R.id.online_status)
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                if (response.getString("status") == "success") {
                    val onlineStatus = response.getString("online_status") ?: "offline"
                    val lastOnline = response.optLong("last_online", 0L)
                    updateOnlineStatus(onlineStatus, lastOnline, onlineStatusTextView)
                } else {
                    onlineStatusTextView.text = "Offline"
                }
            },
            { error ->
                Log.e("Volley", "Error fetching status: ${error.message}")
                onlineStatusTextView.text = "Offline"
            }
        )
        queue.add(request)
    }

    private fun updateOnlineStatus(status: String, lastOnline: Long, textView: TextView) {
        if (status == "online") {
            textView.text = "Online"
            textView.setTextColor(Color.GREEN) // Optional: Green for online
        } else {
            textView.text = formatLastOnline(lastOnline)
            textView.setTextColor(Color.GRAY) // Optional: Gray for offline
        }
    }

    private fun formatLastOnline(lastOnline: Long): String {
        if (lastOnline == 0L) return "Offline"

        val currentTime = System.currentTimeMillis()
        val diffMillis = currentTime - lastOnline
        val diffMinutes = diffMillis / (1000 * 60)
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when {
            diffMinutes < 1 -> "Last online just now"
            diffMinutes < 60 -> "Last online $diffMinutes mins ago"
            diffHours < 24 -> "Last online $diffHours hours ago"
            else -> "Last online $diffDays days ago"
        }
    }

    private fun getChatId(user1: String, user2: String): String {
        val id1 = user1.toIntOrNull() ?: user1.hashCode()
        val id2 = user2.toIntOrNull() ?: user2.hashCode()
        return if (id1 < id2) "${user1}_$user2" else "${user2}_$user1"
    }

    private fun sendMessage(messageText: String = "", imageBase64: String? = null) {
        if (messageText.isBlank() && imageBase64 == null) return // Fixed incomplete if

        updateUserStatus("online")

        val chatId = getChatId(userId, recipientUserId)
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val jsonBody = JSONObject().apply {
            put("message_id", messageId)
            put("chat_id", chatId)
            put("sender_id", userId)
            put("receiver_id", recipientUserId)
            put("message_text", messageText)
            put("image_base64", imageBase64 ?: "")
            put("timestamp", timestamp)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/send_message.php",
            jsonBody,
            { response ->
                if (response.getString("status") == "success") {
                    val newMessage = Message(
                        messageId = messageId,
                        senderId = userId,
                        receiverId = recipientUserId,
                        messageText = messageText,
                        imageBase64 = imageBase64,
                        timestamp = timestamp,
                        isSentByMe = true
                    )
                    messagesList.add(newMessage)
                    messagesAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messagesList.size - 1)
                    sendNotification(recipientUserId, messageText)
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error sending message: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun sendNotification(receiverId: String, messageText: String) {
        val jsonBody = JSONObject().apply {
            put("sender_id", userId)
            put("receiver_id", receiverId)
            put("message", if (messageText.isNotEmpty()) messageText else "New image message")
        }
        android.util.Log.d("OneSignal", "Sending notification: $jsonBody")

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/send_notification.php",
            jsonBody,
            { response ->
                if (response.getString("status") == "success") {
                    Log.d("OneSignal", "Notification sent")
                } else {
                    Log.e("OneSignal", "Error: ${response.getString("message")}")
                }
            },
            { error ->
                Log.e("OneSignal", "Network error: ${error.message}, code: ${error.networkResponse?.statusCode}")
            }
        )
        queue.add(request)
    }


    private fun loadMessages() {
        val chatId = getChatId(userId, recipientUserId)
        Log.d("ChatDebug", "Loading messages for chat_id: $chatId")
        val url = "http://192.168.100.40/assignment3_backend/get_messages.php?chat_id=$chatId"
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                Log.d("ChatDebug", "Response: $response")
                try {
                    if (response.getString("status") == "success") {
                        val newMessages = mutableListOf<Message>()
                        val messagesArray = response.getJSONArray("messages")
                        Log.d("ChatDebug", "Messages array length: ${messagesArray.length()}")
                        for (i in 0 until messagesArray.length()) {
                            val msgJson = messagesArray.getJSONObject(i)
                            val message = Message(
                                messageId = msgJson.getString("message_id"),
                                senderId = msgJson.getString("sender_id"),
                                receiverId = msgJson.getString("receiver_id"),
                                messageText = msgJson.getString("message_text"),
                                imageBase64 = msgJson.getString("image_base64").takeIf { it.isNotEmpty() },
                                timestamp = msgJson.getLong("timestamp"),
                                isSentByMe = msgJson.getString("sender_id") == userId
                            )
                            newMessages.add(message)
                            Log.d("ChatDebug", "Added message: ${message.messageId}")
                        }
                        messagesList.clear()
                        messagesList.addAll(newMessages)
                        Log.d("ChatDebug", "Messages list size before adapter update: ${messagesList.size}")
                        messagesAdapter.updateMessages(messagesList.toList())
                        recyclerView.scrollToPosition(messagesList.size - 1)
                        Log.d("ChatDebug", "Messages loaded: ${messagesList.size}")
                    } else {
                        val errorMsg = response.getString("message")
                        Log.e("ChatDebug", "Server error: $errorMsg")
                        Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ChatDebug", "JSON parsing error: ${e.message}")
                    Toast.makeText(this, "Parsing error", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("ChatDebug", "Network error: ${error.message}")
                Toast.makeText(this, "Network error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    fun showEditDialog(message: Message) {
        if (message.messageId.isEmpty()) {
            Toast.makeText(this, "Invalid message", Toast.LENGTH_SHORT).show()
            return
        }

        val editText = EditText(this)
        editText.setText(message.messageText)

        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    updateMessage(message.messageId, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateMessage(messageId: String, newText: String) {
        val chatId = getChatId(userId, recipientUserId)
        val jsonBody = JSONObject().apply {
            put("message_id", messageId)
            put("chat_id", chatId)
            put("message_text", newText)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/update_message.php",
            jsonBody,
            { response ->
                if (response.getString("status") == "success") {
                    val updatedMessages = messagesList.map {
                        if (it.messageId == messageId) it.copy(messageText = newText) else it
                    }
                    messagesAdapter.updateMessages(updatedMessages)
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error updating message: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    fun deleteMessage(message: Message) {
        val chatId = getChatId(userId, recipientUserId)
        val jsonBody = JSONObject().apply {
            put("message_id", message.messageId)
            put("chat_id", chatId)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/delete_message.php",
            jsonBody,
            { response ->
                if (response.getString("status") == "success") {
                    messagesList.remove(message)
                    messagesAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Error deleting message: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}