package com.example.network

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.model.Marker
import com.example.network.database.DatabaseRepository
import com.example.network.model.Club
import com.google.android.gms.maps.model.LatLngBounds



class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Button listeners
        val viewClubsBtn = findViewById<Button>(R.id.viewClubsBtn)
        viewClubsBtn.setOnClickListener {
            startActivity(Intent(this, ClubListActivity::class.java))
        }

        val viewProfileBtn = findViewById<Button>(R.id.viewProfileBtn)
        viewProfileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mMap.isInitialized) {
            mMap.clear()
            loadClubMarkers(mMap)
        }
    }


    private fun loadClubMarkers(googleMap: GoogleMap) {
        val repo = DatabaseRepository(this)
        val clubs = repo.getAllClubs()

        // Add markers
        clubs.forEach { club ->
            val position = LatLng(club.locationLat, club.locationLong)

            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(club.name)
                    .snippet("Tap for details")
            )

            marker?.tag = club.clubId
        }

        // Zoom to fit all pins
        zoomToFitAllMarkers(clubs)
    }


    private fun setupMarkerClickListeners() {
        mMap.setOnInfoWindowClickListener { marker ->
            val clubId = marker.tag as? Int
            if (clubId != null) {
                val intent = Intent(this, ClubDetailsActivity::class.java)
                intent.putExtra("club_id", clubId)
                startActivity(intent)
            }
        }
    }

    private fun setCustomInfoWindow() {
        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null // Use default background frame
            }

            override fun getInfoContents(marker: Marker): View {
                val view = layoutInflater.inflate(R.layout.custom_info_window, null)

                val title = view.findViewById<TextView>(R.id.title)
                val snippet = view.findViewById<TextView>(R.id.snippet)

                title.text = marker.title
                snippet.text = "Tap for club details"

                return view
            }
        })
    }

    private fun zoomToFitAllMarkers(clubs: List<Club>) {
        if (clubs.isEmpty()) return

        val boundsBuilder = LatLngBounds.Builder()

        clubs.forEach { club ->
            val position = LatLng(club.locationLat, club.locationLong)
            boundsBuilder.include(position)
        }

        val bounds = boundsBuilder.build()

        // Animate camera to fit all markers with padding
        val padding = 150 // px
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set default location (Toronto, based on your location)
        val toronto = LatLng(43.6532, -79.3832)

        // Add sample club markers (replace with your actual club data)
        mMap.addMarker(MarkerOptions()
            .position(toronto)
            .title("Sample Club")
            .snippet("Click for details"))

        // Move camera to Toronto
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(toronto, 12f))

        // Enable zoom controls
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        // Check and request location permission
        enableMyLocation()

        loadClubMarkers(mMap)

        setupMarkerClickListeners()

        setCustomInfoWindow()

    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                } else {
                    Toast.makeText(
                        this,
                        "Location permission denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}