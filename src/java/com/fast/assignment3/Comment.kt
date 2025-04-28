package com.fast.assignment3

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val postId: String = "",
    val userName: String = "",
    val profileImageUrl: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
