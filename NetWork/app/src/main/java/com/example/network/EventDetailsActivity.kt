package com.example.network

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.network.adapters.UserAdapter // Reusing UserAdapter
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager
import com.example.network.model.Event

class EventDetailsActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager
    private var eventId: Int = -1

    private lateinit var eventDetailTitle: TextView
    private lateinit var eventDetailDateTime: TextView
    private lateinit var eventDetailClubHost: TextView
    private lateinit var eventDetailDescription: TextView
    private lateinit var eventDetailParticipants: TextView
    private lateinit var attendeeListTitle: TextView
    private lateinit var attendeeRecycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        repository = DatabaseRepository(this)
        sessionManager = SessionManager(this)

        eventId = intent.getIntExtra("event_id", -1)
        if (eventId == -1) {
            Toast.makeText(this, "Event ID missing.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
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
    }

    private fun loadEventDetails() {
        Thread {
            val event: Event? = repository.getEventById(eventId)

            if (event == null) {
                runOnUiThread {
                    Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                return@Thread
            }

            // Fetch related club to display host name
            val club = repository.getClubById(event.clubId)

            // Fetch attendees
            val attendees = repository.getEventAttendees(eventId)

            runOnUiThread {
                eventDetailTitle.text = event.title
                eventDetailDateTime.text = "${event.date} at ${event.time}"
                eventDetailClubHost.text = "Hosted by: ${club?.name ?: "Unknown Club"}"
                eventDetailDescription.text = event.description ?: "No description provided."
                eventDetailParticipants.text = "Capacity: ${event.currentParticipants} / ${event.maxParticipants}"

                // Set up the Attendee List
                attendeeListTitle.text = "Attendees (${attendees.size})"

                // Use the existing UserAdapter to display attendees
                val adapter = UserAdapter(attendees) { user ->
                    Toast.makeText(this, "Viewing profile for ${user.name}", Toast.LENGTH_SHORT).show()
                    // Start UserProfileActivity here if it existed
                }
                attendeeRecycler.adapter = adapter
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