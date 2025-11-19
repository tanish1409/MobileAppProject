package com.example.network

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.network.ReviewAdapter
import com.example.network.database.DatabaseRepository
import com.example.network.model.Club
import com.example.network.model.Review
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.view.View
import android.widget.LinearLayout
import com.example.network.adapters.MediaAdapter
import com.example.network.model.Media
import com.example.network.utils.SessionManager
import com.example.network.adapters.EventAdapter
import com.example.network.model.Event

class ClubDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var repository: DatabaseRepository

    private lateinit var sessionManager: SessionManager
    private var clubId: Int = -1
    private var userId: Int = -1

    private lateinit var joinClubBtn: Button
    private var isMember: Boolean = false

    private lateinit var clubNameText: TextView
    private lateinit var clubSportText: TextView
    private lateinit var clubDescriptionText: TextView
    private lateinit var clubRatingText: TextView
    private lateinit var clubMembersText: TextView

    private lateinit var reviewsRecycler: RecyclerView
    private lateinit var eventsRecycler: RecyclerView

    private var mMap: GoogleMap? = null
    private var clubLocation: LatLng? = null

    private lateinit var mediaRecycler: RecyclerView
    private lateinit var mediaTitle: TextView
    private lateinit var mediaSectionContainer: LinearLayout

    private lateinit var viewMembersBtn: Button
    private lateinit var createEventBtn: Button // Declaration for the Create Event Button
    private lateinit var ownerControlsLayout: LinearLayout // To control visibility of owner buttons

    private val addReviewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Refreshing reviews...", Toast.LENGTH_SHORT).show()
            loadClubData()
        }
    }

    // Launcher for CreateEventActivity
    private val createEventLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Event created. Events list needs refresh.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_details)

        sessionManager = SessionManager(this)
        userId = sessionManager.getUserId()

        repository = DatabaseRepository(this)

        clubId = intent.getIntExtra("club_id", -1)
        if (clubId == -1) {
            Toast.makeText(this, "No club selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.clubDetailsMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadClubData()

        // Back button is now bound in bindViews and handled there
    }

    private fun bindViews() {
        clubNameText = findViewById(R.id.clubNameText)
        clubSportText = findViewById(R.id.clubSportText)
        clubDescriptionText = findViewById(R.id.clubDescriptionText)
        clubRatingText = findViewById(R.id.clubRatingText)
        clubMembersText = findViewById(R.id.clubMembersText)
        joinClubBtn = findViewById(R.id.joinClubBtn)

        // Owner Controls
        viewMembersBtn = findViewById(R.id.viewMembersBtn)
        createEventBtn = findViewById(R.id.createEventBtn) // Bind create event button
        ownerControlsLayout = findViewById(R.id.ownerControlsLayout) // Bind the layout container

        reviewsRecycler = findViewById(R.id.reviewsRecycler)
        reviewsRecycler.layoutManager = LinearLayoutManager(this)

        mediaRecycler = findViewById(R.id.mediaRecycler)
        mediaRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mediaTitle = findViewById(R.id.mediaTitle)
        mediaSectionContainer = findViewById(R.id.mediaSectionContainer)

        eventsRecycler = findViewById(R.id.eventsRecycler)
        eventsRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        // Set standard listeners once
        findViewById<Button>(R.id.addReviewBtn).setOnClickListener {
            val intent = Intent(this, AddReviewActivity::class.java)
            intent.putExtra("club_id", clubId)
            addReviewLauncher.launch(intent)
        }

        joinClubBtn.setOnClickListener {
            handleJoinLeaveClub()
        }

        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }
    }

    private fun loadClubData() {
        Thread {
            val club: Club? = repository.getClubById(clubId)
            val reviews: List<Review> = repository.getReviewsByClub(clubId)
            val mediaList: List<Media> = repository.getClubMediaWorkaround(clubId)
            val currentIsMember = repository.isUserMemberOfClub(clubId, userId)
            val clubEvents: List<Event> = repository.getEventsByClub(clubId)

            runOnUiThread {
                if (club == null) {
                    Toast.makeText(this, "Club not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                clubNameText.text = club.name
                clubSportText.text = club.sportType
                clubDescriptionText.text = club.description ?: "No description"
                clubRatingText.text = if (club.rating > 0) String.format("%.1f", club.rating) else "N/A"
                clubMembersText.text = club.memberCount.toString()

                clubLocation = LatLng(club.locationLat, club.locationLong)
                updateMapLocation()

                isMember = currentIsMember
                updateClubActionButtons(club.ownerId, club.name)

                reviewsRecycler.adapter = ReviewAdapter(reviews)
                // Setup Events Adapter with dynamic callbacks
                val eventAdapter = EventAdapter(
                    events = clubEvents,
                    userId = userId,
                    joinLeaveListener = ::handleJoinLeaveEvent, // Pass the handler function
                    isUserAttendingChecker = repository::isUserAttendingEvent // Pass the repo check function
                )
                eventsRecycler.adapter = eventAdapter

                if (mediaList.isNotEmpty()) {
                    mediaRecycler.adapter = MediaAdapter(mediaList)
                    // Only set container VISIBLE
                    mediaSectionContainer.visibility = View.VISIBLE
                } else {
                    // Set container GONE when empty
                    mediaSectionContainer.visibility = View.GONE
                }
            }
        }.start()
    }

    // Handles JOIN/LEAVE, VIEW MEMBERS, and CREATE EVENT
    private fun updateClubActionButtons(ownerId: Int, clubName: String?) {
        // 1. Check if the current user is the owner
        if (userId == ownerId) {
            joinClubBtn.text = "CLUB OWNER"
            joinClubBtn.isEnabled = false

            // Show owner controls container
            ownerControlsLayout.visibility = View.VISIBLE
            viewMembersBtn.visibility = View.VISIBLE
            createEventBtn.visibility = View.VISIBLE

            // Set up View Members Click Listener
            viewMembersBtn.setOnClickListener {
                val intent = Intent(this, ClubMembersActivity::class.java).apply {
                    putExtra("club_id", clubId)
                    putExtra("owner_id", ownerId)
                }
                startActivity(intent)
            }

            // Set up Create Event Click Listener
            createEventBtn.setOnClickListener {
                val intent = Intent(this, CreateEventActivity::class.java).apply {
                    putExtra("club_id", clubId)
                    putExtra("club_name", clubName)
                }
                createEventLauncher.launch(intent)
            }
        }
        // 2. Check if the current user is a regular member
        else if (isMember) {
            joinClubBtn.text = "LEAVE CLUB"
            joinClubBtn.setBackgroundResource(android.R.color.darker_gray)
            joinClubBtn.isEnabled = true
            ownerControlsLayout.visibility = View.GONE
        }
        // 3. User is not the owner and not a member
        else {
            joinClubBtn.text = "JOIN CLUB"
            joinClubBtn.isEnabled = true
            ownerControlsLayout.visibility = View.GONE
        }
    }

    private fun handleJoinLeaveClub() {
        // Disable button to prevent double click
        joinClubBtn.isEnabled = false

        Thread {
            val success = if (isMember) {
                repository.leaveClub(clubId, userId)
            } else {
                repository.joinClub(clubId, userId)
            }

            runOnUiThread {
                if (success) {
                    Toast.makeText(this, if (isMember) "Left club successfully" else "Joined club successfully!", Toast.LENGTH_SHORT).show()
                    // Reload data to update button state and member count
                    loadClubData()
                } else {
                    Toast.makeText(this, "Action failed. Please try again.", Toast.LENGTH_SHORT).show()
                    joinClubBtn.isEnabled = true // Re-enable if failure
                }
            }
        }.start()
    }

    private fun handleJoinLeaveEvent(event: Event) {
        // Determine current status using the repository function
        val isCurrentlyAttending = repository.isUserAttendingEvent(event.eventId, userId)
        val actionText = if (isCurrentlyAttending) "Leave" else "Join"

        Thread {
            val success = if (isCurrentlyAttending) {
                // If attending, leave the event
                repository.leaveEvent(event.eventId, userId)
            } else {
                // If not attending, join the event
                repository.joinEvent(event.eventId, userId, "joined")
            }

            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Successfully $actionText event!", Toast.LENGTH_SHORT).show()
                    loadClubData() // Reload to update button states and participant counts
                } else {
                    Toast.makeText(this, "Failed to $actionText event. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateMapLocation()
    }

    private fun updateMapLocation() {
        val map = mMap ?: return
        val loc = clubLocation ?: return

        map.clear()
        map.addMarker(MarkerOptions().position(loc).title("Club Location"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 14f))
    }

    override fun onDestroy() {
        if (::repository.isInitialized) {
            repository.close()
        }
        super.onDestroy()
    }
}