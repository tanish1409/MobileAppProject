package com.example.network.model

data class Club(
    val clubId: Int = 0,
    val name: String,
    val description: String?,
    val sportType: String,
    val locationLat: Double,
    val locationLong: Double,
    val locationAddress: String? = null, // Human-readable address
    val ownerId: Int,
    val memberCount: Int = 0,
    val rating: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)