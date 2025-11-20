package com.example.network

import android.app.Activity
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

    private lateinit var eventTitleField: TextInputEditText
    private lateinit var eventDescriptionField: TextInputEditText
    private lateinit var maxParticipantsField: TextInputEditText
    private lateinit var clubHostName: TextView
    private lateinit var dateTimeStatusText: TextView
    private lateinit var locationStatusEventText: TextView

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
            startActivityForResult(
                Intent(this, SelectLocationActivity::class.java),
                REQUEST_LOCATION_SELECT_EVENT
            )
        }

        findViewById<Button>(R.id.createEventBtn).setOnClickListener { createEvent() }
        findViewById<Button>(R.id.cancelEventBtn).setOnClickListener { finish() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = Calendar.getInstance().apply { set(year, month, day) }
                updateDateTimeStatus()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                selectedTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                updateDateTimeStatus()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun updateDateTimeStatus() {
        val dateStr = selectedDate?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time)
        } ?: "DATE?"

        val timeStr = selectedTime?.let {
            SimpleDateFormat("hh:mm a", Locale.US).format(it.time)
        } ?: "TIME?"

        dateTimeStatusText.text = "Selected: $dateStr at $timeStr"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return

        if (requestCode == REQUEST_LOCATION_SELECT_EVENT) {
            val lat = data?.getDoubleExtra("lat", 0.0) ?: return
            val lng = data.getDoubleExtra("lng", 0.0)

            selectedLatLng = LatLng(lat, lng)
            locationStatusEventText.text = "Location Set: ($lat, $lng)"
        }
    }

    private fun createEvent() {
        val title = eventTitleField.text.toString().trim()
        val description = eventDescriptionField.text.toString().trim()
        val maxParticipants = maxParticipantsField.text.toString().toIntOrNull()

        if (title.isBlank() ||
            selectedDate == null ||
            selectedTime == null ||
            selectedLatLng == null ||
            maxParticipants == null ||
            maxParticipants <= 0
        ) {
            Toast.makeText(this, "Fill all required fields.", Toast.LENGTH_LONG).show()
            return
        }

        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate!!.time)
        val timeString = SimpleDateFormat("HH:mm", Locale.US).format(selectedTime!!.time)

        val createBtn = findViewById<Button>(R.id.createEventBtn)
        createBtn.isEnabled = false

        Thread {
            val eventId = repository.createEvent(
                clubId,
                hostId,
                title,
                description.ifEmpty { null },
                dateString,
                timeString,
                selectedLatLng!!.latitude,
                selectedLatLng!!.longitude,
                maxParticipants
            )

            runOnUiThread {
                createBtn.isEnabled = true
                if (eventId > 0) {
                    Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show()

                    // 🔥 CRITICAL FIX — Notify parent to refresh
                    setResult(Activity.RESULT_OK)

                    finish()
                } else {
                    Toast.makeText(this, "Failed to create event.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        if (::repository.isInitialized) repository.close()
        super.onDestroy()
    }
}
