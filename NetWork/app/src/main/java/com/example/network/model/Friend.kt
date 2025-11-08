package com.example.network.model

data class Friend(
    val userId: Int,
    val friendId: Int,
    val friendName: String? = null, // For display
    val friendProfileImage: String? = null, // For display
    val status: String, // pending, accepted, blocked
    val createdAt: Long = System.currentTimeMillis()
)