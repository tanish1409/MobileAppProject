package com.example.network

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class CreateEventActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager

    private var clubId: Int = -1
    private var hostId: Int = -1

    // Event Data Fields
    private lateinit var eventTitleField: TextInputEditText
    private lateinit var eventDescriptionField: TextInputEditText
    private lateinit var maxParticipantsField: TextInputEditText
    private lateinit var clubHostName: TextView
    private lateinit var dateTimeStatusText: TextView
    private lateinit var locationStatusEventText: TextView

    // State
    private var selectedLatLng: LatLng? = null
    private var selectedDate: Calendar? = null
    private var selectedTime: Calendar? = null

    companion object {
        const val REQUEST_LOCATION_SELECT_EVENT = 6000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        repository = DatabaseRepository(this)
        sessionManager = SessionManager(this)

        // Get data passed from ClubDetailsActivity
        clubId = intent.getIntExtra("club_id", -1)
        val clubName = intent.getStringExtra("club_name")
        hostId = sessionManager.getUserId()

        if (clubId == -1 || hostId == -1) {
            Toast.makeText(this, "Invalid club or user session.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews(clubName)
        setupClickListeners()
    }

    private fun bindViews(clubName: String?) {
        eventTitleField = findViewById(R.id.eventTitleField)
        eventDescriptionField = findViewById(R.id.eventDescriptionField)
        maxParticipantsField = findViewById(R.id.maxParticipantsField)
        clubHostName = findViewById(R.id.clubHostName)
        dateTimeStatusText = findViewById(R.id.dateTimeStatusText)
        locationStatusEventText = findViewById(R.id.locationStatusEventText)

        clubHostName.text = "Hosting for: ${clubName ?: "Club ID $clubId"}"
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.pickDateBtn).setOnClickListener { showDatePicker() }
        findViewById<Button>(R.id.pickTimeBtn).setOnClickListener { showTimePicker() }

        findViewById<Button>(R.id.chooseLocationEventBtn).setOnClickListener {
            // Reuse the existing location selector activity
            startActivityForResult(Intent(this, SelectLocationActivity::class.java), REQUEST_LOCATION_SELECT_EVENT)
        }

        findViewById<Button>(R.id.createEventBtn).setOnClickListener { createEvent() }
        findViewById<Button>(R.id.cancelEventBtn).setOnClickListener { finish() }
    }

    // --- Picker and Location Result Handlers ---

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                updateDateTimeStatus()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() // Prevent past dates
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                updateDateTimeStatus()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false // 24-hour format
        )
        timePickerDialog.show()
    }

    private fun updateDateTimeStatus() {
        val dateStr = selectedDate?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) } ?: "DATE?"
        val timeStr = selectedTime?.let { SimpleDateFormat("hh:mm a", Locale.US).format(it.time) } ?: "TIME?"

        dateTimeStatusText.text = "Selected: $dateStr at $timeStr"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        if (requestCode == REQUEST_LOCATION_SELECT_EVENT) {
            val lat = data?.getDoubleExtra("lat", 0.0) ?: return
            val lng = data.getDoubleExtra("lng", 0.0)
            selectedLatLng = LatLng(lat, lng)
            locationStatusEventText.text = "Location Set: Lat ${lat.toInt()}, Lng ${lng.toInt()}"
            Toast.makeText(this, "Event location selected!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Event Creation Logic ---

    private fun createEvent() {
        val title = eventTitleField.text.toString().trim()
        val description = eventDescriptionField.text.toString().trim()
        val maxParticipants = maxParticipantsField.text.toString().toIntOrNull()

        // 1. Validation
        if (title.isBlank() || selectedDate == null || selectedTime == null || selectedLatLng == null || maxParticipants == null || maxParticipants <= 0) {
            Toast.makeText(this, "Please fill out all required fields (Title, Date, Time, Location, Max Participants).", Toast.LENGTH_LONG).show()
            return
        }

        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate!!.time)
        val timeString = SimpleDateFormat("HH:mm", Locale.US).format(selectedTime!!.time)

        // Disable button
        findViewById<Button>(R.id.createEventBtn).isEnabled = false

        // 2. Database Call
        Thread {
            val eventId = repository.createEvent(
                clubId = clubId,
                hostId = hostId,
                title = title,
                description = description.ifEmpty { null },
                date = dateString,
                time = timeString,
                locationLat = selectedLatLng!!.latitude,
                locationLong = selectedLatLng!!.longitude,
                maxParticipants = maxParticipants
            )

            runOnUiThread {
                findViewById<Button>(R.id.createEventBtn).isEnabled = true
                if (eventId > 0) {
                    Toast.makeText(this, "Event created successfully!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to create event. Try again.", Toast.LENGTH_LONG).show()
                }
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