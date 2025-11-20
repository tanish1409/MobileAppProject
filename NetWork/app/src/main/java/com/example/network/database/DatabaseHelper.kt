package com.example.network.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper (context: Context) : SQLiteOpenHelper(
    context, "NetWork.db", null, 4
){
    override fun onCreate(db: SQLiteDatabase) {

        // -- USERS TABLE
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS Users (
                    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    bio TEXT,
                    location TEXT,
                    preferences TEXT,
                    profile_image_path TEXT
            )
            """
        )

        // -- FRIENDS TABLE
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS Friends (
                    user_id INTEGER NOT NULL,
                    friend_id INTEGER NOT NULL,
                    status TEXT CHECK(status IN ('pending', 'accepted', 'blocked')),
                    PRIMARY KEY (user_id, friend_id),
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
                    FOREIGN KEY (friend_id) REFERENCES Users(user_id) ON DELETE CASCADE
            )
            """
        )

        // -- CLUBS TABLE
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS Clubs (
                    club_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    sport_type TEXT,
                    location_lat REAL,
                    location_long REAL,
                    owner_id INTEGER,
                    FOREIGN KEY (owner_id) REFERENCES Users(user_id) ON DELETE SET NULL
                )
            """
        )

        // -- EVENTS TABLE
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS Events (
                    event_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    club_id INTEGER,
                    host_id INTEGER,
                    title TEXT NOT NULL,
                    description TEXT,
                    date TEXT NOT NULL,
                    time TEXT NOT NULL,
                    location_lat REAL,
                    location_long REAL,
                    max_participants INTEGER,
                    FOREIGN KEY (club_id) REFERENCES Clubs(club_id) ON DELETE CASCADE,
                    FOREIGN KEY (host_id) REFERENCES Users(user_id) ON DELETE CASCADE
                )
            """
        )

        // -- EVENT_ATTENDANCE TABLE
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS Event_Attendance (
                    event_id INTEGER,
                    user_id INTEGER,
                    status TEXT CHECK(status IN ('joined', 'interested', 'completed')),
                    PRIMARY KEY (event_id, user_id),
                    FOREIGN KEY (event_id) REFERENCES Events(event_id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                )
            """
        )

        // -- REVIEWS TABLE
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS Reviews (
                    review_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    club_id INTEGER,
                    user_id INTEGER,
                    rating INTEGER CHECK(rating BETWEEN 1 AND 5),
                    text TEXT,
                    media_url TEXT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (club_id) REFERENCES Clubs(club_id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                );
            """
        )

        // -- MEDIA TABLE
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS Media (
                    media_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    event_id INTEGER,
                    type TEXT CHECK(type IN ('photo', 'video')),
                    url TEXT NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
                    FOREIGN KEY (event_id) REFERENCES Events(event_id) ON DELETE SET NULL
                )
            """
        )

        // -- CLUB MEMBERSHIP
        db.execSQL("""
        CREATE TABLE IF NOT EXISTS Club_Membership (
            club_id INTEGER NOT NULL,
            user_id INTEGER NOT NULL,
            joined_date DATETIME DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (club_id, user_id),
            FOREIGN KEY (club_id) REFERENCES Clubs(club_id) ON DELETE CASCADE,
            FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
        )
    """)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 to 2 Upgrade: Added profile_image_path
        if (oldVersion < 2) {
            // NOTE: The previous version (v1) code already handled this update:
            db.execSQL("ALTER TABLE Users ADD COLUMN profile_image_path TEXT")
            // If your current DB version is 2, you don't need this, but for safety
            // if oldVersion was 1, you'd execute the change to get to version 2.
        }

        // Version 2 to 3 Upgrade: Added Club_Membership table
        if (oldVersion < 3) {
            // You only need to create the table here.
            // The ALTER TABLE command for v2 (profile_image_path) should NOT be repeated here.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS Club_Membership (
                    club_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    joined_date DATETIME DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (club_id, user_id),
                    FOREIGN KEY (club_id) REFERENCES Clubs(club_id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
                )
            """
            )
        }
    }
}