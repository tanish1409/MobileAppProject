package com.example.network

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText

class EditProfileActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var editName: TextInputEditText
    private lateinit var editBio: TextInputEditText
    private lateinit var editLocation: TextInputEditText
    private lateinit var saveBtn: Button

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)
        userId = sessionManager.getUserId()

        if (userId == -1) {
            Toast.makeText(this, "User session not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        repository = DatabaseRepository(this)
        bindViews()
        setupClickListeners()
        loadUserData()
    }

    private fun bindViews() {
        editName = findViewById(R.id.editName)
        editBio = findViewById(R.id.editBio)
        editLocation = findViewById(R.id.editLocation)
        saveBtn = findViewById(R.id.saveBtn)
    }

    private fun setupClickListeners() {
        saveBtn.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserData() {
        Thread {
            val user = repository.getUserById(userId)

            runOnUiThread {
                if (user != null) {
                    editName.setText(user.name)
                    editBio.setText(user.bio ?: "")
                    editLocation.setText(user.location ?: "")
                } else {
                    Toast.makeText(
                        this,
                        "Failed to load user data",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }.start()
    }

    private fun saveProfile() {
        val name = editName.text.toString().trim()
        val bio = editBio.text.toString().trim()
        val location = editLocation.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            editName.error = "Name cannot be empty"
            editName.requestFocus()
            return
        }

        // Disable button to prevent multiple clicks
        saveBtn.isEnabled = false

        Thread {
            val success = repository.updateUserProfile(
                userId = userId,
                name = name,
                bio = bio.ifEmpty { null },
                location = location.ifEmpty { null },
                preferences = null
            )

            runOnUiThread {
                saveBtn.isEnabled = true

                if (success) {
                    Toast.makeText(
                        this,
                        "Profile updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish() // Return to ProfileActivity
                } else {
                    Toast.makeText(
                        this,
                        "Failed to update profile",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        if (::repository.isInitialized) {
            repository.close()
        }
        super.onDestroy()
    }
}