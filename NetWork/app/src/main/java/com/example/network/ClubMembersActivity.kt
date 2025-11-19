package com.example.network

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.network.adapters.UserAdapter
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager

class ClubMembersActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager
    private var clubId: Int = -1
    private var ownerId: Int = -1 // ID of the club owner

    private lateinit var memberRecycler: RecyclerView
    private lateinit var titleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_members)

        repository = DatabaseRepository(this)
        sessionManager = SessionManager(this)

        // Get parameters passed from the previous activity
        clubId = intent.getIntExtra("club_id", -1)
        ownerId = intent.getIntExtra("owner_id", -1)

        if (clubId == -1 || ownerId == -1) {
            Toast.makeText(this, "Club information is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        titleText = findViewById(R.id.memberListTitle)
        memberRecycler = findViewById(R.id.memberRecycler)
        memberRecycler.layoutManager = LinearLayoutManager(this)

        // Set the title initially based on the club ID
        titleText.text = "Loading Members..."

        findViewById<Button>(R.id.backBtn).setOnClickListener { finish() }

        loadMembers()
    }

    private fun loadMembers() {
        Thread {
            // Fetch club details to get the name (optional, but nice for the title)
            val club = repository.getClubById(clubId)

            // Fetch the list of members, excluding the owner
            val members = repository.getClubMembers(clubId, ownerId)

            runOnUiThread {
                if (club != null) {
                    titleText.text = "${club.name} Members (${members.size})"
                } else {
                    titleText.text = "Club Members (${members.size})"
                }

                // Initialize the adapter with the fetched members
                val adapter = UserAdapter(members) { user ->
                    // Action when a member is clicked (e.g., viewing their profile)
                    Toast.makeText(this, "Tapped on ${user.name}", Toast.LENGTH_SHORT).show()
                }
                memberRecycler.adapter = adapter
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