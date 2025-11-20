package com.example.network

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.network.adapters.EventAdapter
import com.example.network.adapters.MediaAdapter
import com.example.network.database.DatabaseRepository
import com.example.network.model.*
import com.example.network.utils.SessionManager
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ClubDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager

    private var clubId: Int = -1
    private var userId: Int = -1
    private var isMember: Boolean = false

    private var mMap: GoogleMap? = null
    private var clubLocation: LatLng? = null

    // UI
    private lateinit var clubNameText: TextView
    private lateinit var clubSportText: TextView
    private lateinit var clubDescriptionText: TextView
    private lateinit var clubRatingText: TextView
    private lateinit var clubMembersText: TextView
    private lateinit var joinClubBtn: Button

    private lateinit var reviewsRecycler: RecyclerView
    private lateinit var eventsRecycler: RecyclerView

    private lateinit var mediaRecycler: RecyclerView
    private lateinit var mediaSectionContainer: LinearLayout

    private lateinit var viewMembersBtn: Button
    private lateinit var createEventBtn: Button
    private lateinit var deleteClubBtn: Button
    private lateinit var ownerControlsLayout: LinearLayout

    // Launchers
    private val addReviewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) loadClubData()
    }

    private val createEventLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) loadClubData()
    }

    private val eventDetailsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) loadClubData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_details)

        sessionManager = SessionManager(this)
        userId = sessionManager.getUserId()

        clubId = intent.getIntExtra("club_id", -1)
        if (clubId == -1) {
            Toast.makeText(this, "Invalid Club ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        repository = DatabaseRepository(this)

        bindViews()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.clubDetailsMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadClubData()
    }

    private fun bindViews() {
        clubNameText = findViewById(R.id.clubNameText)
        clubSportText = findViewById(R.id.clubSportText)
        clubDescriptionText = findViewById(R.id.clubDescriptionText)
        clubRatingText = findViewById(R.id.clubRatingText)
        clubMembersText = findViewById(R.id.clubMembersText)
        joinClubBtn = findViewById(R.id.joinClubBtn)

        viewMembersBtn = findViewById(R.id.viewMembersBtn)
        createEventBtn = findViewById(R.id.createEventBtn)
        deleteClubBtn = findViewById(R.id.deleteClubBtn)
        ownerControlsLayout = findViewById(R.id.ownerControlsLayout)

        reviewsRecycler = findViewById(R.id.reviewsRecycler)
        reviewsRecycler.layoutManager = LinearLayoutManager(this)

        eventsRecycler = findViewById(R.id.eventsRecycler)
        eventsRecycler.layoutManager = LinearLayoutManager(this)

        mediaRecycler = findViewById(R.id.mediaRecycler)
        mediaRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        mediaSectionContainer = findViewById(R.id.mediaSectionContainer)

        findViewById<Button>(R.id.backBtn).setOnClickListener { finish() }

        findViewById<Button>(R.id.addReviewBtn).setOnClickListener {
            val intent = Intent(this, AddReviewActivity::class.java)
            intent.putExtra("club_id", clubId)
            addReviewLauncher.launch(intent)
        }

        joinClubBtn.setOnClickListener { handleJoinLeaveClub() }
    }

    private fun loadClubData() {
        Thread {
            val club = repository.getClubById(clubId)
            val reviews = repository.getReviewsByClub(clubId)
            val mediaList = repository.getClubMediaWorkaround(clubId)
            val events = repository.getEventsByClub(clubId)
            val userIsMember = repository.isUserMemberOfClub(clubId, userId)

            runOnUiThread {
                if (club == null) {
                    Toast.makeText(this, "Club not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                isMember = userIsMember

                clubNameText.text = club.name
                clubSportText.text = club.sportType
                clubDescriptionText.text = club.description ?: "No description"
                clubRatingText.text = if (club.rating > 0) "%.1f".format(club.rating) else "N/A"
                clubMembersText.text = club.memberCount.toString()

                clubLocation = LatLng(club.locationLat, club.locationLong)
                updateMapLocation()

                reviewsRecycler.adapter = ReviewAdapter(reviews)

                eventsRecycler.adapter = EventAdapter(
                    events,
                    userId,
                    joinLeaveListener = ::handleJoinLeaveEvent,
                    isUserAttendingChecker = repository::isUserAttendingEvent
                ) { event ->
                    val i = Intent(this, EventDetailsActivity::class.java)
                    i.putExtra("event_id", event.eventId)
                    eventDetailsLauncher.launch(i)
                }

                if (mediaList.isNotEmpty()) {
                    mediaRecycler.adapter = MediaAdapter(mediaList)
                    mediaSectionContainer.visibility = View.VISIBLE
                } else {
                    mediaSectionContainer.visibility = View.GONE
                }

                updateOwnerAndMemberUI(club)
            }
        }.start()
    }

    private fun updateOwnerAndMemberUI(club: Club) {
        when {
            userId == club.ownerId -> {
                joinClubBtn.text = "CLUB OWNER"
                joinClubBtn.isEnabled = false

                ownerControlsLayout.visibility = View.VISIBLE
                viewMembersBtn.visibility = View.VISIBLE
                createEventBtn.visibility = View.VISIBLE
                deleteClubBtn.visibility = View.VISIBLE

                viewMembersBtn.setOnClickListener {
                    val i = Intent(this, ClubMembersActivity::class.java)
                    i.putExtra("club_id", clubId)
                    i.putExtra("owner_id", club.ownerId)
                    startActivity(i)
                }

                createEventBtn.setOnClickListener {
                    val i = Intent(this, CreateEventActivity::class.java)
                    i.putExtra("club_id", clubId)
                    i.putExtra("club_name", club.name)
                    createEventLauncher.launch(i)
                }

                deleteClubBtn.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Delete Club?")
                        .setMessage("This action cannot be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            if (repository.deleteClub(clubId, userId)) {
                                Toast.makeText(this, "Club deleted.", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }

            isMember -> {
                joinClubBtn.text = "LEAVE CLUB"
                joinClubBtn.isEnabled = true
                ownerControlsLayout.visibility = View.GONE
            }

            else -> {
                joinClubBtn.text = "JOIN CLUB"
                joinClubBtn.isEnabled = true
                ownerControlsLayout.visibility = View.GONE
            }
        }
    }

    private fun handleJoinLeaveClub() {
        joinClubBtn.isEnabled = false

        Thread {
            val success = if (isMember) {
                repository.leaveClub(clubId, userId)
            } else {
                repository.joinClub(clubId, userId)
            }

            runOnUiThread {
                if (success) loadClubData()
                else {
                    Toast.makeText(this, "Action failed", Toast.LENGTH_SHORT).show()
                    joinClubBtn.isEnabled = true
                }
            }
        }.start()
    }

    private fun handleJoinLeaveEvent(event: Event) {
        Thread {
            val attending = repository.isUserAttendingEvent(event.eventId, userId)
            val success = if (attending)
                repository.leaveEvent(event.eventId, userId)
            else
                repository.joinEvent(event.eventId, userId)

            runOnUiThread {
                if (success) loadClubData()
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
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))
    }

    override fun onDestroy() {
        if (::repository.isInitialized) repository.close()
        super.onDestroy()
    }
}
