package com.example.network

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.network.database.DatabaseHelper

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val backBtn = findViewById<Button>(R.id.backBtn)
        backBtn.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        // 1. Get a reference to the helper class
        val dbHelper = DatabaseHelper(this)

        // 2. THIS IS THE CRITICAL CALL that triggers onCreate the first time!
        val db = dbHelper.writableDatabase

// Now you can start performing INSERT/SELECT/UPDATE operations using 'db'
    }

}