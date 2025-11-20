package com.example.network.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.network.model.*
import java.security.MessageDigest

class DatabaseRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)
    private val db: SQLiteDatabase = dbHelper.writableDatabase

    // ============================================
    // USER OPERATIONS
    // ============================================

    fun createUser(name: String, email: String, password: String, bio: String? = null, location: String? = null): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("email", email)
            put("password_hash", hashPassword(password))
            put("bio", bio)
            put("location", location)
        }
        return db.insert("Users", null, values)
    }

    fun authenticateUser(email: String, password: String): User? {
        val hashedPassword = hashPassword(password)
        val cursor = db.query(
            "Users",
            null,
            "email = ? AND password_hash = ?",
            arrayOf(email, hashedPassword),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val user = cursorToUser(cursor)
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    fun getUserById(userId: Int): User? {
        val cursor = db.query(
            "Users",
            null,
            "user_id = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val user = cursorToUser(cursor)
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    fun getUserByEmail(email: String): User? {
        val cursor = db.query(
            "Users",
            null,
            "email = ?",
            arrayOf(email),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val user = cursorToUser(cursor)
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    // Search by email first; if not found, fall back to exact name match
    fun getUserByEmailOrName(query: String): User? {
        // Try email
        getUserByEmail(query)?.let { return it }

        // Try exact name match (case-insensitive)
        val cursor = db.query(
            "Users",
            null,
            "LOWER(name) = LOWER(?)",
            arrayOf(query),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val user = cursorToUser(cursor)
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    fun updateUserProfile(userId: Int, name: String?, bio: String?, location: String?, preferences: String?): Boolean {
        val values = ContentValues().apply {
            name?.let { put("name", it) }
            bio?.let { put("bio", it) }
            location?.let { put("location", it) }
            preferences?.let { put("preferences", it) }
        }
        return db.update("Users", values, "user_id = ?", arrayOf(userId.toString())) > 0
    }

    fun searchUsers(query: String): List<User> {
        val users = mutableListOf<User>()
        val cursor = db.query(
            "Users",
            null,
            "name LIKE ? OR email LIKE ?",
            arrayOf("%$query%", "%$query%"),
            null, null, null
        )

        while (cursor.moveToNext()) {
            users.add(cursorToUser(cursor))
        }
        cursor.close()
        return users
    }

    fun getUserClubCount(userId: Int): Int {
        val cursor = db.rawQuery(
            """
            SELECT COUNT(*) 
            FROM Clubs 
            WHERE owner_id = ?
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun getUserEventCount(userId: Int): Int {
        val cursor = db.rawQuery(
            """
            SELECT COUNT(*) 
            FROM Event_Attendance 
            WHERE user_id = ?
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    // ============================================
    // CLUB OPERATIONS
    // ============================================

    fun isUserClubOwner(userId: Int, clubId: Int): Boolean {
        val cursor = db.query(
            "Clubs",
            arrayOf("owner_id"),
            "club_id = ?",
            arrayOf(clubId.toString()),
            null, null, null
        )

        val isOwner = if (cursor.moveToFirst()) {
            val ownerId = cursor.getInt(cursor.getColumnIndexOrThrow("owner_id"))
            ownerId == userId
        } else {
            false
        }
        cursor.close()
        return isOwner
    }

    fun createClub(name: String, description: String?, sportType: String,
                   locationLat: Double, locationLong: Double, ownerId: Int): Long {
        var clubId: Long = -1L
        db.beginTransaction() // Start transaction
        try {
            // 1. Create the Club
            val clubValues = ContentValues().apply {
                put("name", name)
                put("description", description)
                put("sport_type", sportType)
                put("location_lat", locationLat)
                put("location_long", locationLong)
                put("owner_id", ownerId)
            }
            clubId = db.insert("Clubs", null, clubValues)

            if (clubId > 0) {
                // 2. Automatically enroll the Owner as a member
                val membershipValues = ContentValues().apply {
                    put("club_id", clubId)
                    put("user_id", ownerId)
                }
                db.insert("Club_Membership", null, membershipValues)
            }

            db.setTransactionSuccessful() // Commit if successful
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return clubId
    }

    fun joinClub(clubId: Int, userId: Int): Boolean {
        val values = ContentValues().apply {
            put("club_id", clubId)
            put("user_id", userId)
        }
        // CONFLICT_IGNORE ensures a user can't be added twice
        return db.insertWithOnConflict("Club_Membership", null, values,
            SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    fun leaveClub(clubId: Int, userId: Int): Boolean {
        // Prevent owner from leaving via this method (optional but good practice)
        // Note: A more complex query could check for ownership first. For now, rely on UI check.
        return db.delete(
            "Club_Membership",
            "club_id = ? AND user_id = ?",
            arrayOf(clubId.toString(), userId.toString())
        ) > 0
    }

    fun getClubMemberCount(clubId: Int): Int {
        val cursor = db.rawQuery(
            """
            SELECT COUNT(user_id) 
            FROM Club_Membership 
            WHERE club_id = ?
        """.trimIndent(),
            arrayOf(clubId.toString())
        )

        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    fun isUserMemberOfClub(clubId: Int, userId: Int): Boolean {
        val cursor = db.query(
            "Club_Membership",
            arrayOf("user_id"),
            "club_id = ? AND user_id = ?",
            arrayOf(clubId.toString(), userId.toString()),
            null, null, null
        )
        val isMember = cursor.count > 0
        cursor.close()
        return isMember
    }

    /**
     * Retrieves a list of users who are members of a specific club.
     * Excludes the owner who is technically also in the membership table.
     */
    fun getClubMembers(clubId: Int, ownerId: Int): List<User> {
        val members = mutableListOf<User>()

        // We join Club_Membership with Users to get the member details.
        // We exclude the owner_id to avoid confusion, as the owner is treated separately.
        val cursor = db.rawQuery(
            """
            SELECT U.* FROM Club_Membership CM
            INNER JOIN Users U ON CM.user_id = U.user_id
            WHERE CM.club_id = ? AND CM.user_id != ?
            ORDER BY U.name ASC
        """.trimIndent(),
            arrayOf(clubId.toString(), ownerId.toString())
        )

        // Assuming you have a cursorToUser(cursor) helper function:
        while (cursor.moveToNext()) {
            members.add(cursorToUser(cursor))
        }
        cursor.close()
        return members
    }

    fun getAllClubs(): List<Club> {
        val clubs = mutableListOf<Club>()
        val cursor = db.rawQuery("""
            SELECT c.*, 
                   COUNT(DISTINCT r.user_id) as member_count,
                   AVG(CAST(rev.rating AS REAL)) as avg_rating
            FROM Clubs c
            LEFT JOIN Reviews r ON c.club_id = r.club_id
            LEFT JOIN Reviews rev ON c.club_id = rev.club_id
            GROUP BY c.club_id
        """, null)

        while (cursor.moveToNext()) {
            clubs.add(cursorToClub(cursor))
        }
        cursor.close()
        return clubs
    }

    fun getClubById(clubId: Int): Club? {
        val cursor = db.rawQuery("""
            SELECT c.*, 
                   COUNT(DISTINCT r.user_id) as member_count,
                   AVG(CAST(rev.rating AS REAL)) as avg_rating
            FROM Clubs c
            LEFT JOIN Reviews r ON c.club_id = r.club_id
            LEFT JOIN Reviews rev ON c.club_id = rev.club_id
            WHERE c.club_id = ?
            GROUP BY c.club_id
        """, arrayOf(clubId.toString()))

        return if (cursor.moveToFirst()) {
            val club = cursorToClub(cursor)
            cursor.close()
            club
        } else {
            cursor.close()
            null
        }
    }

    fun getClubsBySportType(sportType: String): List<Club> {
        val clubs = mutableListOf<Club>()
        val cursor = db.query(
            "Clubs",
            null,
            "sport_type = ?",
            arrayOf(sportType),
            null, null, null
        )

        while (cursor.moveToNext()) {
            clubs.add(cursorToClub(cursor))
        }
        cursor.close()
        return clubs
    }

    fun searchClubs(query: String): List<Club> {
        val clubs = mutableListOf<Club>()
        val cursor = db.query(
            "Clubs",
            null,
            "name LIKE ? OR sport_type LIKE ?",
            arrayOf("%$query%", "%$query%"),
            null, null, null
        )

        while (cursor.moveToNext()) {
            clubs.add(cursorToClub(cursor))
        }
        cursor.close()
        return clubs
    }

    fun getClubsNearLocation(lat: Double, lng: Double, radiusKm: Double): List<Club> {
        // Simple bounding box calculation
        val latDelta = radiusKm / 111.0 // Approximate km per degree latitude
        val lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)))

        val clubs = mutableListOf<Club>()
        val cursor = db.query(
            "Clubs",
            null,
            "location_lat BETWEEN ? AND ? AND location_long BETWEEN ? AND ?",
            arrayOf(
                (lat - latDelta).toString(),
                (lat + latDelta).toString(),
                (lng - lngDelta).toString(),
                (lng + lngDelta).toString()
            ),
            null, null, null
        )

        while (cursor.moveToNext()) {
            clubs.add(cursorToClub(cursor))
        }
        cursor.close()
        return clubs
    }

    // ============================================
    // EVENT OPERATIONS
    // ============================================

    fun createEvent(clubId: Int, hostId: Int, title: String, description: String?,
                    date: String, time: String, locationLat: Double, locationLong: Double,
                    maxParticipants: Int): Long {
        val values = ContentValues().apply {
            put("club_id", clubId)
            put("host_id", hostId)
            put("title", title)
            put("description", description)
            put("date", date)
            put("time", time)
            put("location_lat", locationLat)
            put("location_long", locationLong)
            put("max_participants", maxParticipants)
        }
        return db.insert("Events", null, values)
    }

    fun getEventsByClub(clubId: Int): List<Event> {
        val events = mutableListOf<Event>()
        val cursor = db.rawQuery("""
            SELECT e.*, COUNT(ea.user_id) as participant_count
            FROM Events e
            LEFT JOIN Event_Attendance ea ON e.event_id = ea.event_id 
            WHERE e.club_id = ?
            GROUP BY e.event_id
            ORDER BY e.date, e.time
        """, arrayOf(clubId.toString()))

        while (cursor.moveToNext()) {
            events.add(cursorToEvent(cursor))
        }
        cursor.close()
        return events
    }

    fun getUpcomingEvents(): List<Event> {
        val events = mutableListOf<Event>()
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        val cursor = db.rawQuery("""
            SELECT e.*, COUNT(ea.user_id) as participant_count
            FROM Events e
            LEFT JOIN Event_Attendance ea ON e.event_id = ea.event_id
            WHERE e.date >= ?
            GROUP BY e.event_id
            ORDER BY e.date, e.time
            LIMIT 20
        """, arrayOf(today))

        while (cursor.moveToNext()) {
            events.add(cursorToEvent(cursor))
        }
        cursor.close()
        return events
    }

    fun getEventById(eventId: Int): Event? {
        val cursor = db.rawQuery("""
            SELECT e.*, COUNT(ea.user_id) as participant_count
            FROM Events e
            LEFT JOIN Event_Attendance ea ON e.event_id = ea.event_id
            WHERE e.event_id = ?
            GROUP BY e.event_id
        """, arrayOf(eventId.toString()))

        return if (cursor.moveToFirst()) {
            val event = cursorToEvent(cursor)
            cursor.close()
            event
        } else {
            cursor.close()
            null
        }
    }

    fun joinEvent(eventId: Int, userId: Int, status: String = "joined"): Boolean {
        val values = ContentValues().apply {
            put("event_id", eventId)
            put("user_id", userId)
            put("status", status)
        }
        return db.insertWithOnConflict("Event_Attendance", null, values,
            SQLiteDatabase.CONFLICT_REPLACE) != -1L
    }

    fun leaveEvent(eventId: Int, userId: Int): Boolean {
        return db.delete("Event_Attendance",
            "event_id = ? AND user_id = ?",
            arrayOf(eventId.toString(), userId.toString())) > 0
    }

    fun getUserEventAttendance(userId: Int): List<Event> {
        val events = mutableListOf<Event>()
        val cursor = db.rawQuery("""
            SELECT e.*, COUNT(ea2.user_id) as participant_count
            FROM Events e
            INNER JOIN Event_Attendance ea ON e.event_id = ea.event_id
            LEFT JOIN Event_Attendance ea2 ON e.event_id = ea2.event_id
            WHERE ea.user_id = ?
            GROUP BY e.event_id
            ORDER BY e.date, e.time
        """, arrayOf(userId.toString()))

        while (cursor.moveToNext()) {
            events.add(cursorToEvent(cursor))
        }
        cursor.close()
        return events
    }

    fun isUserAttendingEvent(eventId: Int, userId: Int): Boolean {
        val cursor = db.query(
            "Event_Attendance",
            arrayOf("user_id"),
            "event_id = ? AND user_id = ?",
            arrayOf(eventId.toString(), userId.toString()),
            null, null, null
        )
        val isAttending = cursor.count > 0
        cursor.close()
        return isAttending
    }

    /**
     * Retrieves a list of users attending a specific event.
     */
    fun getEventAttendees(eventId: Int): List<User> {
        val attendees = mutableListOf<User>()

        // Join Event_Attendance with Users to get profile details for every attendee
        val cursor = db.rawQuery(
            """
            SELECT U.* FROM Event_Attendance EA
            INNER JOIN Users U ON EA.user_id = U.user_id
            WHERE EA.event_id = ? AND EA.status = 'joined'
            ORDER BY U.name ASC
        """.trimIndent(),
            arrayOf(eventId.toString())
        )

        // Assuming you have a cursorToUser(cursor) helper function that works for the User model
        while (cursor.moveToNext()) {
            attendees.add(cursorToUser(cursor))
        }
        cursor.close()
        return attendees
    }

    /**
     * Retrieves clubs owned by a specific user.
     */
    fun getClubsOwnedByUser(userId: Int): List<Club> {
        val clubs = mutableListOf<Club>()
        val cursor = db.query(
            "Clubs",
            null,
            "owner_id = ?",
            arrayOf(userId.toString()),
            null, null, null
        )
        while (cursor.moveToNext()) {
            clubs.add(cursorToClub(cursor))
        }
        cursor.close()
        return clubs
    }

    /**
     * Retrieves clubs a user has joined (is a member of, excluding clubs they own).
     */
    fun getClubsJoinedByUser(userId: Int): List<Club> {
        val clubs = mutableListOf<Club>()
        val cursor = db.rawQuery(
            """
        SELECT C.* FROM Clubs C
        INNER JOIN Club_Membership CM ON C.club_id = CM.club_id
        WHERE CM.user_id = ? AND C.owner_id != ?
        """.trimIndent(),
            arrayOf(userId.toString(), userId.toString())
        )

        while (cursor.moveToNext()) {
            clubs.add(cursorToClub(cursor))
        }
        cursor.close()
        return clubs
    }

    // ============================================
    // REVIEW OPERATIONS
    // ============================================

    fun addReview(clubId: Int, userId: Int, rating: Int, text: String?, mediaUrl: String?): Long {
        val values = ContentValues().apply {
            put("club_id", clubId)
            put("user_id", userId)
            put("rating", rating)
            put("text", text)
            put("media_url", mediaUrl)
        }
        return db.insert("Reviews", null, values)
    }

    fun getReviewsByClub(clubId: Int): List<Review> {
        val reviews = mutableListOf<Review>()
        val cursor = db.rawQuery("""
            SELECT r.*, u.name as user_name
            FROM Reviews r
            INNER JOIN Users u ON r.user_id = u.user_id
            WHERE r.club_id = ?
            ORDER BY r.timestamp DESC
        """, arrayOf(clubId.toString()))

        while (cursor.moveToNext()) {
            reviews.add(cursorToReview(cursor))
        }
        cursor.close()
        return reviews
    }

    fun getClubAverageRating(clubId: Int): Float {
        val cursor = db.rawQuery("""
            SELECT AVG(CAST(rating AS REAL)) as avg_rating
            FROM Reviews
            WHERE club_id = ?
        """, arrayOf(clubId.toString()))

        return if (cursor.moveToFirst()) {
            val rating = cursor.getFloat(0)
            cursor.close()
            rating
        } else {
            cursor.close()
            0f
        }
    }

    /**
     * Add review with media attachment
     * This method adds the review AND saves media to Media table if provided
     */
    fun addReviewWithMedia(
        clubId: Int,
        userId: Int,
        rating: Int,
        text: String?,
        mediaPath: String?,
        mediaType: String?
    ): Long {
        // First add the review
        val reviewId = addReview(clubId, userId, rating, text, mediaPath)

        // If media is attached and review was successful, also save to Media table
        if (reviewId > 0 && mediaPath != null && mediaType != null) {
            saveMedia(userId, null, mediaType, mediaPath)
        }

        return reviewId
    }

    /**
     * Get reviews with user profile images
     * Returns reviews with additional user profile image path
     */
    fun getReviewsWithUserImages(clubId: Int): List<Map<String, Any?>> {
        val reviews = mutableListOf<Map<String, Any?>>()
        val cursor = db.rawQuery("""
            SELECT r.*, u.name as user_name, u.profile_image_path
            FROM Reviews r
            INNER JOIN Users u ON r.user_id = u.user_id
            WHERE r.club_id = ?
            ORDER BY r.timestamp DESC
        """, arrayOf(clubId.toString()))

        while (cursor.moveToNext()) {
            val reviewMap = mapOf(
                "review_id" to cursor.getInt(cursor.getColumnIndexOrThrow("review_id")),
                "club_id" to cursor.getInt(cursor.getColumnIndexOrThrow("club_id")),
                "user_id" to cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                "user_name" to cursor.getString(cursor.getColumnIndexOrThrow("user_name")),
                "profile_image_path" to cursor.getString(cursor.getColumnIndexOrThrow("profile_image_path")),
                "rating" to cursor.getInt(cursor.getColumnIndexOrThrow("rating")),
                "text" to cursor.getString(cursor.getColumnIndexOrThrow("text")),
                "media_url" to cursor.getString(cursor.getColumnIndexOrThrow("media_url")),
                "timestamp" to cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
            )
            reviews.add(reviewMap)
        }
        cursor.close()
        return reviews
    }

    // ============================================
    // FRIEND OPERATIONS
    // ============================================

    fun sendFriendRequest(userId: Int, friendId: Int): Boolean {
        if (userId == friendId) return false

        // Already friends?
        if (areFriends(userId, friendId)) return false

        // Existing pending either direction?
        if (hasPendingFriendRequest(userId, friendId)) return false

        val values = ContentValues().apply {
            put("user_id", userId)
            put("friend_id", friendId)
            put("status", "pending")
        }

        return db.insert("Friends", null, values) != -1L
    }

    fun acceptFriendRequest(senderId: Int, receiverId: Int): Boolean {
        db.beginTransaction()
        return try {
            // 1) Update SENDER -> RECEIVER to accepted
            val values = ContentValues().apply {
                put("status", "accepted")
            }

            val whereArgs = arrayOf(senderId.toString(), receiverId.toString(), "pending")
            val rowsUpdated = db.update(
                "Friends",
                values,
                "user_id = ? AND friend_id = ? AND status = ?",
                whereArgs
            )

            if (rowsUpdated <= 0) {
                db.endTransaction()
                return false
            }

            // 2) Insert RECEIVER -> SENDER as accepted if not already there
            val reverseValues = ContentValues().apply {
                put("user_id", receiverId)
                put("friend_id", senderId)
                put("status", "accepted")
            }

            db.insertWithOnConflict(
                "Friends",
                null,
                reverseValues,
                SQLiteDatabase.CONFLICT_IGNORE
            )

            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Rejects a friend request by deleting the pending row.
     * Note: SENDER ID is the user who sent the request.
     * RECEIVER ID is the current user (the one rejecting).
     */
    fun rejectFriendRequest(senderId: Int, receiverId: Int): Boolean {
        return db.delete(
            "Friends",
            "user_id = ? AND friend_id = ? AND status = ?",
            arrayOf(senderId.toString(), receiverId.toString(), "pending")
        ) > 0
    }

    fun getFriends(userId: Int): List<Friend> {
        val friends = mutableListOf<Friend>()
        val cursor = db.rawQuery("""
            SELECT f.*, u.name as friend_name
            FROM Friends f
            INNER JOIN Users u ON f.friend_id = u.user_id
            WHERE f.user_id = ? AND f.status = 'accepted'
        """, arrayOf(userId.toString()))

        while (cursor.moveToNext()) {
            friends.add(cursorToFriend(cursor))
        }
        cursor.close()
        return friends
    }

    fun removeFriend(userId: Int, friendId: Int): Boolean {
        db.beginTransaction()
        return try {
            // Delete both directions
            db.delete(
                "Friends",
                "user_id = ? AND friend_id = ? AND status = 'accepted'",
                arrayOf(userId.toString(), friendId.toString())
            )

            db.delete(
                "Friends",
                "user_id = ? AND friend_id = ? AND status = 'accepted'",
                arrayOf(friendId.toString(), userId.toString())
            )

            db.setTransactionSuccessful()
            true
        } finally {
            db.endTransaction()
        }
    }

    fun getPendingFriendRequests(userId: Int): List<Friend> {
        val requests = mutableListOf<Friend>()
        val cursor = db.rawQuery("""
            SELECT f.*, u.name as friend_name
            FROM Friends f
            INNER JOIN Users u ON f.user_id = u.user_id
            WHERE f.friend_id = ? AND f.status = 'pending'
        """, arrayOf(userId.toString()))

        while (cursor.moveToNext()) {
            requests.add(cursorToFriend(cursor))
        }
        cursor.close()
        return requests
    }

    fun hasPendingFriendRequest(userId: Int, friendId: Int): Boolean {
        val cursor = db.query(
            "Friends",
            arrayOf("status"),
            "((user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)) AND status = ?",
            arrayOf(
                userId.toString(), friendId.toString(),
                friendId.toString(), userId.toString(),
                "pending"
            ),
            null, null, null
        )

        val hasPending = cursor.moveToFirst()
        cursor.close()
        return hasPending
    }

    fun areFriends(userId: Int, friendId: Int): Boolean {
        val cursor = db.query(
            "Friends",
            arrayOf("status"),
            "(user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)",
            arrayOf(userId.toString(), friendId.toString(), friendId.toString(), userId.toString()),
            null, null, null
        )

        val areFriends = cursor.moveToFirst() && cursor.getString(0) == "accepted"
        cursor.close()
        return areFriends
    }

    // ============================================
    // MEDIA OPERATIONS
    // ============================================

    fun saveMedia(userId: Int, eventId: Int?, type: String, url: String): Long {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("event_id", eventId)
            put("type", type)
            put("url", url)
        }
        return db.insert("Media", null, values)
    }

    fun getMediaByEvent(eventId: Int): List<Media> {
        val mediaList = mutableListOf<Media>()
        val cursor = db.query(
            "Media",
            null,
            "event_id = ?",
            arrayOf(eventId.toString()),
            null, null, "timestamp DESC"
        )

        while (cursor.moveToNext()) {
            mediaList.add(cursorToMedia(cursor))
        }
        cursor.close()
        return mediaList
    }

    //Convenience method to save audio media
    fun saveAudioMedia(userId: Int, eventId: Int?, audioPath: String): Long {
        return saveMedia(userId, eventId, "audio", audioPath)
    }

    //Save media with video type
    fun saveVideoMedia(userId: Int, eventId: Int?, videoPath: String, thumbnailPath: String?): Long {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("event_id", eventId)
            put("type", "video")
            put("url", videoPath)
            put("thumbnail", thumbnailPath)
        }
        return db.insert("Media", null, values)
    }

    //Get all media by user
    fun getMediaByUser(userId: Int): List<Media> {
        val mediaList = mutableListOf<Media>()
        val cursor = db.query(
            "Media",
            null,
            "user_id = ?",
            arrayOf(userId.toString()),
            null, null, "timestamp DESC"
        )

        while (cursor.moveToNext()) {
            mediaList.add(cursorToMedia(cursor))
        }
        cursor.close()
        return mediaList
    }

    //Delete media by ID
    fun deleteMedia(mediaId: Int): Boolean {
        return db.delete("Media", "media_id = ?", arrayOf(mediaId.toString())) > 0
    }

    //Get media by type (photo, video, audio)
    fun getMediaByType(type: String): List<Media> {
        val mediaList = mutableListOf<Media>()
        val cursor = db.query(
            "Media",
            null,
            "type = ?",
            arrayOf(type),
            null, null, "timestamp DESC"
        )

        while (cursor.moveToNext()) {
            mediaList.add(cursorToMedia(cursor))
        }
        cursor.close()
        return mediaList
    }

    //Update user's profile image path
    fun updateUserProfileImage(userId: Int, imagePath: String): Boolean {
        val values = ContentValues().apply {
            put("profile_image_path", imagePath)
        }
        return db.update("Users", values, "user_id = ?", arrayOf(userId.toString())) > 0
    }

    //Get user's profile image path
    fun getUserProfileImage(userId: Int): String? {
        val cursor = db.query(
            "Users",
            arrayOf("profile_image_path"),
            "user_id = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val path = cursor.getString(0)
            cursor.close()
            path
        } else {
            cursor.close()
            null
        }
    }

    //TODO: Fix event_id to become club_id
    fun getClubMediaWorkaround(clubId: Int): List<Media> {
        val mediaList = mutableListOf<Media>()
        val cursor = db.query(
            "Media",
            null,
            "event_id = ?", // Querying on event_id, which currently holds the clubId
            arrayOf(clubId.toString()),
            null, null, "timestamp DESC"
        )

        while (cursor.moveToNext()) {
            mediaList.add(cursorToMedia(cursor))
        }
        cursor.close()
        return mediaList
    }

    fun verifyCurrentPassword(userId: Int, password: String): Boolean {
        val hashedPassword = hashPassword(password)
        val cursor = db.query(
            "Users",
            arrayOf("user_id"),
            "user_id = ? AND password_hash = ?",
            arrayOf(userId.toString(), hashedPassword),
            null, null, null
        )
        val isValid = cursor.moveToFirst()
        cursor.close()
        return isValid
    }

    fun updateUserPassword(userId: Int, newPassword: String): Boolean {
        val newHashedPassword = hashPassword(newPassword)
        val values = ContentValues().apply {
            put("password_hash", newHashedPassword)
        }
        return db.update("Users", values, "user_id = ?", arrayOf(userId.toString())) > 0
    }


    // ============================================
    // HELPER METHODS
    // ============================================

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun cursorToUser(cursor: Cursor): User {
        return User(
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
            passwordHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash")),
            bio = cursor.getString(cursor.getColumnIndexOrThrow("bio")),
            location = cursor.getString(cursor.getColumnIndexOrThrow("location")),
            preferences = cursor.getString(cursor.getColumnIndexOrThrow("preferences"))
        )
    }

    private fun cursorToClub(cursor: Cursor): Club {
        return Club(
            clubId = cursor.getInt(cursor.getColumnIndexOrThrow("club_id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
            sportType = cursor.getString(cursor.getColumnIndexOrThrow("sport_type")),
            locationLat = cursor.getDouble(cursor.getColumnIndexOrThrow("location_lat")),
            locationLong = cursor.getDouble(cursor.getColumnIndexOrThrow("location_long")),
            ownerId = cursor.getInt(cursor.getColumnIndexOrThrow("owner_id")),
            memberCount = if (cursor.getColumnIndex("member_count") != -1)
                cursor.getInt(cursor.getColumnIndexOrThrow("member_count")) else 0,
            rating = if (cursor.getColumnIndex("avg_rating") != -1)
                cursor.getFloat(cursor.getColumnIndexOrThrow("avg_rating")) else 0f
        )
    }

    private fun cursorToEvent(cursor: Cursor): Event {
        return Event(
            eventId = cursor.getInt(cursor.getColumnIndexOrThrow("event_id")),
            clubId = cursor.getInt(cursor.getColumnIndexOrThrow("club_id")),
            hostId = cursor.getInt(cursor.getColumnIndexOrThrow("host_id")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
            description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
            date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
            time = cursor.getString(cursor.getColumnIndexOrThrow("time")),
            locationLat = cursor.getDouble(cursor.getColumnIndexOrThrow("location_lat")),
            locationLong = cursor.getDouble(cursor.getColumnIndexOrThrow("location_long")),
            maxParticipants = cursor.getInt(cursor.getColumnIndexOrThrow("max_participants")),
            currentParticipants = if (cursor.getColumnIndex("participant_count") != -1)
                cursor.getInt(cursor.getColumnIndexOrThrow("participant_count")) else 0
        )
    }

    private fun cursorToReview(cursor: Cursor): Review {
        return Review(
            reviewId = cursor.getInt(cursor.getColumnIndexOrThrow("review_id")),
            clubId = cursor.getInt(cursor.getColumnIndexOrThrow("club_id")),
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
            userName = if (cursor.getColumnIndex("user_name") != -1)
                cursor.getString(cursor.getColumnIndexOrThrow("user_name")) else null,
            rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating")),
            text = cursor.getString(cursor.getColumnIndexOrThrow("text")),
            mediaUrl = cursor.getString(cursor.getColumnIndexOrThrow("media_url")),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
        )
    }

    private fun cursorToFriend(cursor: Cursor): Friend {
        return Friend(
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
            friendId = cursor.getInt(cursor.getColumnIndexOrThrow("friend_id")),
            friendName = if (cursor.getColumnIndex("friend_name") != -1)
                cursor.getString(cursor.getColumnIndexOrThrow("friend_name")) else null,
            status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
        )
    }

    private fun cursorToMedia(cursor: Cursor): Media {
        return Media(
            mediaId = cursor.getInt(cursor.getColumnIndexOrThrow("media_id")),
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
            eventId = if (!cursor.isNull(cursor.getColumnIndexOrThrow("event_id")))
                cursor.getInt(cursor.getColumnIndexOrThrow("event_id")) else null,
            type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
            url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
        )
    }

    fun close() {
        db.close()
    }
}