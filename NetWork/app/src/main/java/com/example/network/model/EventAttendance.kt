package com.example.network.model

data class EventAttendance(
    val eventId: Int,
    val userId: Int,
    val status: String, // joined, interested, completed
    val joinedAt: Long = System.currentTimeMillis()
)