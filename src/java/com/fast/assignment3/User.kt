package com.fast.assignment3

data class User(
    val userId: String = "", // Firebase User ID
    val profileImageUrl: String = "", // URL of profile image from Firebase
    val userName: String = "" , // User's username
    val name: String = "" // User's name
)