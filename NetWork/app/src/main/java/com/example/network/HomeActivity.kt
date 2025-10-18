package com.example.network

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val viewClubsBtn = findViewById<Button>(R.id.viewClubsBtn)
        viewClubsBtn.setOnClickListener {
            startActivity(Intent(this, ClubListActivity::class.java))
        }

        val viewProfileBtn = findViewById<Button>(R.id.viewProfileBtn)
        viewProfileBtn.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
