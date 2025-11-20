package com.example.network

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.network.database.DatabaseRepository
import com.example.network.utils.SessionManager
import com.google.android.material.textfield.TextInputEditText
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.IOException

class AddReviewActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var ratingBar: RatingBar
    private lateinit var commentField: TextInputEditText
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button

    private lateinit var recordButton: Button
    private var isRecording = false
    private var audioFileName: String? = null
    private var mediaRecorder: MediaRecorder? = null

    private val RECORD_PERMISSION_CODE = 101
    private val RECORD_PERMISSION = Manifest.permission.RECORD_AUDIO

    private var clubId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_review)

        // --- Configuration for Pop-up Window ---
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Close the activity if the user taps outside the dialog frame
        findViewById<View>(android.R.id.content).setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Check if the touch is outside the main container
                val container = findViewById<View>(R.id.popup_container)
                if (event.x < container.left || event.x > container.right ||
                    event.y < container.top || event.y > container.bottom) {
                    finish()
                }
            }
            // **FIX 3: Call performClick for accessibility**
            v.performClick()
            true
        }
        // --- End Configuration ---

        repository = DatabaseRepository(this)
        sessionManager = SessionManager(this)

        clubId = intent.getIntExtra("club_id", -1)
        if (clubId == -1) {
            Toast.makeText(this, "Error: Club ID not found.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ratingBar = findViewById(R.id.reviewRatingBar)
        commentField = findViewById(R.id.reviewCommentField)
        submitButton = findViewById(R.id.submitReviewBtn)
        cancelButton = findViewById(R.id.cancelReviewBtn)
        recordButton = findViewById(R.id.recordVoiceNoteBtn)

        submitButton.setOnClickListener {
            submitReview()
        }

        cancelButton.setOnClickListener {
            finish() // Simply close the activity
        }

        recordButton.setOnClickListener {
            if (checkPermissions()) {
                toggleRecording()
            } else {
                requestPermissions()
            }
        }
    }

    private fun submitReview() {
        val rating = ratingBar.rating.toInt()
        val comment = commentField.text.toString().trim()
        val userId = sessionManager.getUserId()
        val finalMediaUrl = audioFileName

        if (rating == 0) {
            Toast.makeText(this, "Please provide a rating.", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == -1) {
            Toast.makeText(this, "You must be logged in to post a review.", Toast.LENGTH_LONG).show()
            return
        }

        val reviewId = repository.addReview(clubId, userId, rating, comment, finalMediaUrl)

        if (reviewId > -1) {
            Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK) // Set result to indicate success
            finish()
        } else {
            Toast.makeText(this, "Failed to submit review.", Toast.LENGTH_SHORT).show()
        }
    }

    // In AddReviewActivity.kt, add these new methods:

    // --- Permissions ---
    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, RECORD_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(RECORD_PERMISSION),
            RECORD_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            toggleRecording()
        } else {
            Toast.makeText(this, "Microphone permission is required for voice notes.", Toast.LENGTH_SHORT).show()
        }
    }


    // --- Recording Logic ---
    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
        isRecording = !isRecording
        recordButton.text = if (isRecording) "STOP RECORDING" else "RECORD VOICE NOTE"
    }

    private fun startRecording() {
        val audioDir = File(filesDir, "voice_notes")
        if (!audioDir.exists()) audioDir.mkdirs()

        audioFileName = File(audioDir, "${System.currentTimeMillis()}.m4a").absolutePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setAudioChannels(1)

                setOutputFile(audioFileName)
                prepare()
                start()
            }

            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Recording failed: ${e.message}", Toast.LENGTH_LONG).show()
            audioFileName = null
            mediaRecorder = null
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                release()
                Toast.makeText(this@AddReviewActivity, "Recording saved: $audioFileName", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@AddReviewActivity, "Failed to stop recording.", Toast.LENGTH_SHORT).show()
                File(audioFileName).delete() // Delete corrupted file
                audioFileName = null
            }
        }
        mediaRecorder = null
    }

    override fun onDestroy() {
        if (::repository.isInitialized) {
            repository.close()
        }
        if (isRecording) {
            stopRecording()
        }
        mediaRecorder?.release()
        mediaRecorder = null
        super.onDestroy()
    }
}
