package com.example.network

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val signupLink = findViewById<TextView>(R.id.signupLink)
        signupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        findViewById<Button>(R.id.loginBtn).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}
