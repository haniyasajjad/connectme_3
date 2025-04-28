package com.fast.assignment3

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSentByMe: Boolean = false,
    val seen: Boolean = false
)