package com.example.network

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.network.adapters.UserAdapter
import com.example.network.database.DatabaseRepository
import com.example.network.model.Event
import com.example.network.utils.SessionManager

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager

    private var eventId: Int = -1

    // Views
    private lateinit var eventDetailTitle: TextView
    private lateinit var eventDetailDateTime: TextView
    private lateinit var eventDetailClubHost: TextView
    private lateinit var eventDetailDescription: TextView
    private lateinit var eventDetailParticipants: TextView
    private lateinit var attendeeListTitle: TextView
    private lateinit var attendeeRecycler: RecyclerView
    private lateinit var deleteEventBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        repository = DatabaseRepository(this)
        sessionManager = SessionManager(this)

        eventId = intent.getIntExtra("event_id", -1)
        if (eventId == -1) {
            Toast.makeText(this, "Event ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupDeleteLogic()
        findViewById<Button>(R.id.backBtn).setOnClickListener { finish() }

        loadEventDetails()
    }

    private fun bindViews() {
        eventDetailTitle = findViewById(R.id.eventDetailTitle)
        eventDetailDateTime = findViewById(R.id.eventDetailDateTime)
        eventDetailClubHost = findViewById(R.id.eventDetailClubHost)
        eventDetailDescription = findViewById(R.id.eventDetailDescription)
        eventDetailParticipants = findViewById(R.id.eventDetailParticipants)
        attendeeListTitle = findViewById(R.id.attendeeListTitle)

        attendeeRecycler = findViewById(R.id.attendeeRecycler)
        attendeeRecycler.layoutManager = LinearLayoutManager(this)

        deleteEventBtn = findViewById(R.id.deleteEventBtn)
    }

    private fun setupDeleteLogic() {
        deleteEventBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to permanently delete this event?")
                .setPositiveButton("Delete") { _, _ ->
                    val success = repository.deleteEvent(eventId, sessionManager.getUserId())

                    if (success) {
                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show()

                        // ⭐ IMPORTANT: Tell ClubDetailsActivity to refresh
                        setResult(Activity.RESULT_OK)

                        finish()
                    } else {
                        Toast.makeText(this, "Unable to delete event", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadEventDetails() {
        Thread {
            val event = repository.getEventById(eventId)

            if (event == null) {
                runOnUiThread {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
                return@Thread
            }

            val club = repository.getClubById(event.clubId)
            val attendees = repository.getEventAttendees(eventId)
            val currentUserId = sessionManager.getUserId()

            runOnUiThread {

                // Show delete button ONLY for event host
                deleteEventBtn.visibility =
                    if (event.hostId == currentUserId) View.VISIBLE else View.GONE

                eventDetailTitle.text = event.title
                eventDetailDateTime.text = "${event.date} at ${event.time}"
                eventDetailClubHost.text = "Hosted by: ${club?.name ?: "Unknown Club"}"
                eventDetailDescription.text = event.description ?: "No description provided"
                eventDetailParticipants.text =
                    "Participants: ${event.currentParticipants} / ${event.maxParticipants}"

                attendeeListTitle.text = "Attendees (${attendees.size})"

                attendeeRecycler.adapter = UserAdapter(
                    attendees,
                    onUserClick = { user ->
                        Toast.makeText(this, "User: ${user.name}", Toast.LENGTH_SHORT).show()
                    },
                    onRemoveFriend = {} // No remove friend button here
                )
            }
        }.start()
    }

    override fun onDestroy() {
        if (::repository.isInitialized) repository.close()
        super.onDestroy()
    }
}
