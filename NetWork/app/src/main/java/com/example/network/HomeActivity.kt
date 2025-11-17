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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.ImageButton
import android.graphics.BitmapFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory





class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var selectedSport: String = "All"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val fabClubs = findViewById<FloatingActionButton>(R.id.fabClubs)
        fabClubs.setOnClickListener {
            startActivity(Intent(this, ClubListActivity::class.java))
        }

        val fabProfile = findViewById<FloatingActionButton>(R.id.fabProfile)
        fabProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val zoomIn = findViewById<ImageButton>(R.id.zoomInBtn)
        zoomIn.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.zoomIn())
        }

        val zoomOut = findViewById<ImageButton>(R.id.zoomOutBtn)
        zoomOut.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.zoomOut())
        }


        val chipGroup = findViewById<ChipGroup>(R.id.sportChipGroup)

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty() && ::mMap.isInitialized) {
                val chipId = checkedIds[0]
                val chip = findViewById<Chip>(chipId)
                selectedSport = chip.text.toString()

                mMap.clear()
                loadClubMarkers(mMap)
            }
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
        val allClubs = repo.getAllClubs()

        // Apply filter
        val clubs = if (selectedSport.equals("All", ignoreCase = true)) {
            allClubs
        } else {
            val target = selectedSport.trim().lowercase()

            allClubs.filter { club ->
                club.sportType.trim().lowercase() == target
            }
        }


        clubs.forEach { club ->
            val position = LatLng(club.locationLat, club.locationLong)

            val iconRes = getIconForSport(club.sportType)
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(club.name)
                    .snippet("Tap for details")
                    .icon(getMarkerIcon(iconRes))
            )

            marker?.tag = club.clubId
        }

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

    private fun getMarkerIcon(resourceId: Int): BitmapDescriptor {
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun getIconForSport(sport: String): Int {
        return when (sport.lowercase()) {
            "basketball" -> R.drawable.marker_basketball
            "soccer" -> R.drawable.marker_soccer
            "volleyball" -> R.drawable.marker_volleyball
            // New sports
            "tennis" -> R.drawable.marker_tennis
            "running" -> R.drawable.marker_running
            "badminton" -> R.drawable.marker_badminton
            else -> R.drawable.marker_default // fallback
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

        // Check and request location permission
        enableMyLocation()

        loadClubMarkers(mMap)

        setupMarkerClickListeners()

        setCustomInfoWindow()

        // Enable zoom controls
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isCompassEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
        
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