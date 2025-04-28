package com.fast.assignment3

data class Post(
    val postId: String = "",       // Matches Firestore field "postId"
    val userId: String = "",       // Matches Firestore field "userId"
    val caption: String = "",      // Matches Firestore field "caption"
    val media: String = "",        // Matches Firestore field "media"
    val likes: Long = 0,           // Matches Firestore field "likes"
    val timestamp: Long = 0        // Matches Firestore field "timestamp"
)

