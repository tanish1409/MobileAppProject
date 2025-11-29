package com.example.network

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ImageButton
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
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.example.network.utils.OnShakeListener
import com.example.network.utils.ShakeDetector
import com.example.network.utils.SessionManager
import androidx.annotation.DrawableRes

class HomeActivity : AppCompatActivity(), OnMapReadyCallback, OnShakeListener {

    private lateinit var mMap: GoogleMap
    private lateinit var sessionManager: SessionManager
    private var userId: Int = -1
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var selectedFilter: String = "All"
    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mShakeDetector: ShakeDetector
    private val activeMarkers = mutableListOf<Marker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sessionManager = SessionManager(this) // NEW
        userId = sessionManager.getUserId() // NEW

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        mShakeDetector = ShakeDetector(this)

        // Initialize map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this) // mMap is initialized inside onMapReady() later

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
            if (::mMap.isInitialized) mMap.animateCamera(CameraUpdateFactory.zoomIn())
        }

        val zoomOut = findViewById<ImageButton>(R.id.zoomOutBtn)
        zoomOut.setOnClickListener {
            if (::mMap.isInitialized) mMap.animateCamera(CameraUpdateFactory.zoomOut())
        }


        val chipGroup = findViewById<ChipGroup>(R.id.sportChipGroup)


        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty() && ::mMap.isInitialized) {
                val chipId = checkedIds[0]
                val chip = findViewById<Chip>(chipId)
                selectedFilter = chip.text.toString() // Use selectedFilter

                mMap.clear()
                activeMarkers.clear()
                loadClubMarkers(mMap)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register shake listener
        mSensorManager.registerListener(
            mShakeDetector,
            mAccelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        // Re-initialize userId in case of login/logout changes
        userId = sessionManager.getUserId()

        if (::mMap.isInitialized) {
            mMap.clear()
            activeMarkers.clear()
            loadClubMarkers(mMap)
        }
    }

    override fun onPause() {
        // Unregister shake listener to save battery
        mSensorManager.unregisterListener(mShakeDetector)
        super.onPause()
    }

    // --- SHAKE DETECTOR CALLBACK ---
    override fun onShake() {
        if (activeMarkers.isNotEmpty()) {
            Toast.makeText(this, "Suggesting a Club!", Toast.LENGTH_SHORT).show()
            highlightRandomClub()
        } else {
            Toast.makeText(this, "Shake: No clubs found in this category.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- SIMPLIFIED FOCUSING LOGIC ---
    private fun highlightRandomClub() {
        if (!::mMap.isInitialized) return
        val suggestedMarker = activeMarkers.random()
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(suggestedMarker.position, 15f))
        suggestedMarker.showInfoWindow()
    }


    private fun loadClubMarkers(googleMap: GoogleMap) {
        val repo = DatabaseRepository(this)

        activeMarkers.clear()
        googleMap.clear()

        // --- FILTERING LOGIC ---
        val clubs = when (selectedFilter) {
            "My Clubs" -> {
                if (userId != -1) repo.getClubsOwnedByUser(userId) else emptyList()
            }
            "Joined Clubs" -> {
                if (userId != -1) repo.getClubsJoinedByUser(userId) else emptyList()
            }
            "All" -> {
                repo.getAllClubs()
            }
            else -> { // Default: Filter by specific sport type
                val target = selectedFilter.trim().lowercase()
                repo.getAllClubs().filter { club ->
                    club.sportType.trim().lowercase() == target
                }
            }
        }
        // --- END FILTERING LOGIC ---

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
            if (marker != null) {
                activeMarkers.add(marker)
            }
        }

        zoomToFitAllMarkers(clubs)
    }


    private fun setupMapListeners() {
        // 1. Marker Click Listener (Handles Zoom)
        mMap.setOnMarkerClickListener { marker ->
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
            marker.showInfoWindow()
            true
        }

        // 2. Info Window Click Listener (Handles Navigation)
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
            "tennis" -> R.drawable.marker_tennis
            "running" -> R.drawable.marker_running
            "badminton" -> R.drawable.marker_badminton
            else -> R.drawable.marker_default
        }
    }

    private fun setCustomInfoWindow() {
        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null
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
        if (!::mMap.isInitialized) return

        val boundsBuilder = LatLngBounds.Builder()

        clubs.forEach { club ->
            val position = LatLng(club.locationLat, club.locationLong)
            boundsBuilder.include(position)
        }

        val bounds = boundsBuilder.build()

        val padding = 150 // px
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        enableMyLocation()

        loadClubMarkers(mMap)

        setupMapListeners()

        setCustomInfoWindow()

        // Disable default zoom controls
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