package com.example.network.model

data class Media(
    val mediaId: Int = 0,
    val userId: Int,
    val eventId: Int?,
    val type: String, // photo, video
    val url: String, // File path or URL
    val thumbnail: String? = null, // For video thumbnails
    val timestamp: Long = System.currentTimeMillis()
)