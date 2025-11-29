package com.example.network

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.network.adapters.ClubAdapter
import com.example.network.database.DatabaseRepository
import com.example.network.model.Club
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class ClubListActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var clubAdapter: ClubAdapter
    private lateinit var clubRecycler: RecyclerView
    private lateinit var searchField: TextInputEditText

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private lateinit var locationSearchToggle: SwitchMaterial
    private lateinit var radiusField: TextInputEditText
    private lateinit var locationStatusText: TextView
    private lateinit var locationSearchContainer: LinearLayout
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchLastLocation()
        } else {
            locationStatusText.text = "Location access denied."
            Toast.makeText(this, "Location permission denied. Cannot search by radius.", Toast.LENGTH_LONG).show()
            // Reset toggle if permission is denied
            locationSearchToggle.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_club_list)

        repository = DatabaseRepository(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        bindViews()
        setupListeners()

        val initialClubs = repository.getAllClubs()
        clubAdapter = ClubAdapter(initialClubs) { club ->
            val intent = Intent(this, ClubDetailsActivity::class.java)
            intent.putExtra("club_id", club.clubId)
            startActivity(intent)
        }
        clubRecycler.adapter = clubAdapter

    }

    private fun bindViews() {
        clubRecycler = findViewById(R.id.clubRecycler)
        clubRecycler.layoutManager = LinearLayoutManager(this)

        searchField = findViewById(R.id.searchClubField) as TextInputEditText

        locationSearchToggle = findViewById(R.id.locationSearchToggle)
        radiusField = findViewById(R.id.radiusField)
        locationStatusText = findViewById(R.id.locationStatusText)
        locationSearchContainer = findViewById(R.id.locationSearchContainer)
    }

    private fun setupListeners() {
        // 1. Text Search Listener
        searchField.addTextChangedListener(searchTextWatcher)

        // 2. Toggle Listener
        locationSearchToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show radius input and check permissions
                locationSearchContainer.visibility = View.VISIBLE
                checkLocationPermissionAndFetch()
            } else {
                // Hide radius input and revert to text search
                locationSearchContainer.visibility = View.GONE
                // Force a reload using the text search criteria
                loadClubs(searchField.text.toString())
            }
        }

        // 3. Create and Back Buttons
        findViewById<Button>(R.id.createClubBtn).setOnClickListener {
            startActivity(Intent(this, CreateClubActivity::class.java))
        }

        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }

        // 4. Radius Field Listener - To trigger search when radius changes
        radiusField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Only reload if location search is active
                if (locationSearchToggle.isChecked) {
                    loadClubs(searchField.text.toString())
                }
            }
        })
    }


    private fun checkLocationPermissionAndFetch() {
        when {
            // Permission already granted
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchLastLocation()
            }
            // Permission needed
            else -> {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun fetchLastLocation() {
        // First check if location client is initialized and permission is truly granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationStatusText.text = "Permission not granted."
            return
        }

        locationStatusText.text = "Fetching location..."

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    locationStatusText.text = "Location: ${currentLat!!.toInt()}, ${currentLng!!.toInt()}"
                    // Trigger the club search after location is successfully fetched
                    loadClubs(searchField.text.toString())
                } else {
                    locationStatusText.text = "Location not found. Check settings."
                    Toast.makeText(this, "Could not get current location.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                locationStatusText.text = "Location fetch failed."
                Toast.makeText(this, "Error fetching location.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadClubs(query: String) {
        // Get user inputs, safely handling nulls/empties
        val radius = radiusField.text.toString().toDoubleOrNull() ?: 10.0
        val isLocationSearch = locationSearchToggle.isChecked

        Thread {
            val clubs: List<Club> = if (isLocationSearch && currentLat != null && currentLng != null && radius > 0) {
                // --- ADVANCED LOCATION SEARCH ---
                // If location search is active and we have coordinates, use them.
                repository.getClubsNearLocation(currentLat!!, currentLng!!, radius)

            } else if (!query.isBlank()) {
                // --- REGULAR TEXT SEARCH (Fallback if toggle is off) ---
                repository.searchClubs(query)

            } else {
                // --- LOAD ALL CLUBS ---
                repository.getAllClubs()
            }

            runOnUiThread {
                clubAdapter.updateData(clubs)
                // If in location mode, show number of results
                if (isLocationSearch && currentLat != null) {
                    locationStatusText.text = "Found ${clubs.size} clubs within ${radius}km"
                }
            }
        }.start()
    }

    private val searchTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Only search by text if the location toggle is OFF
            if (!locationSearchToggle.isChecked) {
                loadClubs(s.toString())
            }
            // If location toggle is ON, text search is ignored for the query
        }
    }

    override fun onResume() {
        super.onResume()
        // If the activity is resumed and the toggle is already checked, fetch location again.
        if (locationSearchToggle.isChecked) {
            checkLocationPermissionAndFetch()
        } else {
            loadClubs(searchField.text.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::repository.isInitialized) {
            repository.close()
        }
    }
}