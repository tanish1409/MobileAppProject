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
import com.google.android.material.textfield.TextInputEditText // <-- **FIX 1: Import TextInputEditText**

class AddReviewActivity : AppCompatActivity() {

    private lateinit var repository: DatabaseRepository
    private lateinit var sessionManager: SessionManager

    private lateinit var ratingBar: RatingBar
    private lateinit var commentField: TextInputEditText // <-- **FIX 2: Change EditText to TextInputEditText**
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button

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
        commentField = findViewById(R.id.reviewCommentField) // This now matches correctly
        submitButton = findViewById(R.id.submitReviewBtn)
        cancelButton = findViewById(R.id.cancelReviewBtn)

        submitButton.setOnClickListener {
            submitReview()
        }

        cancelButton.setOnClickListener {
            finish() // Simply close the activity
        }
    }

    private fun submitReview() {
        val rating = ratingBar.rating.toInt()
        val comment = commentField.text.toString().trim()
        val userId = sessionManager.getUserId()

        if (rating == 0) {
            Toast.makeText(this, "Please provide a rating.", Toast.LENGTH_SHORT).show()
            return
        }

        if (userId == -1) {
            Toast.makeText(this, "You must be logged in to post a review.", Toast.LENGTH_LONG).show()
            return
        }

        val reviewId = repository.addReview(clubId, userId, rating, comment, null)

        if (reviewId > -1) {
            Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK) // Set result to indicate success
            finish()
        } else {
            Toast.makeText(this, "Failed to submit review.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (::repository.isInitialized) {
            repository.close()
        }
        super.onDestroy()
    }
}
