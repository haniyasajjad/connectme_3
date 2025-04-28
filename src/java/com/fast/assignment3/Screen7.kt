package com.fast.assignment3

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
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
import com.fast.assignment3.Message
import com.fast.assignment3.MessageAdapter
import com.fast.assignment3.Utils
import org.json.JSONObject
import java.util.UUID

class Screen7 : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen7)

        queue = Volley.newRequestQueue(this)

        recipientUserId = intent.getStringExtra("userId") ?: ""
        if (userId.isEmpty() || recipientUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in or invalid recipient", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userNameTextView: TextView = findViewById(R.id.user_name)
        fetchRecipientName(recipientUserId, userNameTextView)

        recyclerView = findViewById(R.id.recycler_messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        messagesAdapter = MessageAdapter(messagesList)
        recyclerView.adapter = messagesAdapter

        val sendButton: ImageView = findViewById(R.id.send_button)
        val messageInput: EditText = findViewById(R.id.message_input)
        val imageButton: ImageView = findViewById(R.id.attachment_button)
        val backIcon: ImageView = findViewById(R.id.back_icon)

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

        backIcon.setOnClickListener {
            finish()
        }

        handler.post(loadMessagesRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(loadMessagesRunnable)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
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

    private fun fetchRecipientName(userId: String, userNameTextView: TextView) {
        val url = "http://192.168.100.40/assignment3_backend/get_user.php?user_id=$userId"
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                if (response.getString("status") == "success") {
                    val userJson = response.getJSONObject("user")
                    val name = userJson.getString("Name") ?: "Unknown"
                    userNameTextView.text = name
                } else {
                    userNameTextView.text = "Unknown"
                }
            },
            { error ->
                Log.e("Volley", "Error fetching user: ${error.message}")
                userNameTextView.text = "Unknown"
            }
        )
        queue.add(request)
    }

    private fun getChatId(user1: String, user2: String): String {
        val id1 = user1.toIntOrNull() ?: user1.hashCode()
        val id2 = user2.toIntOrNull() ?: user2.hashCode()
        return if (id1 < id2) "${user1}_$user2" else "${user2}_$user1"
    }

    private fun sendMessage(messageText: String = "", imageBase64: String? = null) {
        if (messageText.isBlank() && imageBase64 == null) return

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
            put("seen", false)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/send_vanish_message.php",
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
                        isSentByMe = true,
                        seen = false
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
            put("message", if (messageText.isNotEmpty()) "New vanishing message: $messageText" else "New vanishing image message")
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/send_notification.php",
            jsonBody,
            { response ->
                if (response.getString("status") == "success") {
                    Log.d("FCM", "Notification sent")
                } else {
                    Log.e("FCM", "Error: ${response.getString("message")}")
                }
            },
            { error -> Log.e("FCM", "Network error: ${error.message}") }
        )
        queue.add(request)
    }

    private fun loadMessages() {
        val chatId = getChatId(userId, recipientUserId)
        val url = "http://192.168.100.40/assignment3_backend/get_vanish_messages.php?chat_id=$chatId"
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                try {
                    if (response.getString("status") == "success") {
                        val newMessages = mutableListOf<Message>()
                        val messagesArray = response.getJSONArray("messages")
                        for (i in 0 until messagesArray.length()) {
                            val msgJson = messagesArray.getJSONObject(i)
                            val message = Message(
                                messageId = msgJson.getString("message_id"),
                                senderId = msgJson.getString("sender_id"),
                                receiverId = msgJson.getString("receiver_id"),
                                messageText = msgJson.getString("message_text"),
                                imageBase64 = msgJson.getString("image_base64").takeIf { it.isNotEmpty() },
                                timestamp = msgJson.getLong("timestamp"),
                                isSentByMe = msgJson.getString("sender_id") == userId,
                                seen = msgJson.getBoolean("seen")
                            )
                            // Only add messages that are not seen by the recipient
                            if (!message.seen || message.isSentByMe) {
                                newMessages.add(message)
                            }
                            // Mark received messages as seen
                            if (!message.isSentByMe && !message.seen) {
                                markMessageAsSeen(message.messageId, chatId)
                            }
                        }
                        messagesList.clear()
                        messagesList.addAll(newMessages)
                        messagesAdapter.notifyDataSetChanged()
                        recyclerView.scrollToPosition(messagesList.size - 1)
                    } else {
                        Toast.makeText(this, "Error: ${response.getString("message")}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("Volley", "JSON parsing error: ${e.message}")
                    Toast.makeText(this, "Parsing error", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Log.e("Volley", "Network error: ${error.message}")
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }

    private fun markMessageAsSeen(messageId: String, chatId: String) {
        val jsonBody = JSONObject().apply {
            put("message_id", messageId)
            put("chat_id", chatId)
            put("seen", true)
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            "http://192.168.100.40/assignment3_backend/update_vanish_message_seen.php",
            jsonBody,
            { response ->
                if (response.getString("status") == "success") {
                    Log.d("Volley", "Message marked as seen: $messageId")
                } else {
                    Log.e("Volley", "Error marking message as seen: ${response.getString("message")}")
                }
            },
            { error ->
                Log.e("Volley", "Network error marking seen: ${error.message}")
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
            "http://192.168.100.40/assignment3_backend/update_vanish_message.php",
            jsonBody,
            { response ->
                if (response.getString("status") == "success") {
                    val updatedMessages = messagesList.map {
                        if (it.messageId == messageId) it.copy(messageText = newText) else it
                    }
                    messagesList.clear()
                    messagesList.addAll(updatedMessages)
                    messagesAdapter.notifyDataSetChanged()
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
            "http://192.168.100.40/assignment3_backend/delete_vanish_message.php",
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