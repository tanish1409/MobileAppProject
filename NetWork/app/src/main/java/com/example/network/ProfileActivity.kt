package com.example.network

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var profileBio: TextView
    private lateinit var profileLocation: TextView
    private lateinit var clubsCount: TextView
    private lateinit var eventsCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()
        if (userId == -1) {
            navigateToLogin()
            return
        }

        repository = DatabaseRepository(this)
        bindViews()
        setupClickListeners()
        loadUserProfile(userId)
    }

    private fun bindViews() {
        profileImage = findViewById(R.id.profileImage)
        profileName = findViewById(R.id.profileName)
        profileEmail = findViewById(R.id.profileEmail)
        profileBio = findViewById(R.id.profileBio)
        profileLocation = findViewById(R.id.profileLocation)
        clubsCount = findViewById(R.id.clubsCount)
        eventsCount = findViewById(R.id.eventsCount)
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.logoutBtn).setOnClickListener {
            sessionManager.logout()
            navigateToLogin()
        }

        findViewById<Button>(R.id.editProfileBtn).setOnClickListener {
            Toast.makeText(this, "Edit profile coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile(userId: Int) {
        Thread {
            val user = repository.getUserById(userId)
            val profileImagePath = repository.getUserProfileImage(userId)
            val ownedClubCount = repository.getUserClubCount(userId)
            val userEventCount = repository.getUserEventCount(userId)

            runOnUiThread {
                if (user == null) {
                    Toast.makeText(
                        this,
                        "Unable to load user profile.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@runOnUiThread
                }

                profileName.text = user.name
                profileEmail.text = user.email
                profileBio.text = user.bio?.takeIf { it.isNotBlank() } ?: "No bio added."
                profileLocation.text = user.location?.takeIf { it.isNotBlank() } ?: "Not set"
                clubsCount.text = ownedClubCount.toString()
                eventsCount.text = userEventCount.toString()

                if (!profileImagePath.isNullOrEmpty()) {
                    val bitmap = BitmapFactory.decodeFile(profileImagePath)
                    if (bitmap != null) {
                        profileImage.setImageBitmap(bitmap)
                    } else {
                        profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }
        }.start()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        if (::repository.isInitialized) {
            repository.close()
        }
        super.onDestroy()
    }
}