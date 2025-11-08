package com.example.network.model

data class Review(
    val reviewId: Int = 0,
    val clubId: Int,
    val userId: Int,
    val userName: String? = null, // For display
    val rating: Int, // 1-5
    val text: String?,
    val mediaUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)