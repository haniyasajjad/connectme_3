package com.fast.assignment3

data class Contact(
    val userId: String,
    val profileImage: String,
    val name: String,
    val status: String // "none", "sent", "received"
)
