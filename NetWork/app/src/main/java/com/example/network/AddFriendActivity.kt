package com.example.network

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText

class AddFriendActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var friendSearchInput: TextInputEditText
    private lateinit var sendRequestBtn: Button

    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        repository = DatabaseRepository(this)
        sessionManager = SessionManager(this)
        currentUserId = sessionManager.getUserId()

        if (currentUserId == -1) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        friendSearchInput = findViewById(R.id.friendSearchInput)
        sendRequestBtn = findViewById(R.id.sendRequestBtn)

        findViewById<Button>(R.id.cancelAddFriendBtn)?.setOnClickListener { finish() }

        sendRequestBtn.setOnClickListener {
            sendFriendRequest()
        }
    }

    private fun sendFriendRequest() {
        val query = friendSearchInput.text?.toString()?.trim().orEmpty()

        if (query.isEmpty()) {
            friendSearchInput.error = "Please enter email or name"
            return
        }

        sendRequestBtn.isEnabled = false

        Thread {
            // 1. Look up user by email or exact name
            val targetUser = repository.getUserByEmailOrName(query)

            if (targetUser == null) {
                runOnUiThread {
                    friendSearchInput.error = "No user found with that email or name"
                    Toast.makeText(
                        this,
                        "User not found.",
                        Toast.LENGTH_LONG
                    ).show()
                    sendRequestBtn.isEnabled = true
                }
                return@Thread
            }

            val targetUserId = targetUser.userId

            // 2. Cannot add yourself
            if (targetUserId == currentUserId) {
                runOnUiThread {
                    friendSearchInput.error = "You cannot add yourself"
                    sendRequestBtn.isEnabled = true
                }
                return@Thread
            }

            // 3. Already friends?
            if (repository.areFriends(currentUserId, targetUserId)) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "${targetUser.name} is already your friend.",
                        Toast.LENGTH_LONG
                    ).show()
                    sendRequestBtn.isEnabled = true
                }
                return@Thread
            }

            // 4. Pending request in either direction?
            if (repository.hasPendingFriendRequest(currentUserId, targetUserId)) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "There is already a pending friend request with ${targetUser.name}.",
                        Toast.LENGTH_LONG
                    ).show()
                    sendRequestBtn.isEnabled = true
                }
                return@Thread
            }

            // 5. Send request
            val success = repository.sendFriendRequest(
                userId = currentUserId,
                friendId = targetUserId
            )

            runOnUiThread {
                sendRequestBtn.isEnabled = true
                if (success) {
                    Toast.makeText(
                        this,
                        "Friend request sent to ${targetUser.name}.",
                        Toast.LENGTH_LONG
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to send request. Please try again.",
                        Toast.LENGTH_LONG
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
