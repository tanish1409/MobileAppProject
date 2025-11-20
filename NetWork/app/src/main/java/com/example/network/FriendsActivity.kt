package com.example.network

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.network.adapters.FriendRequestAdapter
import com.example.network.adapters.UserAdapter
import com.example.network.database.DatabaseRepository
import com.example.network.model.Friend
import com.example.network.model.User
import com.example.network.utils.SessionManager

class FriendsActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager
    private var userId: Int = -1

    private lateinit var pendingRequestsRecycler: RecyclerView
    private lateinit var friendsRecycler: RecyclerView
    private lateinit var requestsTitle: TextView
    private lateinit var friendsTitle: TextView

    private var requestAdapter: FriendRequestAdapter? = null
    private var friendsAdapter: UserAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        sessionManager = SessionManager(this)
        repository = DatabaseRepository(this)
        userId = sessionManager.getUserId()

        if (userId == -1) {
            Toast.makeText(this, "User session not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        findViewById<Button>(R.id.backBtn)?.setOnClickListener { finish() }
    }

    private fun bindViews() {
        requestsTitle = findViewById(R.id.requestsTitle)
        friendsTitle = findViewById(R.id.friendsTitle)

        pendingRequestsRecycler = findViewById(R.id.pendingRequestsRecycler)
        friendsRecycler = findViewById(R.id.friendsRecycler)

        pendingRequestsRecycler.layoutManager = LinearLayoutManager(this)
        friendsRecycler.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        loadFriendData()
    }

    private fun loadFriendData() {
        Thread {
            val pendingRequests = repository.getPendingFriendRequests(userId)
            val friends = repository.getFriends(userId)

            val pendingUsers = pendingRequests.mapNotNull { friendRowToUser(it, fromSender = true) }
            val friendUsers = friends.mapNotNull { friendRowToUser(it, fromSender = false) }

            requestsTitle.text = "Pending Friend Requests (${pendingUsers.size})"
            friendsTitle.text = "Friends List (${friendUsers.size})"

            runOnUiThread {
                // Pending
                if (pendingUsers.isEmpty()) {
                    requestsTitle.visibility = View.GONE
                    pendingRequestsRecycler.visibility = View.GONE
                } else {
                    requestsTitle.visibility = View.VISIBLE
                    pendingRequestsRecycler.visibility = View.VISIBLE

                    if (requestAdapter == null) {
                        requestAdapter = FriendRequestAdapter(
                            pendingUsers,
                            onAccept = { user -> acceptRequest(user) },
                            onReject = { user -> rejectRequest(user) }
                        )
                        pendingRequestsRecycler.adapter = requestAdapter
                    } else {
                        requestAdapter?.updateData(pendingUsers)
                    }
                }

                // Friends
                if (friendUsers.isEmpty()) {
                    friendsTitle.visibility = View.GONE
                    friendsRecycler.visibility = View.GONE
                } else {
                    friendsTitle.visibility = View.VISIBLE
                    friendsRecycler.visibility = View.VISIBLE

                    if (friendsAdapter == null) {
                        friendsAdapter = UserAdapter(friendUsers) { user ->
                            Toast.makeText(
                                this,
                                "Clicked on ${user.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            // TODO: open profile etc
                        }
                        friendsRecycler.adapter = friendsAdapter
                    } else {
                        friendsAdapter?.updateData(friendUsers)
                    }
                }
            }
        }.start()
    }

    private fun friendRowToUser(friend: Friend, fromSender: Boolean): User? {
        // For pending requests: userId = sender, friendId = current user
        val otherUserId = if (fromSender) friend.userId else friend.friendId
        return repository.getUserById(otherUserId)
    }

    private fun acceptRequest(sender: User) {
        Thread {
            val success = repository.acceptFriendRequest(sender.userId, userId)
            runOnUiThread {
                if (success) {
                    Toast.makeText(
                        this,
                        "Accepted ${sender.name}'s request.",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadFriendData()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to accept request.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun rejectRequest(sender: User) {
        Thread {
            val success = repository.rejectFriendRequest(sender.userId, userId)
            runOnUiThread {
                if (success) {
                    Toast.makeText(
                        this,
                        "Rejected ${sender.name}'s request.",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadFriendData()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to reject request.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository.close()
    }
}
