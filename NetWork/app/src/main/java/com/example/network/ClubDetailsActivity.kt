package com.example.network

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.network.adapters.MediaAdapter
import com.example.network.ReviewAdapter
import com.example.network.database.DatabaseRepository
import com.example.network.model.Club
import com.example.network.model.Media
import com.example.network.model.Review
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ClubDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var repository: DatabaseRepository
    private var clubId: Int = -1

    private lateinit var clubNameText: TextView
    private lateinit var clubSportText: TextView
    private lateinit var clubDescriptionText: TextView
    private lateinit var clubRatingText: TextView
    private lateinit var clubMembersText: TextView

    private lateinit var reviewsRecycler: RecyclerView
    private lateinit var mediaRecycler: RecyclerView

    private var mMap: GoogleMap? = null
    private var clubLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_details)

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

        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.joinClubBtn).setOnClickListener {
            Toast.makeText(this, "Join Club coming soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.addReviewBtn).setOnClickListener {
            Toast.makeText(this, "Add Review coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindViews() {
        clubNameText = findViewById(R.id.clubNameText)
        clubSportText = findViewById(R.id.clubSportText)
        clubDescriptionText = findViewById(R.id.clubDescriptionText)
        clubRatingText = findViewById(R.id.clubRatingText)
        clubMembersText = findViewById(R.id.clubMembersText)

        reviewsRecycler = findViewById(R.id.reviewsRecycler)
        mediaRecycler = findViewById(R.id.mediaRecycler)

        reviewsRecycler.layoutManager = LinearLayoutManager(this)
        mediaRecycler.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
    }

    private fun loadClubData() {
        Thread {
            val club: Club? = repository.getClubById(clubId)
            val reviews: List<Review> = repository.getReviewsByClub(clubId)
            val mediaList: List<Media> = repository.getMediaByEvent(clubId)

            runOnUiThread {
                if (club == null) {
                    Toast.makeText(this, "Club not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                clubNameText.text = club.name
                clubSportText.text = club.sportType
                clubDescriptionText.text = club.description ?: "No description"
                clubRatingText.text = String.format("%.1f", club.rating)
                clubMembersText.text = club.memberCount.toString()

                clubLocation = LatLng(club.locationLat, club.locationLong)
                updateMapLocation()

                reviewsRecycler.adapter = ReviewAdapter(reviews)
                mediaRecycler.adapter = MediaAdapter(mediaList)
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
