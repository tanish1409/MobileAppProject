package com.example.network.model

data class Event(
    val eventId: Int = 0,
    val clubId: Int,
    val hostId: Int,
    val title: String,
    val description: String?,
    val date: String, // Format: YYYY-MM-DD
    val time: String, // Format: HH:MM
    val locationLat: Double,
    val locationLong: Double,
    val locationAddress: String? = null,
    val maxParticipants: Int,
    val currentParticipants: Int = 0,
    val status: String = "upcoming", // upcoming, ongoing, completed, cancelled
    val createdAt: Long = System.currentTimeMillis()
)