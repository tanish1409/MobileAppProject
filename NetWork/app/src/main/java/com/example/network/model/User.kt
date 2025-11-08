package com.example.network.model

data class User(
    val userId: Int = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val bio: String? = null,
    val location: String? = null,
    val preferences: String? = null, // JSON string of sport preferences
    val profileImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)