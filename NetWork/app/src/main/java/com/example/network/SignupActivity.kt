package com.example.network

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val backBtn = findViewById<Button>(R.id.backBtn)
        backBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
